package ch.krie.api

import ch.krie.PaintedSpot
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

final case class Point(x: Int, y: Int)
final case class Color(r: Int, g: Int, b: Int)
final case class NewSpot(point: Point, color: Color) {

  def toPaintedSpot: PaintedSpot = {
    PaintedSpot(point.x, point.y, color.r, color.g, color.b)
  }
}

object NewSpot {
  implicit val pointFormat: RootJsonFormat[Point] = jsonFormat2(Point.apply)
  implicit val colorFormat: RootJsonFormat[Color] = jsonFormat3(Color.apply)
  implicit val spotFormat: RootJsonFormat[NewSpot] = jsonFormat2(NewSpot.apply)
}