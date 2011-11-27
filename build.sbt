import com.typesafe.startscript.StartScriptPlugin

seq(StartScriptPlugin.startScriptForClassesSettings: _*)

name := "Hanuman"

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
  "org.scala-tools.time"      %%  "time"            % "latest.integration" % "compile",
  "se.scalablesolutions.akka" %  "akka-actor"       % "latest.integration" % "compile" withSources(),
  "se.scalablesolutions.akka" %  "akka-stm"         % "latest.integration" % "compile" withSources(),
  "se.scalablesolutions.akka" %  "akka-typed-actor" % "latest.integration" % "compile" withSources(),
  "com.reportgrid"            %% "blueeyes"         % "0.4.26"             % "compile" withSources(),
  "org.scalatest"             %% "scalatest"        % "latest.integration" % "test"    withSources()
)