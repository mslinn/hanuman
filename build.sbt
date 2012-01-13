// see https://github.com/sbt/sbt-assembly
import AssemblyKeys._ // put this at the top of the file

name := "Hanuman"

version := "0.1"

scalaVersion := "2.9.1"

scalacOptions ++= Seq("-deprecation")

seq(assemblySettings: _*)

resolvers ++= Seq(
  "Typesafe Snapshots"    at "http://repo.typesafe.com/typesafe/snapshots",
  "Typesafe Releases"     at "http://repo.typesafe.com/typesafe/releases",
  "Scala-Tools Snapshots" at "http://scala-tools.org/repo-snapshots",
  "Scala Tools Releases"  at "http://scala-tools.org/repo-releases",
  "Sonatype"              at "http://nexus.scala-tools.org/content/repositories/public",
  "JBoss"                 at "http://repository.jboss.org/nexus/content/groups/public",
  "GuiceyFruit"           at "http://guiceyfruit.googlecode.com/svn/repo/releases"
)

libraryDependencies ++= Seq(
  "com.reportgrid"       %% "blueeyes"   % "0.4.26"             % "compile" withSources(),
  "org.scalatest"        %% "scalatest"  % "latest.integration" % "test"    withSources(),
  "org.scala-tools.time" %% "time"       % "latest.integration" % "compile",
  "com.typesafe.akka"    %  "akka-actor" % "latest.milestone"   % "compile" withSources(),
  "org.scala-tools"      %% "scala-stm"  % "0.5-SNAPSHOT"       % "compile" withSources()
)