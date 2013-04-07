import sbt._
import sbt.Keys._
import sbtrelease.ReleasePlugin._
import spray.revolver.RevolverPlugin._
import com.typesafe.sbteclipse.plugin.EclipsePlugin._

object MyBuild extends Build {

  import Repositories._
  import Dependencies._

  lazy val root = Project("qjob", file("."), settings = 
    Defaults.defaultSettings ++ 
    releaseSettings ++
    Revolver.settings ++
    Seq(
      organization := "com.addthis",
      name := "qjob",
      scalaVersion := "2.10.0",
      scalacOptions ++= Seq("-deprecation", "-unchecked", "-explaintypes", "-encoding", "utf8", "-Xcheckinit"),
      // disable checksums check for dependencies! otherwise headache!!!
      checksums := Nil,
      // look for dependencies only in cs nexus
      resolvers ++= Seq("spray repo" at "http://repo.spray.io", ClearspringNexus),
      externalResolvers <<= resolvers map { Resolver.withDefaultResolvers(_, mavenCentral = false) },
      // sbt eclipse plugin setup
      EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource,
      EclipseKeys.withSource := true,
      // shared test dependencies
      libraryDependencies ++= Seq(
        "io.spray" % "spray-can" % "1.1-M7",
        "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
        "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,
        "org.mockito" % "mockito-all" % MockitoVersion % "test",
        "org.specs2" %% "specs2" % Specs2Version % "test"
      ),
      // use target/logs as the logs dir for tests - otherwise it'd be created under project root which is annoying
      // testOptions in Test += Tests.Setup( () => sys.props += "qj.logs" -> logsDir.toString ),
      // publish to nexus snapshots/releases accordingly
      credentials += sbtCredentials,
      publishTo <<= version { v => if (v.endsWith("SNAPSHOT")) Some(NexusSnapshots) else Some(NexusReleases) }
    )
  )

  private def sbtCredentials = sys.props.get("sbt.credentials") match {
    case Some(v) => Credentials(file(v))
    case _       => Credentials(Path.userHome / ".ivy2" / ".credentials")
  }
}

object Repositories {
  final val NexusUrl = "http://buildspring/nexus/content/repositories/"
  final val ClearspringNexus = MavenRepository("Clearspring Nexus", NexusUrl + "public/")
  final val NexusSnapshots = MavenRepository("Snapshots", NexusUrl + "snapshots/")
  final val NexusReleases = MavenRepository("Releases", NexusUrl + "releases/")
}

object Dependencies {
  final val AkkaVersion = "2.1.0"
  final val ConfigVersion = "0.5.2"
  final val DispatchVersion = "0.8.8"
  final val DispatchLiftJsonVersion = "0.1.1.at1"
  final val UnfilteredVersion = "0.6.2"
  final val LiftVersion = "2.4"
  final val CsUtilVersion = "1.0.35"
  final val CsChunkVersion = "1.0.2"
  final val CommonsLang3Version = "3.1"
  final val CommonsIoVersion = "2.4"
  final val MetricsVersion = "2.0.3"
  final val ScoptVersion = "1.1.2"
  final val ScalaTimeVersion = "0.5"
  final val OpenCsvVersion = "2.3"
  final val FlywayVersion = "2.0.2" // Apache License, Version 2.0
  final val SquerylVersion = "0.9.5-2" // Apache License, Version 2.0
  final val C3p0Version = "0.9.2-pre4"
  final val EhcacheVersion = "2.6.0"
  final val MysqlVersion = "5.1.17"
  final val LogbackVersion = "1.0.0"
  final val Log4jVersion = "1.2.16"

  final val MockitoVersion = "1.9.5" // The MIT License
  final val Specs2Version = "1.14" // The MIT License
}
