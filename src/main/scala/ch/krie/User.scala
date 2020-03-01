package ch.krie


import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import ch.krie.AckingReceiver.Ack

object User {
  sealed trait Event
  case class Connected(outgoing: ActorRef[OutgoingMessage], ackTo:ActorRef[Ack]) extends Event
  case object Disconnected extends Event
  case class IncomingMessage(paintedSpot: PaintedSpot) extends Event
  case class OutgoingMessage(text: String) extends Event

  def apply(chatRoom: ActorRef[World.Command]): Behavior[Event] =
    waitingForConnection(chatRoom)

  private def waitingForConnection(world: ActorRef[World.Command]): Behavior[Event] =
    Behaviors.setup { context =>
      Behaviors.receiveMessagePartial {
        case Connected(outgoing, ackTo) =>
          world ! World.Join(context.messageAdapter {
            case World.DrawMessage(update) => {
              val message =
                s"x${update.point.x}" +
                  s"y${update.point.y}" +
                  s"r${update.color.getRed}" +
                  s"g${update.color.getGreen}" +
                  s"b${update.color.getBlue}"
              println("Sending message: " + message)
              OutgoingMessage(message)
            }
          })
          ackTo ! AckingReceiver.Ack
          connected(world, outgoing, ackTo)
      }
    }

  private def connected(chatRoom: ActorRef[World.Command], outgoing: ActorRef[OutgoingMessage], ackTo: ActorRef[Ack]): Behavior[Event] =
    Behaviors.receiveMessagePartial {
      case IncomingMessage(updated) =>
        chatRoom ! World.DrawMessage(updated)
        ackTo ! AckingReceiver.Ack
        Behaviors.same
      case msg: OutgoingMessage =>
        outgoing ! msg
        ackTo ! AckingReceiver.Ack
        Behaviors.same
      case Disconnected =>
        Behaviors.stopped
    }
}