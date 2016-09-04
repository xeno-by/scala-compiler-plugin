// mkdir -p {macros,plugins}/src/{main,test}/{scala,scala-2.10,scala-2.11,java,resources}

lazy val macros = project.settings(
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
  libraryDependencies += "org.typelevel" %% "macro-compat" % "1.1.0"
)

lazy val plugins = project.settings(
  scalacOptions in Test <++= (packageBin in Compile) map { jar =>
    // needs timestamp to force recompile
    Seq("-Xplugin:" + jar.getAbsolutePath, "-Jdummy=" + jar.lastModified)
  }
)
