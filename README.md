# scala-compiler-plugin

Exercise material for Scala World 2016 talk "Return of the Scala Compiler Plugin"

## ENSIME Support

To get ensime support, create a file `ensime.sbt` with the following contents

```scala
import org.ensime.CommandSupport._

EnsimeKeys.ensimeCompilerArgs <+= state.map { implicit s =>
  implicit val structure = Project.extract(s).structure
  implicit val plugin = structure.allProjectRefs.find(_.project == "plugins").get
  val jar = (packageBin in plugin in Compile).run
  s"-Xplugin:${jar.getCanonicalPath}"
}
```
