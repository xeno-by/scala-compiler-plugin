// Copyright: 2010 - 2016 Rory Graves, Sam Halliday
// License: http://www.apache.org/licenses/LICENSE-2.0
package fommil

import scala.tools.nsc._
import scala.reflect.internal.util._

/**
 * A major source of bugs in macros and compiler plugins is failure to
 * preserve mutable information when transforming `global.Tree`
 * instances.
 *
 * Copying a `global.Tree` will result in the `pos`, `symbol` and
 * `tpe` fields being reset.
 *
 * The `global.treeCopy._` methods should be used instead of `copy` on
 * `global.Tree` implementations.
 *
 * Note that the `Modifiers.withAnnotations` method adds to existing
 * methods instead of replacing, so we provide a more cananocial form
 * here in `copyWithAnns`.
 *
 * http://docs.scala-lang.org/overviews/reflection/symbols-trees-types#trees
 */
trait WithPos { this: BackCompat =>
  val global: Global

  // Modifiers is an inner class, oh joy
  implicit class RichModifiers(t: global.Modifiers) {
    /** withAnnotations appears to be broken */
    def copyWithAnns(anns: List[global.Tree]): global.Modifiers =
      t.copy(annotations = anns).setPositions(t.positions)
  }

  implicit class RichTree[T <: global.Tree](t: T) {
    /** when generating a tree, use this to generate positions all the way down. */
    def withAllPos(pos: Position): T = {
      t.foreach(_.setPos(new TransparentPosition(pos.source, pos.startOrCursor, pos.endOrCursor, pos.endOrCursor)))
      t
    }
  }
}

/**
 * Convenient way to tell if you are running in an interactive context.
 */
trait IsIde {
  val global: Global

  val isIde = global.isInstanceOf[tools.nsc.interactive.Global]
}
