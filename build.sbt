ThisBuild / version      := "0.1.0"
ThisBuild / scalaVersion := "3.7.3"

// Common settings for all modules
lazy val commonSettings = Seq(
  libraryDependencies ++= Seq(
    "com.lihaoyi" %% "upickle" % "3.1.3"
  )
)

lazy val serviceSettings = commonSettings ++ Seq(
  libraryDependencies ++= Seq(
    "com.lihaoyi" %% "cask" % "0.9.1"
  )
)

lazy val clientSettings = commonSettings ++ Seq(
  libraryDependencies ++= Seq(
    "com.lihaoyi" %% "requests" % "0.8.0"
  )
)

// Integration test configuration
lazy val IntegrationTest = config("it") extend Test

lazy val integrationTestSettings = Defaults.itSettings ++ Seq(
  IntegrationTest / fork              := true,
  IntegrationTest / parallelExecution := false
)

// Root project
lazy val root = (project in file("."))
  .aggregate(domain, database, payment, inventory, checkout)
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
  .configs(IntegrationTest)
  .dependsOn(domain)
  .settings(
    name := "checkout-service",
    serviceSettings ++ clientSettings ++ integrationTestSettings,
    Compile / mainClass := Some("checkout.CheckoutService")
  )
