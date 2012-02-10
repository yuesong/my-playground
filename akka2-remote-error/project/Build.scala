import sbt._
import Keys._
import cc.spray.revolver.RevolverPlugin._
import com.typesafe.sbteclipse.plugin.EclipsePlugin._

object MyBuild extends Build {

  // import Repositories._
  import Dependencies._

  lazy val root = Project("akka2-remote-error", file("."), settings = 
    Defaults.defaultSettings ++ 
    Revolver.settings ++
    Seq(
      organization := "net.yuesong",
      name := "akka2-remote-error",
      version := "1.0-SNAPSHOT",

      scalaVersion := "2.9.1",
      scalacOptions ++= Seq("-deprecation", "-unchecked", "-explaintypes", "-encoding", "utf8", "-Xcheckinit"),

      // disable checksums check for dependencies! otherwise headache!!!
      checksums := Nil,
      libraryDependencies ++= Seq(
        "com.typesafe.akka" % "akka-actor" % AkkaVersion,
        "com.typesafe.akka" % "akka-remote" % AkkaVersion
      ),

      // sbt eclipse plugin setup
      EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource,
      EclipseKeys.withSource := true,

      // use regular sbt run command for remote debugging
      fork in (Compile,run) := true,
      javaOptions in (Compile,run) ++= Seq("-Xdebug", "-Xnoagent", "-Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y")
    )
  )
}

object Dependencies {
  final val AkkaVersion = "2.0-M4"
}
