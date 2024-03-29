import sbt.Keys.libraryDependencies

name := "actormatrix"

version := "0.0.1"

scalaVersion := "2.12.14"

val akkaVersion = "2.6.9"
val prometheusVersion = "0.8.0"
val akkaHttpVersion = "10.2.0"
val AkkaManagementVersion = "1.0.9"

assemblyMergeStrategy in assembly := {
  case PathList("jackson-annotations-2.10.3.jar", xs @ _*)            => MergeStrategy.last
  case PathList("jackson-core-2.10.3.jar", xs @ _*)                   => MergeStrategy.last
  case PathList("jackson-databind-2.10.3.jar", xs @ _*)               => MergeStrategy.last
  case PathList("jackson-dataformat-cbor-2.10.3.jar", xs @ _*)        => MergeStrategy.last
  case PathList("jackson-datatype-jdk8-2.10.3.jar", xs @ _*)          => MergeStrategy.last
  case PathList("jackson-datatype-jsr310-2.10.3.jar", xs @ _*)        => MergeStrategy.last
  case PathList("jackson-module-parameter-names-2.10.3.jar", xs @ _*) => MergeStrategy.last
  case PathList("jackson-module-paranamer-2.10.3.jar", xs @ _*)       => MergeStrategy.last
  case PathList("META-INF", "MANIFEST.MF")                            => MergeStrategy.discard
  case PathList("META-INF", xs @ _*)                                  => MergeStrategy.discard
  case PathList("reference.conf")                                     => MergeStrategy.concat
  case _                                                              => MergeStrategy.first
}

mainClass in assembly := Some("com.vrann.App")

val `actormatrix` = project
  .in(file("."))
  .settings(
    organization := "com.lightbend.akka.samples",
    version := "0.0.1",
    scalaVersion := "2.12.11",
    scalacOptions in Compile ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint"),
    javacOptions in Compile ++= Seq("-Xlint:unchecked", "-Xlint:deprecation"),
    javaOptions in run ++= Seq("-Xms128m", "-Xmx1024m"),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
      "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.0.8" % Test,
      "io.aeron" % "aeron-driver" % "1.27.0",
      "io.aeron" % "aeron-client" % "1.27.0",
      "io.prometheus" % "simpleclient" % prometheusVersion,
      "io.prometheus" % "simpleclient_common" % prometheusVersion,
      "io.micrometer" % "micrometer-registry-prometheus" % "1.5.3",
      "io.kamon" %% "kamon-bundle" % "2.1.4",
      "io.kamon" %% "kamon-prometheus" % "2.1.4",
      "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % AkkaManagementVersion,
      "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % AkkaManagementVersion,
      "com.lightbend.akka.management" %% "akka-management-cluster-http" % AkkaManagementVersion,
      "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
      "com.typesafe.akka" %% "akka-discovery" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.1.1" % Test,
      "com.github.fommil.netlib" % "all" % "1.1.2",
      "org.apache.spark" %% "spark-mllib" % "2.4.4",
//      "com.typesafe" %% "config" % "1.4.0",
      "org.apache.spark" %% "spark-mllib-local" % "2.4.4").map(_.exclude("org.slf4j", "*")),
    libraryDependencies ++= Seq("ch.qos.logback" % "logback-classic" % "1.2.3"),
    fork in run := true,
    Global / cancelable := false, // ctrl-c
    // disable parallel tests
    parallelExecution in Test := false,
    // show full stack traces and test case durations
    testOptions in Test += Tests.Argument("-oDF"),
    logBuffered in Test := false,
    licenses := Seq(("CC0", url("http://creativecommons.org/publicdomain/zero/1.0"))))
