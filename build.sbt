lazy val commonSettings = Seq(
    organization := "io.github.pityka",
    scalaVersion := "2.12.4",
    crossScalaVersions := Seq("2.12.4"),
    version := "1.2.0",
    scalacOptions ++= Seq("-deprecation")
  )

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .settings(
    name := "akka-http-unboundedqueue",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.0.0" % "test",
      "com.typesafe.akka" %% "akka-actor" % "2.5.12",
      "com.typesafe.akka" %% "akka-stream" % "2.5.12",
      "com.typesafe.akka" %% "akka-http-core" % "10.1.1"),
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    publishTo := sonatypePublishTo.value,
    pomExtra in Global := {
      <url>https://pityka.github.io/akka-http-unboundedqueue</url>
      <scm>
        <connection>scm:git:github.com/pityka/akka-http-unboundedqueue</connection>
        <developerConnection>scm:git:git@github.com:pityka/akka-http-unboundedqueue</developerConnection>
        <url>github.com/pityka/akka-http-unboundedqueue</url>
      </scm>
      <developers>
        <developer>
          <id>pityka</id>
          <name>Istvan Bartha</name>
          <url>https://pityka.github.io/akka-http-unboundedqueue/</url>
        </developer>
      </developers>
    }
  )
