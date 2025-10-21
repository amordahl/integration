ThisBuild / version      := "0.1.0"
ThisBuild / scalaVersion := "3.7.3"

// Common settings for all modules
lazy val commonSettings = Seq(
  libraryDependencies ++= Seq(
    "com.lihaoyi" %% "upickle" % "4.4.0"
  )
)

lazy val serviceSettings = commonSettings ++ Seq(
  libraryDependencies ++= Seq(
    "com.lihaoyi" %% "cask" % "0.11.3"
  )
)

lazy val clientSettings = commonSettings ++ Seq(
  libraryDependencies ++= Seq(
    "com.lihaoyi" %% "requests" % "0.9.0"
  )
)

lazy val databaseSettings = commonSettings ++ Seq(
  libraryDependencies ++= Seq(
    "com.typesafe.slick" %% "slick"           % "3.6.1",
    "com.typesafe.slick" %% "slick-hikaricp"  % "3.6.1",
    "org.postgresql"      % "postgresql"      % "42.7.8",
    "ch.qos.logback"      % "logback-classic" % "1.5.20"
  )
)

// Root project
lazy val root = (project in file("."))
  .aggregate(domain, checkout, stubs)
  .settings(
    name                                   := "integration-testing-demo",
    publish / skip                         := true,
    Compile / packageBin / publishArtifact := false,
    Compile / packageSrc / publishArtifact := false,
    Compile / packageDoc / publishArtifact := false
  )

// Domain module - shared models
lazy val domain = (project in file("modules/domain"))
  .settings(
    name := "domain",
    commonSettings
  )

// Checkout service (includes integration tests)
lazy val checkout = (project in file("modules/checkout"))
  .enablePlugins(JavaServerAppPackaging)
  .dependsOn(domain)
  .settings(
    name := "checkout-service",
    serviceSettings ++ clientSettings,
    Compile / mainClass := Some("checkout.CheckoutService")
  )

// Stubs
lazy val stubs = (project in file("modules/stubs"))
  .enablePlugins(JavaServerAppPackaging)
  .dependsOn(domain)
  .settings(
    name := "stub-services",
    serviceSettings
  )

// Integration tests as a separate subproject (modern SBT best practice)
lazy val integrationTests = (project in file("modules/integration-tests"))
  .dependsOn(domain)
  .settings(
    name := "integration-tests",
    clientSettings,
    publish / skip := true
  )
