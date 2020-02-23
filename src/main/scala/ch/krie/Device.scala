package ch.krie

import ch.krie.FunServer.PaintedSpot
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.LoggerOps
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import ch.krie.FunServer.PaintedSpot


object Device {

  final case class Update(update: PaintedSpot)

  def apply(deviceId: String, out: ActorRef[Update]): Behavior[Update] = Behaviors.receive { (ctx, msg) =>
    out.tell(msg)
    Behaviors.same
  }
}

/*

akka-http server ->
  sink (in): either REGISTER, SYNC, UPDATE
    - REGISTER: cool, connection is already made, so send back worldId
    - SYNC: Sure, that sure is a worldId. I'll send you specifically everything the world has so far.
    - UPDATE: Neat, thanks, we'll send this to the world actor to hold onto.
  source (out): UPDATE
    - UPDATE: Hey, someone (it could have been you tbh) did something, here's the thing.


    [Device]
    |
    |
    |
    v
    (server) []
    |
    |
    |
    (world actor) [contains the current map] ----------> (device) [forwards messages back to device]

 */
