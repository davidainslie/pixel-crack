ThisBuild / scalaVersion     := "2.13.1"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.chatroulette"
ThisBuild / organizationName := "example"

// Useful so the driver (if using) can be started and stopped. 
fork := true

lazy val root = (project in file("."))
  .settings(
    name := "pixel-crack",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.0"
  )
