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
  .aggregate(domain, database, payment, inventory, checkout, integrationTests, e2eTests)
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

// Database service
lazy val database = (project in file("modules/database"))
  .enablePlugins(JavaServerAppPackaging)
  .dependsOn(domain)
  .settings(
    name := "database-service",
    serviceSettings,
    databaseSettings,
    Compile / mainClass := Some("database.DatabaseService")
  )

// Payment service
lazy val payment = (project in file("modules/payment"))
  .enablePlugins(JavaServerAppPackaging)
  .dependsOn(domain)
  .settings(
    name := "payment-service",
    serviceSettings,
    Compile / mainClass := Some("payment.PaymentService")
  )

// Inventory service
lazy val inventory = (project in file("modules/inventory"))
  .enablePlugins(JavaServerAppPackaging)
  .dependsOn(domain)
  .settings(
    name := "inventory-service",
    serviceSettings ++ clientSettings,
    Compile / mainClass := Some("inventory.InventoryService")
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

// Integration tests as a separate subproject (modern SBT best practice)
lazy val integrationTests = (project in file("modules/integration-tests"))
  .dependsOn(domain)
  .settings(
    name := "integration-tests",
    clientSettings,
    publish / skip := true
  )

// E2E tests - focused on user journeys and data consistency
lazy val e2eTests = (project in file("modules/e2e-tests"))
  .dependsOn(domain)
  .settings(
    name := "e2e-tests",
    clientSettings,
    publish / skip := true
  )
