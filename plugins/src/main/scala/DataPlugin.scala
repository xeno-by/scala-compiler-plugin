// Copyright: 2010 - 2016 Rory Graves, Sam Halliday
// License: http://www.apache.org/licenses/LICENSE-2.0
package fommil

import scala.collection.breakOut
import scala.reflect.internal.ModifierFlags
import scala.tools.nsc._
import scala.tools.nsc.plugins._
import scala.tools.nsc.transform._

class DataPlugin(override val global: Global) extends Plugin {
  override val name: String = "data"
  override val description: String = s"Generates code when using an annotation named '$name'"

  abstract class TransformingComponent(override val global: Global) extends PluginComponent with TypingTransformers with WithPos with IsIde {
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

  private val parameters = new TransformingComponent(global) {
    override val phaseName: String = DataPlugin.this.name + "-params"
    import global._

    def parameters(mods: global.Modifiers) = {
      mods.annotations.map {
        case ann @ Apply(Select(New(Ident(trigger)), nme.CONSTRUCTOR), _) =>
          // NOTE: ignores all parameters, even bad ones
          treeCopy.Apply(
            ann,
            ann.fun,
            Literal(Constant(true)) :: Literal(Constant(true)) :: Literal(Constant(0)) :: Nil
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

    /** generates a zero-functionality companion for a class */
    def genCompanion(clazz: ClassDef): ModuleDef = ModuleDef(
      Modifiers(),
      clazz.name.companionName,
      Template(
        List(Select(Ident(nme.scala_), nme.AnyRef.toTypeName)),
        emptyValDef,
        List(
          DefDef(
            Modifiers(),
            nme.CONSTRUCTOR,
            List(),
            List(List()),
            TypeTree(),
            Block(
              List(
                Apply(Select(Super(This(tpnme.EMPTY), tpnme.EMPTY), nme.CONSTRUCTOR), List())
              ),
              Literal(Constant(()))
            )
          )
        )
      )
    )

    def genLog: DefDef = DefDef(
      Modifiers(ModifierFlags.PRIVATE),
      newTermName("log"),
      Nil,
      Nil,
      Select(Select(Select(Ident(nme.java), newTermName("util")), newTermName("logging")), newTypeName("Logger")),
      Literal(Constant(null))
    )

    def genProperty(name: String, tpt: Tree): DefDef = DefDef(
      Modifiers(),
      newTermName(name),
      Nil,
      Nil,
      tpt,
      Literal(Constant(null))
    )

    def addMethods(template: Template, methods: List[DefDef]): Template = {
      val body = methods ::: template.body
      treeCopy.Template(template, template.parents, template.self, body)
    }

    /** adds a log method to a class */
    def updateClass(clazz: ClassDef): ClassDef = {
      val log = genLog
      log.setPos(clazz.pos)
      val impl = addMethods(clazz.impl, log :: Nil)
      treeCopy.ClassDef(clazz, clazz.mods, clazz.name, clazz.tparams, impl)
    }

    /** adds a apply and log methods to a companion to a class */
    def updateCompanion(clazz: ClassDef, companion: ModuleDef): ModuleDef = {
      // not sure these can be re-used, so create fresh ones
      //def name = newTermName(clazz.name.toString)
      def tpe = newTypeName(clazz.name.toString)
      def stripVariance(t: TypeDef): TypeDef = {
        val mods = t.mods &~ ModifierFlags.COVARIANT &~ ModifierFlags.CONTRAVARIANT
        val params = t.tparams.map(stripVariance)
        treeCopy.TypeDef(t, mods, t.name, params, t.rhs)
      }.duplicate

      clazz.impl.body.collectFirst {
        case d @ DefDef(_, nme.CONSTRUCTOR, _, params, _, _) => (params, d.pos)
      } match {
        case None => companion // traits don't have constructor parameters
        case Some((ps, pos)) =>
          val apply = DefDef(
            Modifiers(),
            newTermName("apply"),
            clazz.tparams.map(stripVariance),
            ps.map(_.map { v =>
              val mods = (v.mods &~ ModifierFlags.PARAMACCESSOR)
              treeCopy.ValDef(v, mods, v.name, v.tpt.duplicate, v.rhs)
            }),
            if (clazz.tparams.isEmpty) Ident(tpe)
            else AppliedTypeTree(Ident(tpe), clazz.tparams.map { t => Ident(t.name) }),
            // PC plugins don't need implementations
            Literal(Constant(null))
          )
          apply.setPos(pos)

          val log = genLog
          log.setPos(pos)

          val properties = ps.flatMap {
            vs =>
              vs.flatMap {
                v =>
                  // 1. need to take the type of v: V and turn it into Future[V]
                  // 2. add the type parameters of the class to the method (taking only what is needed for the method)
                  //genProperty(v.name.toString, Ident("scala.concurrent.Future[Unit]"))
                  Nil
              }
          }

          treeCopy.ModuleDef(
            companion, companion.mods, companion.name, {
            addMethods(companion.impl, apply :: log :: properties)
          }
          )
      }
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
