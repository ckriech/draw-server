package ch.krie

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object AckingReceiver {

  sealed trait Ack
  final case object Ack extends Ack

  def apply(): Behavior[Ack] = Behaviors.ignore
}
