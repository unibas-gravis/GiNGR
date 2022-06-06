lazy val root = (project in file("."))
  .settings(
    name := "GiNGR",
    organization := "ch.unibas.cs.gravis",
    scalaVersion := "3.1.0",
    scalacOptions := Seq("-deprecation"),
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    scmInfo := Some(
      ScmInfo(url("https://github.com/unibas-gravis/GiNGR"), "git@github.com:unibas-gravis/GiNGR.git")
    ),
    crossScalaVersions := Seq("2.13.6", "3.1.0"),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    libraryDependencies ++= Seq(
      "ch.unibas.cs.gravis"        %% "scalismo-ui"   % "0.91.+",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.+"
    ),
    libraryDependencies ++= (scalaBinaryVersion.value match {
      case "3" =>
        Seq(
          "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4"
        )
      case "2.13" =>
        Seq(
          "org.scala-lang.modules" %% "scala-parallel-collections" % "0.2.0"
        )
      case _ => { println(scalaBinaryVersion.value); Seq() }
    })
  )
