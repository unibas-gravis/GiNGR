import sbt.Resolver

organization := "ch.unibas.cs.gravis"

name := "GiNGR"

version := "0.1"

scalaVersion := "2.13.8"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers ++= Seq(
  Resolver.bintrayRepo("unibas-gravis", "maven"),
  Resolver.bintrayRepo("cibotech", "public"),
  Opts.resolver.sonatypeSnapshots
)

libraryDependencies ++= Seq(
  "ch.unibas.cs.gravis" %% "scalismo-ui" % "0.91.+",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.+"
)

libraryDependencies ++= (scalaBinaryVersion.value match {
  case "2.13" => Seq("org.scala-lang.modules" %% "scala-parallel-collections" % "0.2.0")
  case _      => Seq()
})

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case PathList("META-INF", s) if s.endsWith(".SF") || s.endsWith(".DSA") || s.endsWith(".RSA") => MergeStrategy.discard
  case "reference.conf" => MergeStrategy.concat
  case _ => MergeStrategy.first
}
