package ch.krie

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, Terminated}

//https://github.com/johanandren/chat-with-akka-http-websockets/blob/akka-2.6/src/main/scala/chat/ChatRoom.scala#L1
object World {
  sealed trait Command
  case class Join(user: ActorRef[DrawMessage]) extends Command
  case class DrawMessage(paintedSpot: PaintedSpot) extends Command

  def apply(): Behavior[Command] = run(Set.empty, Seq.empty)

  private def run(users: Set[ActorRef[DrawMessage]], messageHistory: Seq[DrawMessage]): Behavior[Command] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage[Command] {
        case Join(user) =>
          // watch so we can remove the user when if actor is stopped
          context.watch(user)
          messageHistory.foreach(msg => user ! msg)
          run(users + user, messageHistory)
        case msg: DrawMessage =>
          users.foreach(_ ! msg)
          run(users, messageHistory :+ msg)
      }.receiveSignal {
        case (__, Terminated(user)) =>
          run(users.filterNot(_ == user), messageHistory)
      }
    }
}
