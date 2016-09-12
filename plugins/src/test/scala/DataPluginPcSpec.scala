// Copyright: 2010 - 2016 Rory Graves, Sam Halliday
// License: http://www.apache.org/licenses/LICENSE-2.0

// intentionally not in the same package as the plugin
package pctesting

import org.scalatest._
import org.scalatest.Matchers._

import org.scalatest._
import org.scalatest.Matchers._

import org.ensime.pcplod._
import testing.Logging

class DataPluginPcSpec extends FlatSpec with Logging {

  "@data" should "handle definitions of @data" in withMrPlod("classes.scala") { mr: MrPlod =>
    mr.messages shouldBe Nil

    mr.symbolAtPoint('me) shouldBe Some("pctesting.Me")
    mr.typeAtPoint('me) shouldBe Some("pctesting.Me")

    mr.symbolAtPoint('myself) shouldBe Some("pctesting.Myself")
    mr.typeAtPoint('myself) shouldBe Some("pctesting.Myself")

    mr.symbolAtPoint('foo) shouldBe Some("pctesting.Myself.foo")
    mr.typeAtPoint('foo) shouldBe Some("String")
  }

}
