import sbt._

object Dependencies {
  val all = Seq(
    "com.typesafe.slick"  %% "slick"              % "3.3.3",
    "ch.qos.logback"       % "logback-core"       % "1.2.10",
    "com.typesafe.slick"  %% "slick-hikaricp"     % "3.3.3",
    "com.github.tminglei" %% "slick-pg"           % "0.20.2",
    "com.github.tminglei" %% "slick-pg_play-json" % "0.20.2",
    "org.scalatest"       %% "scalatest"          % "3.2.9"  % Test,
    "ch.qos.logback"       % "logback-classic"    % "1.2.10" % Test
  )
}
