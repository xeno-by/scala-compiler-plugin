// Copyright: 2010 - 2016 Rory Graves, Sam Halliday
// License: http://www.apache.org/licenses/LICENSE-2.0

// intentionally not in the same package as the plugin
package pctesting

import org.scalatest._
import org.scalatest.Matchers._

import org.scalatest._
import org.scalatest.Matchers._

import org.ensime.pcplod._

class DataMacroPcSpec extends FlatSpec {

  "@data" should "handle definitions of @data" in withPcPlod { pc =>
    val f = "classes.scala"
    pc.loadScala(f)
    pc.messages shouldBe Nil

    pc.symbolAtPoint(f, 'me) shouldBe Some("pctesting.Me")
    pc.typeAtPoint(f, 'me) shouldBe Some("pctesting.Me")

    pc.symbolAtPoint(f, 'myself) shouldBe Some("pctesting.Myself")
    pc.typeAtPoint(f, 'myself) shouldBe Some("pctesting.Myself")

    pc.symbolAtPoint(f, 'foo) shouldBe Some("pctesting.Myself.foo")
    pc.typeAtPoint(f, 'foo) shouldBe Some("String")
  }

}
