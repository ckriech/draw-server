package ch.krie

import java.awt.{Color, Point}

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage, UpgradeToWebSocket}
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.typed.scaladsl.ActorSource
import akka.{Done, NotUsed}
import ch.krie.Device.Update
import ch.krie.api.{MessageWrapper, Register}
import spray.json.{JsonParser, JsonReader}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.StdIn
import scala.language.postfixOps

object FunServer extends App {

  implicit val system = ActorSystem()
  implicit val materializer = Materializer(system)
  implicit val executionContext = system.dispatcher



  var area: Map[Point, Color] = Map.empty

//  area = area updated (new Point(1, 1), new Color(1,2,3))

//  val in: Sink[Message, Future[Done]] = Sink.foreach[Message] {
//    case bm: BinaryMessage => bm.toStrict(1 minute)
//      .map(_.getStrictData)
//      .map(MessageReader.fromBytes)
//      .map(dealWithMessage)
//
//    case tm: TextMessage => tm.toStrict(1 minute)
//      .map(_.text)
//      .map(MessageReader.fromString)
//      .map(dealWithMessage)
//
//    case _ => throw new IllegalArgumentException("")
//  }

  val in: Sink[Message, Future[Done]] = Sink.foreach[Message] {
    case tm: TextMessage => tm.toStrict(1 minute)
      .map(_.text)
      .map(s => JsonParser.apply(s))
      .map(implicitly[JsonReader[MessageWrapper]].read)
      .foreach { wrapper =>
        wrapper.clientMessage match {
          case api.Register(_) => Unit
          case api.Sync(_) => Unit
          case api.Update(_, _) => Unit
        }
      }

    case _ => throw new IllegalArgumentException("")
  }

//  private def dealWithMessage(spot: Option[PaintedSpot]): Unit = {
//    println(s"Updating spot: $spot")
//    spot.foreach(s =>
//      area = area updated(s.point, s.color)
//    )
//  }
//
//  def out: Source[Message, NotUsed] = Source.repeat(true)
//    .map(_ => area)
//    .throttle(1, 1 second)
//    .map(map => TextMessage.apply {
//      val message =
//        map
//        .toSeq
//        .map { case (p, c) => s"x${p.x}y${p.y}r${c.getRed}g${c.getGreen}b${c.getBlue}" }.mkString(",")
//      println("Sending message: " + message)
//      message
//    }.asInstanceOf[Message])


  def out: Source[Message, ActorRef[Update]] = ActorSource.actorRef[Update](
    completionMatcher = {
    case _ =>
  }, PartialFunction.empty, bufferSize = 1000, overflowStrategy = OverflowStrategy.backpressure)
    .map(u => TextMessage.apply {
      val message =
          s"x${u.update.point.x}" +
            s"y${u.update.point.y}" +
            s"r${u.update.color.getRed}" +
            s"g${u.update.color.getGreen}" +
            s"b${u.update.color.getBlue}"
      println("Sending message: " + message)
      message
    }.asInstanceOf[Message])

  //take ref and assigned to device????

  val requestHandler: HttpRequest => HttpResponse = {
    case req @ HttpRequest(HttpMethods.GET, Uri.Path("/game"), _, _, _) =>
      req.header[UpgradeToWebSocket] match {
        case Some(upgrade) => upgrade.handleMessagesWithSinkSource(inSink = in, outSource = out)
        case None          => HttpResponse(400, entity = "Not a valid websocket request!")
      }
    case r: HttpRequest =>
      r.discardEntityBytes() // important to drain incoming HTTP Entity stream
      HttpResponse(404, entity = "Unknown resource!")
  }

  val bindingFuture = Http().bindAndHandleSync(requestHandler, "localhost", 8080)
  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}