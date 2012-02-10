resolvers ++= Seq(
  "spray repo" at "http://repo.spray.cc"
)

addSbtPlugin("cc.spray" % "sbt-revolver" % "0.6.0")

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.0.0-RC1")
