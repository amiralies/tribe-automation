// The simplest possible sbt build file is just one line:

scalaVersion := "2.13.8"
// That is, to create a valid sbt build, all you've got to do is define the
// version of Scala you'd like your project to use.

// ============================================================================

// Lines like the above defining `scalaVersion` are called "settings". Settings
// are key/value pairs. In the case of `scalaVersion`, the key is "scalaVersion"
// and the value is "2.13.8"

// It's possible to define many kinds of settings, such as:

name := "tribe-automation"
organization := "so.tribe"
version := "1.0"

libraryDependencies += "dev.zio" %% "zio" % "2.0.1"

val circeVersion = "0.14.2"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-generic-extras",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

val AkkaVersion = "2.6.8"
val AkkaHttpVersion = "10.2.9"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion
)
libraryDependencies += "io.scalac" %% "zio-akka-http-interop" % "0.6.0"

libraryDependencies += "de.heikoseeberger" %% "akka-http-circe" % "1.39.2"

libraryDependencies += "com.beachape" %% "enumeratum" % "1.7.0"
libraryDependencies += "com.beachape" %% "enumeratum-circe" % "1.7.0"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.13" % "test"
libraryDependencies += "org.scalatest" %% "scalatest-flatspec" % "3.2.13" % "test"
libraryDependencies += "org.scalatest" %% "scalatest-shouldmatchers" % "3.2.13" % "test"

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "2.1.0"

libraryDependencies += "com.github.ghostdogpr" %% "caliban-client" % "2.0.1"

libraryDependencies += "javax.xml.bind" % "jaxb-api" % "2.3.1"

val zioConfigVersion = "3.0.2"
libraryDependencies += "dev.zio" %% "zio-config" % zioConfigVersion
libraryDependencies += "dev.zio" %% "zio-config-magnolia" % zioConfigVersion
libraryDependencies += "dev.zio" %% "zio-config-typesafe" % zioConfigVersion

libraryDependencies ++= Seq(
  "dev.zio" %% "zio-test" % "2.0.1" % Test,
  "dev.zio" %% "zio-test-sbt" % "2.0.1" % Test,
  "dev.zio" %% "zio-test-magnolia" % "2.0.1" % Test
)
testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
enablePlugins(CalibanPlugin)

libraryDependencies += "com.wix" %% "accord-core" % "0.7.6"

/* scalacOptions ++= Seq(          // use ++= to add to existing options */
/* "-Ymacro-annotations" */
/* ) */

// Note, it's not required for you to define these three settings. These are
// mostly only necessary if you intend to publish your library's binaries on a
// place like Sonatype.

// Want to use a published library in your project?
// You can define other libraries as dependencies in your build like this:

// Here, `libraryDependencies` is a set of dependencies, and by using `+=`,
// we're adding the scala-parser-combinators dependency to the set of dependencies
// that sbt will go and fetch when it starts up.
// Now, in any Scala file, you can import classes, objects, etc., from
// scala-parser-combinators with a regular import.

// TIP: To find the "dependency" that you need to add to the
// `libraryDependencies` set, which in the above example looks like this:

// "org.scala-lang.modules" %% "scala-parser-combinators" % "2.1.1"

// You can use Scaladex, an index of all known published Scala libraries. There,
// after you find the library you want, you can just copy/paste the dependency
// information that you need into your build file. For example, on the
// scala/scala-parser-combinators Scaladex page,
// https://index.scala-lang.org/scala/scala-parser-combinators, you can copy/paste
// the sbt dependency from the sbt box on the right-hand side of the screen.

// IMPORTANT NOTE: while build files look _kind of_ like regular Scala, it's
// important to note that syntax in *.sbt files doesn't always behave like
// regular Scala. For example, notice in this build file that it's not required
// to put our settings into an enclosing object or class. Always remember that
// sbt is a bit different, semantically, than vanilla Scala.

// ============================================================================

// Most moderately interesting Scala projects don't make use of the very simple
// build file style (called "bare style") used in this build.sbt file. Most
// intermediate Scala projects make use of so-called "multi-project" builds. A
// multi-project build makes it possible to have different folders which sbt can
// be configured differently for. That is, you may wish to have different
// dependencies or different testing frameworks defined for different parts of
// your codebase. Multi-project builds make this possible.

// Here's a quick glimpse of what a multi-project build looks like for this
// build, with only one "subproject" defined, called `root`:

// lazy val root = (project in file(".")).
//   settings(
//     inThisBuild(List(
//       organization := "ch.epfl.scala",
//       scalaVersion := "2.13.8"
//     )),
//     name := "hello-world"
//   )

// To learn more about multi-project builds, head over to the official sbt
// documentation at http://www.scala-sbt.org/documentation.html
