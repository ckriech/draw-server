package ch.krie

import java.awt.{Color, Point}


case class PaintedSpot(point: Point, color: Color)

object PaintedSpot {
  def apply(x: Int, y: Int, r: Int, g: Int, b: Int): PaintedSpot =
    PaintedSpot(new Point(x, y), new Color(r, g, b))
}
