lazy val commonSettings = Seq(
    organization := "io.github.pityka",
    scalaVersion := "2.11.11",
    crossScalaVersions := Seq("2.12.4","2.11.11"),
    version := "1.1.0",
    scalacOptions ++= Seq("-deprecation")
  ) //++ reformatOnCompileSettings

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .settings(
    name := "akka-http-unboundedqueue",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.0.0" % "test",
      "com.typesafe.akka" %% "akka-actor" % "2.5.11",
      "com.typesafe.akka" %% "akka-stream" % "2.5.11",
      "com.typesafe.akka" %% "akka-http-core" % "10.1.0"),
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    publishTo := sonatypePublishTo.value,
    pomExtra in Global := {
      <url>https://pityka.github.io/nspl/</url>
      <scm>
        <connection>scm:git:github.com/pityka/bottleneck</connection>
        <developerConnection>scm:git:git@github.com:pityka/bottleneck</developerConnection>
        <url>github.com/pityka/bottleneck</url>
      </scm>
      <developers>
        <developer>
          <id>pityka</id>
          <name>Istvan Bartha</name>
          <url>https://pityka.github.io/bottleneck/</url>
        </developer>
      </developers>
    }
  )
