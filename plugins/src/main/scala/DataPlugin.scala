// Copyright: 2010 - 2016 Rory Graves, Sam Halliday
// License: http://www.apache.org/licenses/LICENSE-2.0
package fommil

import scala.collection.breakOut
import scala.tools.nsc._
import scala.tools.nsc.plugins._
import scala.tools.nsc.transform._

class DataPlugin(override val global: Global) extends Plugin {
  override val name: String = "data"
  override val description: String = s"Generates code when using an annotation named '$name'"

  abstract class TransformingComponent(override val global: Global) extends PluginComponent
    with TypingTransformers with WithPos with IsIde with BackCompat {
    val trigger = global.newTypeName(DataPlugin.this.name)

    override def newPhase(prev: Phase): Phase = new StdPhase(prev) {
      override def apply(unit: global.CompilationUnit): Unit = newTransformer(unit).transformUnit(unit)
    }
    override val runsAfter: List[String] = "parser" :: Nil
    override val runsBefore: List[String] = "namer" :: Nil

    def newTransformer(unit: global.CompilationUnit) = new TypingTransformer(unit) {
      override def transform(tree: global.Tree): global.Tree = {
        TransformingComponent.this.transform(super.transform(tree))
      }
    }

    def transform: global.Tree => global.Tree
  }

  // workaround for https://issues.scala-lang.org/browse/SI-9612
  private val parameters = new TransformingComponent(global) {
    override val phaseName: String = DataPlugin.this.name + "-params"
    import global._

    def parameters(mods: global.Modifiers) = {
      mods.annotations.map {
        case ann @ Apply(Select(New(Ident(trigger)), nme.CONSTRUCTOR), _) =>
          // NOTE: this simple impl ignores all parameters, even bad ones
          treeCopy.Apply(
            ann,
            ann.fun,
            Literal(Constant(false)) :: Nil
          )

        case other => other
      }
    }

    override def transform = {
      case t: ModuleDef if t.mods.hasAnnotationNamed(trigger) =>
        val annotations = parameters(t.mods)
        treeCopy.ModuleDef(t, t.mods.copyWithAnns(annotations), t.name, t.impl)

      case t: ClassDef if t.mods.hasAnnotationNamed(trigger) =>
        val annotations = parameters(t.mods)
        treeCopy.ClassDef(t, t.mods.copyWithAnns(annotations), t.name, t.tparams, t.impl)

      case t => t
    }
  }

  private val companion = new TransformingComponent(global) {
    override val phaseName: String = DataPlugin.this.name + "-companion"
    import global._

    // best way to inspect a tree, just call this
    def debug(name: String, tree: Tree): Unit = {
      global.reporter.warning(tree.pos, s"$name: ${showCode(tree)}\n${showRaw(tree)}")
    }

    /** generates a zero-functionality companion for a class */
    def genCompanion(clazz: ClassDef): ModuleDef = {
      debug("genCompanion", clazz)
      val name = TermName(clazz.name.toString)
      q"object ${name}"
    }

    /** adds hashCode, equals and toString to a class */
    def updateClass(clazz: ClassDef): ClassDef = {
      debug("updateClass", clazz)
      clazz
    }

    /** adds apply and unapply to a companion to a class */
    def updateCompanion(clazz: ClassDef, companion: ModuleDef): ModuleDef = {
      debug("updateCompanion (clazz)", clazz)
      debug("updateCompanion (companion)", clazz)
      companion
    }

    def hascaseClasses(t: PackageDef): Boolean =
      t.stats.collectFirst {
        case c: ClassDef if c.mods.hasAnnotationNamed(trigger) => c
      }.isDefined

    override def transform = {
      case t: PackageDef if hascaseClasses(t) =>
        val classes: Map[TypeName, ClassDef] = t.stats.collect {
          case c: ClassDef => c.name -> c
        }(breakOut)

        val companions: Map[TermName, ModuleDef] = t.stats.collect {
          case m: ModuleDef => m.name -> m
        }(breakOut)

        object ClassNoCompanion {
          def unapply(t: Tree): Option[ClassDef] = t match {
            case c: ClassDef if !companions.contains(c.name.companionName) => Some(c)
            case _ => None
          }
        }

        object ClassHasCompanion {
          def unapply(t: Tree): Option[ClassDef] = t match {
            case c: ClassDef if companions.contains(c.name.companionName) => Some(c)
            case _ => None
          }
        }

        object CompanionAndClass {
          def unapply(t: Tree): Option[(ModuleDef, ClassDef)] = t match {
            case m: ModuleDef =>
              classes.get(m.name.companionName).map { c => (m, c) }
            case _ => None
          }
        }

        val updated = t.stats.flatMap {
          case ClassNoCompanion(c) if c.mods.hasAnnotationNamed(trigger) =>
            val companion = updateCompanion(c, genCompanion(c))
            List(updateClass(c), companion)

          case ClassHasCompanion(c) if c.mods.hasAnnotationNamed(trigger) =>
            List(updateClass(c))

          case CompanionAndClass(companion, c) if c.mods.hasAnnotationNamed(trigger) =>
            List(updateCompanion(c, companion))

          case t =>
            List(t)
        }

        treeCopy.PackageDef(t, t.pid, updated)

      case t => t
    }

  }

  override val components = List(parameters, companion)
}
