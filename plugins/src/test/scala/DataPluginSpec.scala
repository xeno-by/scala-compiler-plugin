// Copyright: 2010 - 2016 Rory Graves, Sam Halliday
// License: http://www.apache.org/licenses/LICENSE-2.0

// intentionally not in the same package as the plugin
package testing

import org.scalatest._
import org.scalatest.Matchers._

import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler

import fommil.data

@data class Me()

@data class Myself(val foo: String, val bar: Long)

class Irene()

@data object Thingy

@data class Foo(foo: String, bar: Long) {
  val baz: String = foo // shouldn't be in constructor
}
object Foo {
  def ignore(foo: String, bar: Long): Foo = new Foo(foo, bar)
}

@data class Baz[T](val fred: T)

@data class Mine(val foo: String = "foo", val bar: Long = 13)

@data class Covariant[+I](item: I)

trait Logging {
  SLF4JBridgeHandler.removeHandlersForRootLogger()
  SLF4JBridgeHandler.install()
  val log = LoggerFactory.getLogger(this.getClass)
}

class DataPluginSpec extends FlatSpec with Logging {

  "@data" should "generate equals" in {
    new Me() shouldBe new Me()
    new Myself("a", 1) shouldBe new Myself("a", 1)
    new Myself("a", 1) shouldNot be(null)
    new Myself("a", 1) shouldNot be(new Myself("a", 2))
  }

  it should "generate hashCode" in {
    val me = new Me()
    me.hashCode shouldNot be(System.identityHashCode(me))
    new Myself("a", 1).hashCode shouldBe new Myself("a", 1).hashCode
    new Myself("a", 1).hashCode shouldNot be(new Myself("a", 2).hashCode)
  }

  it should "generate toString" in {
    new Me().toString shouldBe "Me()"
    new Myself("a", 1).toString shouldBe "Myself(a,1)"
  }

  it should "generate companion's apply with no parameters" in {
    { Me(): Me } shouldBe new Me()
  }

  it should "generate companion apply with parameters" in {
    { Myself("foo", 23L): Myself } shouldBe new Myself("foo", 23L)
  }

  it should "generate companion unapply" in {
    { Myself("foo", 23L): Myself } should matchPattern {
      case Myself("foo", 23L) =>
    }
  }

  it should "generate companion unapply for an object" in {
    { Thingy: Thingy.type } should matchPattern {
      case Thingy =>
    }
  }

  it should "update Foo's companion" in {
    Foo.ignore("foo", 13L) shouldBe a[Foo]

    { Foo("foo", 13L): Foo } shouldBe new Foo("foo", 13L)
  }

  it should "generate companion apply with named / default parameters" in {
    { Mine("foo"): Mine } shouldBe new Mine("foo")

    { Mine(foo = "foo"): Mine } shouldBe new Mine("foo", 13L)

    { Mine(bar = 10): Mine } shouldBe new Mine("foo", 10L)
  }

  it should "not create anything not @data" in {
    "Irene" shouldNot compile
  }

  it should "handle typed classes" in {
    { Baz("hello"): Baz[String] } shouldBe new Baz("hello")
  }

  it should "handle covariant types" in {
    { Covariant(""): Covariant[String] } shouldBe new Covariant("")
  }

}
