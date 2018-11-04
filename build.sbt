name := "akka-crypto-balance"

version := "0.1"

scalaVersion := "2.12.7"

lazy val akkaHttpVersion = "10.1.3"
lazy val akkaVersion = "2.5.13"
lazy val web3jVersion = "3.5.0"

mainClass in assembly := Some("me.mbcu.crpyto.Application")

scalacOptions := Seq("-unchecked", "-deprecation")
scalacOptions += "-feature"

resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-xml"        % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream"          % akkaVersion,
  "com.typesafe.play" %% "play-json"            % "2.6.9",
  "com.typesafe.play" %% "play"                 % "2.6.13",
  "javax.inject" % "javax.inject" % "1",
  "com.neovisionaries" % "nv-websocket-client" % "2.4",
  "com.typesafe.play" %% "play-functional" % "2.6.9",
  "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % Test,
  "com.typesafe.akka" %% "akka-testkit"         % akkaVersion     % Test,
  "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion     % Test,
  "org.scalatest"     %% "scalatest"            % "3.0.1"         % Test,
  "org.mockito"       % "mockito-core"          % "2.19.1"         % Test
)


excludeDependencies += "commons-logging" % "commons-logging"
libraryDependencies += "com.github.inmyth" % "scala-mylogging" % "26b5b2c"
libraryDependencies += "com.typesafe.play" %% "play-json-joda" % "2.6.9"
libraryDependencies += "com.typesafe.play" %% "play-ahc-ws-standalone" % "2.0.0-M1"
libraryDependencies += "com.typesafe.play" %% "play-ws-standalone-json" % "2.0.0-M1"
libraryDependencies += "org.web3j" % "core" % web3jVersion
libraryDependencies += "org.web3j" % "utils" % web3jVersion