resolvers ++= Seq(
  Resolver.sonatypeRepo("releases")
)

addSbtPlugin("ch.epfl.scala" % "sbt-scala3-migrate" % "0.5.0")

/*
The error message "SemanticDB not found" means the
[SemanticDB](https://github.com/scalameta/scalameta/blob/master/semanticdb/semanticdb3/semanticdb3.md)
compiler plugin is not enabled in the build. Let's fix that by adding the
following settings to `build.sbt`

```
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.9.34") in plugins.sbt
```

```scala
// build.sbt
lazy val myproject = project.settings(
  scalaVersion := "@SCALA212@", // or @SCALA211@
  addCompilerPlugin(scalafixSemanticdb),
```
*/
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.9.34")
