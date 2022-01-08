ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"

inThisBuild(
  List(
    scalaVersion               := "2.13.7",
    scalafixScalaBinaryVersion := "2.13",
    semanticdbEnabled          := true,
    semanticdbVersion          := scalafixSemanticdb.revision
  )
)

ThisBuild / scalaVersion := "2.13.7"

// Fork when we run tests because our tests fork child processes themselves
Test / fork := true

lazy val root = (project in file("."))
  .settings(
    organization := "highlandcows",
    name         := "pgjobserver-lib",
    version      := Helpers.sysPropOrDefault("version", "0.1.0-SNAPSHOT"),
    scalacOptions ++= Seq("-Wunused", "-deprecation"),
    libraryDependencies ++= Dependencies.all
  )
