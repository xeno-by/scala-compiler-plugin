// mkdir -p {macros,plugins}/src/{main,test}/{scala,scala-2.10,scala-2.11,java,resources}

lazy val macros = project.settings(
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
  libraryDependencies += "org.typelevel" %% "macro-compat" % "1.1.0",
  javaOptions in Test += s"-Dpcplod.plugin=/Users/xeno_by/Projects/Paradise2118/plugin/target/scala-2.11/paradise_2.11.8-2.1.0-SNAPSHOT.jar"
)

lazy val plugins = project.settings(
  scalacOptions in Test <++= (packageBin in Compile) map { jar =>
    // needs timestamp to force recompile
    Seq("-Xplugin:" + jar.getAbsolutePath, "-Jdummy=" + jar.lastModified)
  }
)
