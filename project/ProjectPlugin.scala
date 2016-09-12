import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import de.heikoseeberger.sbtheader.HeaderKey
import sbt._
import sbt.Keys._
import scalariform.formatter.preferences._

object ProjectPlugin extends SensiblePlugin {

  override def projectSettings = Seq(
    scalaVersion := "2.11.8",
    organization := "com.fommil",
    version := "1.0.0-SNAPSHOT",
    HeaderKey.headers := Copyright.ApacheMap,
    ScalariformKeys.preferences := FormattingPreferences().setPreference(AlignSingleLineCaseStatements, true),
    libraryDependencies ++= Sensible.testLibs() ++ Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.ensime" %% "pcplod" % "1.0.0" % "test"
    ),
    dependencyOverrides += "org.scala-lang" % "scala-compiler" % scalaVersion.value,
    javaOptions in Test ++= Seq(
      "-Dlogback.configurationFile=../logback-test.xml"
    )
  ) ++ Sensible.settings ++ SonatypeSupport.sonatype("fommil", "scala-compiler-plugin", SonatypeSupport.Apache2)

}
