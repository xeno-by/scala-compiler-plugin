// Copyright: 2010 - 2016 Rory Graves, Sam Halliday
// License: http://www.apache.org/licenses/LICENSE-2.0

// intentionally not in the same package as the plugin
package pctesting

import fommil.data

@data class M@me@e()

@data class My@myself@self(val fo@foo@o: String, val bar: Long)

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
