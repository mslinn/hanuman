resolvers += "sbt-idea-repo" at "http://mpeltonen.github.com/maven/"

resolvers += Classpaths.typesafeResolver

addSbtPlugin("com.github.mpeltonen"     % "sbt-idea"                 % "latest.integration")

addSbtPlugin("com.typesafe.sbteclipse"  % "sbteclipse"               % "1.5.0")

addSbtPlugin("com.typesafe.startscript" % "xsbt-start-script-plugin" % "latest.integration")
