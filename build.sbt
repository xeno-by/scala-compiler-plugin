lazy val macro = project

lazy val plugin = project.settings(
  scalacOptions in Test <++= (packageBin in Compile) map { jar =>
    // needs timestamp to force recompile
    Seq("-Xplugin:" + jar.getAbsolutePath, "-Jdummy=" + jar.lastModified)
  }
)
