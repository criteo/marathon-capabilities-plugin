organization := "com.criteo"
name := "marathon-capabilities-plugin"
scalaVersion := "2.12.7"

resolvers += Resolver.sonatypeRepo("releases")
version := "1.0"

resolvers += "Mesosphere Public Repo" at "http://downloads.mesosphere.io/maven"

libraryDependencies += "mesosphere.marathon" %% "plugin-interface" % "1.6.325" % "provided"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2" % "provided"

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.4"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.4" % Test
libraryDependencies += "org.scalamock" %% "scalamock" % "4.0.0" % Test