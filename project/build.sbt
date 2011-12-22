resolvers += "sbt-idea-repo" at "http://mpeltonen.github.com/maven/"

resolvers += Classpaths.typesafeResolver

addSbtPlugin("com.github.mpeltonen"     % "sbt-idea"                 % "latest.milestone")

addSbtPlugin("com.typesafe.sbteclipse"  % "sbteclipse"               % "latest.milestone")

addSbtPlugin("com.typesafe.startscript" % "xsbt-start-script-plugin" % "latest.milestone")

addSbtPlugin("com.eed3si9n"             % "sbt-assembly"             % "latest.milestone")

