// Copyright: 2010 - 2016 Rory Graves, Sam Halliday
// License: http://www.apache.org/licenses/LICENSE-2.0
package fommil

import scala.annotation.StaticAnnotation
import scala.reflect.macros._

import macrocompat.bundle

import scala.language.experimental.macros

class data extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro DataMacro.companion
}

@bundle
class DataMacro(val c: whitebox.Context) {
  import c.universe._

  def companion(annottees: c.Expr[Any]*): c.Expr[Any] = {
    val anntTrees = annottees.map(_.tree).toList
    val outputs: List[c.universe.Tree] = anntTrees.flatMap {
      case clz: ClassDef =>
        val term = TermName(clz.name.toString)

        clz.impl.body.collectFirst {
          case d @ DefDef(_, termNames.CONSTRUCTOR, _, params, _, _) => (params, d.pos)
        } match {
          case None =>
            // traits don't have constructor parameters
            List(clz, q"""object $term""")

          case Some((ps, pos)) =>
            List(
              clz,
              q"""
               object $term {
                 def apply(${ps}): ${clz.name} = null
               }
               """
            )
        }

      case objDef: ModuleDef => List(objDef)
    }

    c.Expr[Any](Block(outputs, Literal(Constant(()))))
  }

}
