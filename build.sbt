import CiCommands.{ ciBuild, devBuild }

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "2.12.0",
  "org.typelevel" %% "cats-effect" % "2.1.4",
  "net.snowflake" % "snowflake-jdbc" % "3.18.0",
  "com.github.pureconfig" %% "pureconfig" % "0.17.6",
  "org.scalamock" %% "scalamock" % "5.1.0" % Test,
  "org.scalatest" %% "scalatest" % "3.2.10" % Test,
  "org.scalacheck" %% "scalacheck" % "1.15.4" % Test
)

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Ywarn-unused:imports",
  "-Ywarn-dead-code",
  "-Xlint:adapted-args",
  "-Xsource:2.13",
  "-Xfatal-warnings"
)

commands ++= Seq(ciBuild, devBuild)

scalafmtOnCompile := true
scalafmtConfig := file(".scalafmt.conf")
coverageFailOnMinimum := true
coverageMinimumStmtTotal := 100
