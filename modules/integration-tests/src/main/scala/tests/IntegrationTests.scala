package tests

import domain.*
import upickle.default.*

object IntegrationTests:

  val checkoutUrl  = sys.env.getOrElse("CHECKOUT_URL", "http://localhost:8083")
  val paymentUrl   = sys.env.getOrElse("PAYMENT_URL", "http://localhost:8081")
  val inventoryUrl = sys.env.getOrElse("INVENTORY_URL", "http://localhost:8082")
  val databaseUrl  = sys.env.getOrElse("DATABASE_URL", "http://localhost:8080")
  val testStep     = sys.env.getOrElse("TEST_STEP", "0")

  def waitForServices(): Unit =
    println("Waiting for services to be ready...")
    val services = List(
      ("Database", databaseUrl),
      ("Payment", paymentUrl),
      ("Inventory", inventoryUrl),
      ("Checkout", checkoutUrl)
    )

    services.foreach { case (name, url) =>
      var ready    = false
      var attempts = 0
      while !ready && attempts < 30 do
        try
          val response = requests.get(
            s"$url/health",
            readTimeout = 2000,
            connectTimeout = 2000
          )
          if response.statusCode == 200 then
            val health      = ujson.read(response.text())
            val serviceName = health("service").str
            println(s"  ✓ $name service ready (${serviceName})")
            ready = true
          end if
        catch
          case _: Exception =>
            attempts += 1
            Thread.sleep(1000)
      end while

      if !ready then
        println(s"  ✗ $name service failed to start")
        sys.exit(1)
    }
    println()
  end waitForServices

  def testSuccessfulCheckout(): Unit =
    println("Test: Successful Checkout")
    println("-" * 50)

    val request = CheckoutRequest(
      items = List(CartItem("P1", 1, 999.99)),
      cardNumber = "1234567890123456"
    )

    try
      val response = requests.post(
        s"$checkoutUrl/checkout",
        data = write(request),
        headers = Map("Content-Type" -> "application/json")
      )

      val result = read[CheckoutResponse](response.text())
      println(s"✓ SUCCESS")
      println(s"  Order ID: ${result.orderId}")
      println(s"  Total: ${result.total}")
      println(s"  Status: ${result.status}")
      println(s"  Message: ${result.message}")

      if result.message.contains("[STUB]") then
        println(s"  ⚠️  Note: This response includes stubbed services")
    catch
      case e: Exception =>
        println(s"✗ FAILED: ${e.getMessage}")
    end try

    println()
  end testSuccessfulCheckout

  def testInvalidPayment(): Unit =
    println("Test: Invalid Payment")
    println("-" * 50)

    val request = CheckoutRequest(
      items = List(CartItem("P2", 1, 29.99)),
      cardNumber = "123" // Invalid card number
    )

    try
      val response = requests.post(
        s"$checkoutUrl/checkout",
        data = write(request),
        headers = Map("Content-Type" -> "application/json"),
        check = false
      )

      val result = read[CheckoutResponse](response.text())

      if response.statusCode == 400 then
        println(s"✓ CORRECTLY REJECTED (Real Payment Service)")
        println(s"  Message: ${result.message}")
      else if result.message.contains("[STUB]") then
        println(s"⚠️  BUG HIDDEN BY STUB!")
        println(s"  The payment stub accepted an invalid card number")
        println(s"  Real payment service would reject this")
        println(s"  This demonstrates the limitation of top-down testing")
      else
        println(s"✗ UNEXPECTED: Invalid payment was accepted by real service")
      end if
    catch
      case e: Exception =>
        println(s"✗ ERROR: ${e.getMessage}")
    end try

    println()
  end testInvalidPayment

  def testInsufficientInventory(): Unit =
    println("Test: Insufficient Inventory")
    println("-" * 50)

    val request = CheckoutRequest(
      items = List(CartItem("P1", 100, 999.99)), // Request more than available
      cardNumber = "1234567890123456"
    )

    try
      val response = requests.post(
        s"$checkoutUrl/checkout",
        data = write(request),
        headers = Map("Content-Type" -> "application/json"),
        check = false
      )

      val result = read[CheckoutResponse](response.text())

      if response.statusCode == 400 then
        println(s"✓ CORRECTLY REJECTED (Real Inventory Service)")
        println(s"  Message: ${result.message}")
      else if result.message.contains("[STUB]") then
        println(s"⚠️  BUG HIDDEN BY STUB!")
        println(s"  The inventory stub accepted request for 100 laptops")
        println(s"  Real inventory service would reject this")
        println(s"  This demonstrates the limitation of top-down testing")
      else
        println(s"✗ UNEXPECTED: Insufficient inventory was accepted")
      end if
    catch
      case e: Exception =>
        println(s"✗ ERROR: ${e.getMessage}")
    end try

    println()
  end testInsufficientInventory

  @main def main(): Unit =
    val stepName = testStep match
      case "1" => "STEP 1: All Stubs"
      case "2" => "STEP 2: Real Payment, Other Stubs"
      case "3" => "STEP 3: Real Payment & Inventory, Database Stub"
      case "4" => "STEP 4: Complete System (All Real)"
      case _   => "TOP-DOWN INTEGRATION TESTING"

    println(s"""
╔═══════════════════════════════════════════════════════════╗
║         TOP-DOWN INTEGRATION TESTING                      ║
║         $stepName
╚═══════════════════════════════════════════════════════════╝
    """)

    waitForServices()

    println("=" * 60)
    println("Running Integration Tests")
    println("=" * 60)
    println()

    testSuccessfulCheckout()
    Thread.sleep(500)

    testInvalidPayment()
    Thread.sleep(500)

    testInsufficientInventory()

    println("=" * 60)
    println("Integration Tests Complete!")
    println("=" * 60)
    println()

    testStep match
      case "1" =>
        println("Step 1 Observations:")
        println("  • ✓ Checkout orchestration logic is working")
        println(
          "  • ⚠️  Stubs hide validation bugs (invalid payment accepted!)"
        )
        println("  • ⚠️  Stubs hide inventory bugs (over-ordering accepted!)")
        println("  • All dependencies are stubbed")
        println("  • Ready to integrate real Payment service")
      case "2" =>
        println("Step 2 Observations:")
        println("  • ✓ Real payment validation is working")
        println("  • ✓ Invalid card numbers are now rejected")
        println("  • ✓ Checkout ↔ Payment integration verified")
        println("  • ⚠️  Inventory stub still hides bugs")
        println("  • Database still stubbed")
        println("  • Ready to integrate real Inventory service")
      case "3" =>
        println("Step 3 Observations:")
        println("  • ✓ Real inventory management is working")
        println("  • ✓ Stock validation now works correctly")
        println("  • ✓ Checkout ↔ Inventory integration verified")
        println("  • Database still stubbed (no real persistence)")
        println("  • Ready to integrate real Database")
      case "4" =>
        println("Step 4 Observations:")
        println("  • ✓ Full system integration complete")
        println("  • ✓ All services are real (no stubs)")
        println("  • ✓ All validation working correctly")
        println("  • ✓ Data persists in PostgreSQL")
        println("  • Ready for production!")
      case _ =>
        println("Top-Down Integration Complete!")
    end match
    println()
  end main
end IntegrationTests
