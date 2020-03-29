package ch.krie


import java.io.InputStream
import java.security.{KeyStore, SecureRandom}

import akka.NotUsed
import akka.actor.typed._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl._
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Directives._
import akka.stream.{Materializer, SystemMaterializer}
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.stream.typed.scaladsl.ActorSink
import akka.stream.typed.scaladsl.ActorSource
import akka.util.Timeout
import ch.krie.api.NewSpot
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}
import spray.json.{JsonParser, JsonReader}

import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn
import scala.util.Failure
import scala.util.Success
import scala.concurrent.duration._

object Server {

  def newUserFlow(system: ActorSystem[_],
                  world: ActorRef[World.Command],
                  userParent: ActorRef[SpawnProtocol.Command]): Flow[Message, Message, Future[NotUsed]] = {
    // new connection - new user actor
    implicit val ec = system.executionContext
    import akka.actor.typed.scaladsl.AskPattern._
    implicit val timeout: Timeout = 3.seconds
    implicit val scheduler: Scheduler = system.scheduler
    val futureUserActor: Future[ActorRef[User.Event]] =
      userParent.ask(SpawnProtocol.Spawn(User(world), "user", Props.empty, _))
    val futureAckActor: Future[ActorRef[AckingReceiver.Ack]] =
      userParent.ask(SpawnProtocol.Spawn(AckingReceiver(), "ack", Props.empty, _))

    Flow.futureFlow(futureUserActor.zip(futureAckActor).map { case (userActor, ack) =>
      val incomingMessages: Sink[Message, NotUsed] =
        Flow[Message]
          .map(_.asTextMessage.getStrictText)
          .map(s => JsonParser.apply(s))
          .map(implicitly[JsonReader[NewSpot]].read)
          .map(// transform websocket message to domain message
            spot => User.IncomingMessage(spot.toPaintedSpot))
          .to(
            ActorSink.actorRef[User.Event](
              userActor,
              User.Disconnected,
              _ => User.Disconnected
            )
          )

      val outgoingMessages: Source[Message, NotUsed] = {
        ActorSource.actorRefWithBackpressure[User.OutgoingMessage, AckingReceiver.Ack](
          ackTo = ack,
          ackMessage = AckingReceiver.Ack,
          completionMatcher = PartialFunction.empty,
          failureMatcher = PartialFunction.empty
        )
          .mapMaterializedValue { outActor =>
            // give the user actor a way to send messages out
            userActor ! User.Connected(outActor, ack)
            NotUsed
          }
          .map(
            // transform domain message to web socket message
            (outMsg: User.OutgoingMessage) => TextMessage(outMsg.text)
          )
      }
      // then combine both to a flow
      Flow.fromSinkAndSourceCoupled(incomingMessages, outgoingMessages)
    })

  }

  def main(args: Array[String]): Unit = {

    val (interface, port) = if (args.headOption.isDefined) {
      val split = args.head.split(":")

      split(0) -> split(1).toInt
    } else {
      "localhost" -> 8080
    }

    val pw: Option[String] = Option(args(1))

    val system: ActorSystem[Any] = {

      val https: Option[HttpsConnectionContext] = if (pw.isDefined) {

        val keystorePath = args(2)

        val ks: KeyStore = KeyStore.getInstance("PKCS12")
        val keystore: InputStream = getClass.getClassLoader.getResourceAsStream(keystorePath)
        ks.load(keystore, pw.get.toCharArray)

        val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
        keyManagerFactory.init(ks, pw.get.toCharArray)

        val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
        tmf.init(ks)

        val sslContext: SSLContext = SSLContext.getInstance("TLS")
        sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)

        Some {
          val httpsCC = ConnectionContext.https(sslContext)
          sslContext.getDefaultSSLParameters.getCipherSuites.foreach(println)
          httpsCC.enabledCipherSuites.foreach(_.foreach(println))
          httpsCC
        }

      } else None

      ActorSystem(
        Behaviors.setup[Any] { context =>
          implicit val ec: ExecutionContext = context.executionContext

          val system = context.system
          val chatRoom = context.spawn(World(), "game")
          val userParent = context.spawn(SpawnProtocol(), "users")

          val route =
            path("game") {
              get {
                handleWebSocketMessages(newUserFlow(system, chatRoom, userParent))
              }
            }

          // needed until Akka HTTP has a 2.6 only release
          implicit val materializer: Materializer = SystemMaterializer(context.system).materializer
          implicit val classicSystem: akka.actor.ActorSystem = context.system.toClassic
          if (https.isDefined) {
            Http().setDefaultServerHttpContext(https.get)
            Http()
              .bindAndHandle(route, interface, port, connectionContext = https.get)
          } else {
            Http()
              .bindAndHandle(route, interface, port)
          }
            // future callback, be careful not to touch actor state from in here
            .onComplete {
            case Success(b) =>
              println(
                s"Started server at ${b.localAddress.getHostString}:${b.localAddress.getPort}"
              )
            case Failure(ex) =>
              ex.printStackTrace()
              println("Server failed to start, terminating")
              context.system.terminate()
          }

          Behaviors.empty
        },
        "DrawServer"
      )

    }
      println("Press enter to kill server")
      StdIn.readLine()
      system.terminate()
  }
}
