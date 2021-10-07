name := "BooksAPI"

version := "0.1"

scalaVersion := "2.13.6"

libraryDependencies += "com.twitter" %% "finagle-http" % "21.8.0"
libraryDependencies += "org.jboss.netty" % "netty" % "3.2.10.Final"
libraryDependencies += "org.wvlet.airframe" %% "airframe-http-finagle" % "21.9.0"
libraryDependencies += "com.h2database" % "h2" % "1.4.200"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.9" % Test

libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "3.3.3",
  "org.slf4j" % "slf4j-nop" % "1.7.32",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.3.3"
)

libraryDependencies += "org.bouncycastle" % "bcprov-jdk15on" % "1.69"
libraryDependencies += "commons-codec" % "commons-codec" % "20041127.091804"

libraryDependencies += "org.wvlet.airframe" %% "airspec" % "21.9.0" % "test"
testFrameworks += new TestFramework("wvlet.airspec.Framework")

libraryDependencies += "org.scalamock" %% "scalamock" % "5.1.0" % Test