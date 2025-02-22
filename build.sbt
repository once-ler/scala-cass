val cassandra3Version = "3.5.0"
val cassandra2Version = "2.1.10.3"
val cassandraVersion = sys.props.getOrElse("cassandra-driver.version", cassandra3Version) match {
  case v @ (`cassandra3Version` | `cassandra2Version`) => v
  case _ => throw new IllegalArgumentException(s"cassandra version must be one of $cassandra3Version, $cassandra2Version")
}

val baseVersion = "3.2.1"

def addUnmanagedSourceDirsFrom(folder: String) = {
  def addSourceFilesTo(conf: Configuration) =
    unmanagedSourceDirectories in conf := {
      val sds = (unmanagedSourceDirectories in conf).value
      val sd = (sourceDirectory in conf).value
      sds :+ new java.io.File(sd, folder)
    }

  Seq(addSourceFilesTo(Compile), addSourceFilesTo(Test))
}

lazy val commonSettings = Seq(
  scalaVersion := "2.13.8",
  crossScalaVersions := Seq("2.13.8", "2.12.10", "2.11.12", "2.10.7"),
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-unchecked",
    "-Xfatal-warnings",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard"
  ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 13)) => Seq("-explaintypes", "-language:experimental.macros", "-Xlint:adapted-args,constant,doc-detached,nullary-unit,inaccessible,infer-any,missing-interpolator,doc-detached,private-shadow,type-parameter-shadow,poly-implicit-overload,option-implicit,delayedinit-select,package-object-classes,stars-align", "-Ywarn-unused:patvars,privates,locals", "-Ymacro-annotations", "-Ywarn-extra-implicit", "-Ycache-plugin-class-loader:last-modified", "-Ycache-macro-class-loader:last-modified")
    case Some((2, 12)) => Seq("-Yno-adapted-args", "-Xlint:adapted-args,nullary-unit,inaccessible,infer-any,missing-interpolator,doc-detached,private-shadow,type-parameter-shadow,poly-implicit-overload,option-implicit,delayedinit-select,by-name-right-associative,package-object-classes,unsound-match,stars-align", "-Ywarn-unused:privates,locals", "-Xfuture")
    case Some((2, 11)) => Seq("-Yno-adapted-args", "-Xlint:adapted-args,nullary-unit,inaccessible,infer-any,missing-interpolator,doc-detached,private-shadow,type-parameter-shadow,poly-implicit-overload,option-implicit,delayedinit-select,by-name-right-associative,package-object-classes,unsound-match,stars-align", "-Ywarn-unused", "-Ywarn-unused-import", "-Xfuture")
    case Some((2, 10)) => Seq("-Yno-adapted-args", "-Xlint", "-Xfuture")
    case _             => throw new IllegalArgumentException(s"scala version not configured: ${scalaVersion.value}")
  }),
  (scalacOptions in Test) -= "-Xfatal-warnings",
  parallelExecution in Test := false,
)

lazy val macroSettings = Seq(
  libraryDependencies ++= Seq(
    "org.scalameta" %% "scalameta" % "3.7.4" % "provided",
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
    "com.datastax.cassandra" % "cassandra-driver-core" % cassandraVersion classifier "shaded"
  ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 10)) => Seq("org.scalamacros" %% "quasiquotes" % "2.1.1" cross CrossVersion.binary)
    case _ => Seq.empty
  })
)

lazy val applicationSettings = Seq(
  name := "ScalaCass",
  organization := "com.github.thurstonsand",
  description := "a wrapper for the Java Cassandra driver that uses case classes to simplify and codify creating cached statements in a type-safe manner",
  version := s"$baseVersion-$cassandraVersion",
  libraryDependencies ++= Seq(
    "com.google.code.findbugs" % "jsr305" % "3.0.1" % "provided", // Intellij does not like "compile-internal, test-internal", use "provided" instead
    "org.joda" % "joda-convert" % "1.8.1" % "provided", // Intellij does not like "compile-internal, test-internal", use "provided" instead
    "org.slf4j" % "slf4j-api" % "1.7.25" % "provided", // Intellij does not like "compile-internal, test-internal", use "provided" instead
    "joda-time" % "joda-time" % "2.9.4",
    "com.chuusai" %% "shapeless" % "2.3.3",
    "com.google.guava" % "guava" % "19.0",
    "com.datastax.cassandra" % "cassandra-driver-core" % cassandraVersion classifier "shaded" excludeAll ExclusionRule("com.google.guava", "guava"),
    "org.scalatest" %% "scalatest" % "3.0.8" % "test",
  )  ++ (if (cassandraVersion startsWith "2.1.") Seq(
    "org.cassandraunit" % "cassandra-unit" % "2.2.2.1" % "test"
  ) else Seq(
    "com.datastax.cassandra" % "cassandra-driver-extras" % cassandraVersion excludeAll (ExclusionRule("com.datastax.cassandra", "cassandra-driver-core"), ExclusionRule("com.google.guava", "guava")),
    "org.cassandraunit" % "cassandra-unit" % "3.3.0.2" % "test"
  )) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 13)) => Seq.empty
    case _ => Seq(compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full))
  }),
  initialize := {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 10)) => sys.props("scalac.patmat.analysisBudget") = "off"
      case _             => sys.props remove "scalac.patmat.analysisBudget"
    }
  }
)

lazy val noPublishSettings = Seq(
  publish := ((): Unit),
  publishLocal := ((): Unit),
  publishArtifact := false
)

lazy val `scala-cass` = project.in(file("."))
  .settings(moduleName := "scala-cass",
            sourceGenerators in Compile += (sourceManaged in Compile).map(Boilerplate.gen).taskValue)
  .settings(commonSettings: _*)
  .settings(applicationSettings: _*)
  .settings(addUnmanagedSourceDirsFrom(if (cassandraVersion startsWith "2.1.") "scala_cass21" else "scala_cass3"))
