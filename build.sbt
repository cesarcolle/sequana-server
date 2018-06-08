name := "sequana-server"

version := "0.1"

scalaVersion := "2.12.6"

mainClass in Compile := Some("WebServer")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.12",
  "com.typesafe.akka" %% "akka-stream" % "2.5.12",
  "com.typesafe.akka" %% "akka-http" % "10.1.1",
)
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.1"