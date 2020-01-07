ThisBuild / scalaVersion     := "2.13.1"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.chatroulette"
ThisBuild / organizationName := "example"

// Useful so the driver (if using) can be started and stopped. 
fork := true

lazy val root = (project in file("."))
  .settings(
    name := "pixel-crack",
    scalacOptions ++= Seq(
      "-encoding", "utf8",
      "-deprecation",
      "-unchecked",
      "-language:implicitConversions",
      "-language:higherKinds",
      "-language:existentials",
      "-language:postfixOps",
      "-Ywarn-value-discard"
    ),
    libraryDependencies ++= scalatest ++ scalatestplus ++ scalacheck ++ mockito ++ scribe ++ cats ++ monocle
  )

lazy val scalatest: Seq[ModuleID] = Seq(
  "org.scalatest" %% "scalatest" % "3.1.0" % Test
)

lazy val scalatestplus: Seq[ModuleID] = Seq(
  "org.scalatestplus" %% "scalatestplus-scalacheck" % "3.1.0.0-RC2" % Test
)

lazy val scalacheck: Seq[ModuleID] = Seq(
  "org.scalacheck" %% "scalacheck" % "1.14.3" % Test
)

lazy val mockito: Seq[ModuleID] = Seq(
  "org.mockito" %% "mockito-scala-scalatest" % "1.10.2" % Test
)

lazy val scribe: Seq[ModuleID] = Seq(
  "com.outr" %% "scribe" % "2.7.10"
)

lazy val cats: Seq[ModuleID] = {
  val group = "org.typelevel"
  val version = "2.0.0"

  Seq(
    "cats-core", "cats-effect"
  ).map(group %%  _ % version) ++ Seq(
    "cats-effect-laws"
  ).map(group %%  _ % version % Test)
}

lazy val monocle: Seq[ModuleID] = {
  val group = "com.github.julien-truffaut"
  val version = "2.0.0"

  Seq(
    "monocle-core", "monocle-macro"
  ).map(group %%  _ % version)
}