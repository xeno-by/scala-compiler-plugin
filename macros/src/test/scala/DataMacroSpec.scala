// Copyright: 2010 - 2016 Rory Graves, Sam Halliday
// License: http://www.apache.org/licenses/LICENSE-2.0

// intentionally not in the same package as the plugin
package testing

import org.scalatest._
import org.scalatest.Matchers._

import fommil.data

@data
class Me

@data
class Myself(val foo: String, val bar: Long)

class Irene

@data
trait Mallgan

@data
object MyObj {
  def apply(foo: String, bar: Long): Me = null
}

@data
class Mine(val foo: String = "foo", val bar: Long = 13)

class DataMacroSpec extends FlatSpec {
  "@data" should "generate companion's apply with no parameters" in {
    { Me(): Me } shouldNot be(null)
  }

  it should "create a companion for Mallgan" in {
    Mallgan shouldBe a[Mallgan.type]
  }

  it should "generate companion apply with parameters" in {
    { Myself("foo", 23L): Myself } shouldNot be(null)
  }

  it should "generate companion apply with named / default parameters" in {
    { Mine("foo"): Mine } shouldNot be(null)

    { Mine(foo = "foo"): Mine } shouldNot be(null)

    { Mine(bar = 10): Mine } shouldNot be(null)
  }

  it should "not create anything not @data" in {
    "Irene" shouldNot compile
  }

}
