package ch.krie

import java.awt.{Color, Point}

import akka.util.ByteString
import ch.krie.FunServer.PaintedSpot

object MessageReader {

  def fromBytes(byteString: ByteString): Option[PaintedSpot] = {
    val message = byteString.decodeString("utf-8")

    println("decoded " + message)

    fromString(message)
  }

  def fromString(message: String): Option[PaintedSpot] = {
    val Pattern = "[0,1]?x(\\d+)y(\\d+)r(\\d+)g(\\d+)b(\\d+)".r

    message match {
      case Pattern(x,y,r,g,b) =>

        println(s"$x $y $r $g $b")

        Some(PaintedSpot(
          point = new Point(x.toInt, y.toInt),
          color = new Color(r.toInt, g.toInt, b.toInt)
        ))

      case _ => None
    }
  }
}
