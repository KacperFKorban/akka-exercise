name := "akka-exercise"

version := "0.1"

scalaVersion := "2.13.1"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.6.5"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.6.5"
libraryDependencies += "com.typesafe.akka" %% "akka-http-core" % "10.1.12"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.1.12"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.12"
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.6.5"
libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.6.5"
libraryDependencies += "com.typesafe.akka" %% "akka-persistence" % "2.6.5"

libraryDependencies += "org.jsoup" % "jsoup" % "1.13.1"

libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.31.1"
libraryDependencies += "org.tpolecat" %% "doobie-core" % "0.9.0"
