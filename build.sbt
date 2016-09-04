// mkdir -p {macros,plugins}/src/{main,test}/{scala,scala-2.10,scala-2.11,java,resources}

lazy val macros = project

lazy val plugins = project.settings(
  scalacOptions in Test <++= (packageBin in Compile) map { jar =>
    // needs timestamp to force recompile
    Seq("-Xplugin:" + jar.getAbsolutePath, "-Jdummy=" + jar.lastModified)
  }
)
