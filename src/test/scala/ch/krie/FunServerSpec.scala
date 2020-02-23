package ch.krie

import akka.util.ByteString
import ch.krie.FunServer.PaintedSpot
import org.scalatest.{FunSpec, Matchers}

/*
 *   describe("A Stack") {
 *
 *     describe("(when empty)") {
 *
 *       it("should be empty") {
 *         assert(emptyStack.empty)
 *       }
 *       // ...
 */
class FunServerSpec extends FunSpec with Matchers {

  describe("The server") {
    it("should parse utf-8 strings") {
      val byteString = ByteString("0x1y1r233g123b1", "utf-8")

      MessageReader.fromBytes(byteString) shouldBe Some(
        PaintedSpot(1, 1, 233, 123, 1)
      )
    }
  }
}
