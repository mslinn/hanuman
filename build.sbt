import com.typesafe.startscript.StartScriptPlugin

seq(StartScriptPlugin.startScriptForClassesSettings: _*)

name := "hanuman"

version := "0.1"

scalaVersion := "2.9.1"

scalacOptions ++= Seq("-deprecation")

resolvers ++= Seq(
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases",
  "Sonatype"          at "http://nexus.scala-tools.org/content/repositories/public",
  "Scala Tools"       at "http://scala-tools.org/repo-releases/",
  "JBoss"             at "http://repository.jboss.org/nexus/content/groups/public/",
  "GuiceyFruit"       at "http://guiceyfruit.googlecode.com/svn/repo/releases/"
)

libraryDependencies ++= Seq(
  "se.scalablesolutions.akka" % "akka-actor" % "1.2" % "compile" withSources(),
  "se.scalablesolutions.akka" % "akka-stm" % "1.2" % "compile" withSources(),
  "se.scalablesolutions.akka" % "akka-typed-actor" % "1.2" % "compile" withSources(),
  "com.reportgrid" % "blueeyes_2.9.1" % "0.4.26" % "compile" withSources(),
  "org.scalatest" %% "scalatest" % "1.6.1" % "test" withSources()
)