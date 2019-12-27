ThisBuild / scalaVersion     := "2.13.1"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.chatroulette"
ThisBuild / organizationName := "example"

// Useful so the driver (if using) can be started and stopped. 
fork := true

lazy val root = (project in file("."))
  .settings(
    name := "pixel-crack",
    libraryDependencies ++= scalatest ++ cats ++ fs2 ++ zio
  )

lazy val scalatest: Seq[ModuleID] = Seq(
  "org.scalatest" %% "scalatest" % "3.1.0" % Test
)

lazy val cats: Seq[ModuleID] = {
  val group = "org.typelevel"
  val version = "2.0.0"

  Seq(
    "cats-core", "cats-effect"
  ).map(group %%  _ % version)
}

lazy val fs2: Seq[ModuleID] = {
  val group = "co.fs2"
  val version = "2.1.0"

  Seq(
    "fs2-core", "fs2-io"
  ).map(group %%  _ % version)
}

lazy val zio: Seq[ModuleID] = {
  val group = "dev.zio"
  val version = "1.0.0-RC17"

  Seq(
    "zio", "zio-streams"
  ).map(group %%  _ % version)
}