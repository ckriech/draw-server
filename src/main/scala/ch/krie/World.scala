package ch.krie

import java.awt.{Color, Point}

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.LoggerOps
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

object World {

  sealed trait WorldRequest
  final case class Register(deviceId: String, out: ActorRef[Device.Update]) extends WorldRequest
  final case class Sync(deviceId: String) extends WorldRequest
  final case class Update(deviceId: String, update: PaintedSpot) extends WorldRequest

  def apply(): Behavior[WorldRequest] = {
    worldBot(Map.empty)
  }

  //no restrictions on map right now
  def worldBot(map: Map[Point, Color]): Behavior[WorldRequest] = Behaviors.receive { (ctx, msg) =>
    msg match {
      case Register(deviceId, ref) =>
        ctx.log.info("I should be registering device right now")
        //spawn baby
        ctx.spawn(Device(deviceId, ref), deviceId)
        ctx.self.tell(Sync(deviceId))
        Behaviors.same
      case Sync(deviceId) =>
        ctx.log.info("I should be forwarding the whole world state right now")
        //forward all world to one babe
        Behaviors.same
      case Update(deviceId, update: PaintedSpot) =>
        ctx.log.info("I should be updating the record of my state and sending an update to my babes")
        //update map and forward single update to every babe
        Behaviors.same
    }
  }
}
