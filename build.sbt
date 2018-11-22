name := "akka-crypto-balance"

version := "1.3"

scalaVersion := "2.12.7"

lazy val akkaHttpVersion = "10.1.3"
lazy val akkaVersion = "2.5.13"

mainClass in assembly := Some("me.mbcu.crypto.Application")

scalacOptions := Seq("-unchecked", "-deprecation")
scalacOptions += "-feature"

resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
  "com.typesafe.play" %% "play-json"            % "2.6.9",
  "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % Test,
  "com.typesafe.akka" %% "akka-testkit"         % akkaVersion     % Test,
  "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion     % Test,
  "org.scalatest"     %% "scalatest"            % "3.0.1"         % Test,
  "org.mockito"       % "mockito-core"          % "2.19.1"        % Test
)


//excludeDependencies += "commons-logging" % "commons-logging"
libraryDependencies += "com.github.inmyth" % "scala-mylogging" % "26b5b2c"
libraryDependencies += "com.typesafe.play" %% "play-ahc-ws-standalone" % "2.0.0-M1"
libraryDependencies += "com.typesafe.play" %% "play-ws-standalone-json" % "2.0.0-M1"
libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.5.6"
libraryDependencies += "org.apache.httpcomponents" % "httpmime" % "4.5.6"

//libraryDependencies += "commons-logging" % "commons-logging" % "1.2"


