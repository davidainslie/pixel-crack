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
      "-language:postfixOps"
    ),
    libraryDependencies ++= scalatest ++ scalatestplus ++ scalacheck ++ scalacheckShapeless ++ cats ++ monocle ++ monix ++ stm
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

lazy val scalacheckShapeless: Seq[ModuleID] = Seq(
  "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % "1.2.3" % Test
)

lazy val cats: Seq[ModuleID] = {
  val group = "org.typelevel"
  val version = "2.0.0"

  Seq(
    "cats-core", "cats-effect"
  ).map(group %%  _ % version)
}

lazy val monocle: Seq[ModuleID] = {
  val group = "com.github.julien-truffaut"
  val version = "2.0.0"

  Seq(
    "monocle-core", "monocle-macro"
  ).map(group %%  _ % version)
}

lazy val monix: Seq[ModuleID] = Seq(
  "io.monix" %% "monix" % "3.1.0"
)

lazy val stm: Seq[ModuleID] = Seq(
  "org.scala-stm" %% "scala-stm" % "0.9.1"
)