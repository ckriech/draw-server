package ch.krie.api

import spray.json.DefaultJsonProtocol._
import spray.json.{JsValue, JsonReader, JsonWriter, RootJsonFormat}

import scala.util.Try


final case class Point(x: Int, y: Int)
final case class Color(r: Int, g: Int, b: Int)
final case class NewSpot(point: Point, color: Color)

sealed trait ClientMessage
final case class Register(selfId: String) extends ClientMessage
final case class Sync(selfId: String) extends ClientMessage
final case class Update(selfId: String, newSpot: NewSpot) extends ClientMessage

final case class MessageWrapper(clientMessage: ClientMessage)

object MessageWrapper {
  implicit val pointFormat: RootJsonFormat[Point] = jsonFormat2(Point.apply)
  implicit val colorFormat: RootJsonFormat[Color] = jsonFormat3(Color.apply)
  implicit val spotFormat: RootJsonFormat[NewSpot] = jsonFormat2(NewSpot.apply)

  implicit val registerFormat: RootJsonFormat[Register] = jsonFormat1(Register.apply)
  implicit val syncFormat: RootJsonFormat[Sync] = jsonFormat1(Sync.apply)
  implicit val updateFormat: RootJsonFormat[Update] = jsonFormat2(Update.apply)

  implicit val messageFormat: RootJsonFormat[ClientMessage] = new RootJsonFormat[ClientMessage] {
    override def read(json: JsValue): ClientMessage = {
      val tryReg = Try(implicitly[JsonReader[Register]].read(json)).toOption
      val trySync = Try(implicitly[JsonReader[Sync]].read(json)).toOption
      val tryUpdate  = Try(implicitly[JsonReader[Update]].read(json)).toOption

      Seq(tryReg, trySync, tryUpdate)
        .flatten
        .headOption
        .fold(throw new IllegalArgumentException(s"Couldn't read the client message! json=$json"))(
          msg => msg
        )
    }

    override def write(obj: ClientMessage): JsValue = {
      case r:Register => implicitly[JsonWriter[Register]].write(r)
      case s:Sync => implicitly[JsonWriter[Sync]].write(s)
      case u:Update => implicitly[JsonWriter[Update]].write(u)
    }
  }

  implicit val format: RootJsonFormat[MessageWrapper] = jsonFormat1(MessageWrapper.apply)
}