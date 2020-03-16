lazy val akkaHttpVersion = "10.1.11"
lazy val akkaVersion    = "2.6.3"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "ch.krie",
      scalaVersion    := "2.13.1"
    )),
    name := "server",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"                % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json"     % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"              % akkaVersion,
      "com.typesafe.akka" %% "akka-stream-typed"        % akkaVersion,
      "ch.qos.logback"    % "logback-classic"           % "1.2.3",

      "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"                % "3.0.8"         % Test
    )
  )

name := "Draw-server-prototype"

version := "0.1"

maintainer := "Chris Kriech <chris@krie.ch>"

packageSummary := "server for co-op drawing tool"

packageDescription := """blah blah blah"""

assemblyJarName in assembly := "assembly-project.jar"

enablePlugins(JavaServerAppPackaging)

// removes all jar mappings in universal and appends the fat jar
mappings in Universal := {
  // universalMappings: Seq[(File,String)]
  val universalMappings = (mappings in Universal).value
  val fatJar = (assembly in Compile).value
  // removing means filtering
  val filtered = universalMappings filter {
    case (_, n) =>  ! n.endsWith(".jar")
  }
  // add the fat jar
  filtered :+ (fatJar -> ("lib/" + fatJar.getName))
}

// the bash scripts classpath only needs the fat jar
scriptClasspath := Seq( (assemblyJarName in assembly).value )

