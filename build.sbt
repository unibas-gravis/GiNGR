ThisBuild / scalaVersion := "3.3.1"
ThisBuild / version := "1.0-RC1"

lazy val root = (project in file("."))
  .settings(
    name := "GiNGR",
    homepage := Some(url("https://github.com/unibas-gravis/GiNGR")),
    organization := "ch.unibas.cs.gravis",
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    scmInfo := Some(
      ScmInfo(url("https://github.com/unibas-gravis/GiNGR"), "git@github.com:unibas-gravis/GiNGR.git")
    ),
    developers := List(
      Developer("madsendennis", "madsendennis", "dennis@dentexion.com", url("https://github.com/madsendennis"))
    ),
    publishMavenStyle := true,
    publishTo := Some(
      if (isSnapshot.value)
        Opts.resolver.sonatypeSnapshots
      else
        Opts.resolver.sonatypeStaging
    ),
    resolvers ++= Seq(
      Resolver.jcenterRepo,
      Resolver.sonatypeRepo("releases")
    ),
    scalacOptions ++= {
      Seq(
        "-encoding",
        "UTF-8",
        "-feature",
        "-language:implicitConversions"
        // disabled during the migration
        // "-Xfatal-warnings"
      )
    },
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.18" % "test",
      "ch.unibas.cs.gravis" %% "scalismo" % "1.0-RC1",
      "io.spray" %% "spray-json" % "1.3.6"
    )
  )
