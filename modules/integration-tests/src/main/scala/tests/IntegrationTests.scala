package tests

import domain.*
import upickle.default.*

object IntegrationTests:

  val checkoutUrl  = sys.env.getOrElse("CHECKOUT_URL", "http://localhost:8083")
  val paymentUrl   = sys.env.getOrElse("PAYMENT_URL", "http://localhost:8081")
  val inventoryUrl = sys.env.getOrElse("INVENTORY_URL", "http://localhost:8082")
  val databaseUrl  = sys.env.getOrElse("DATABASE_URL", "http://localhost:8080")

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
            println(s"  ✓ $name service ready")
            ready = true
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
    println("Test 1: Successful Checkout")
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
      println(s"  Total: $${result.total}")
      println(s"  Status: ${result.status}")
      println(s"  Message: ${result.message}")
    catch
      case e: Exception =>
        println(s"✗ FAILED: ${e.getMessage}")
        println("\nBig Bang Challenge:")
        println(
          "  When this fails, we don't know which service caused the issue!"
        )
        println("  - Is it checkout orchestration?")
        println("  - Is it payment processing?")
        println("  - Is it inventory management?")
        println("  - Is it database storage?")
    end try

    println()
  end testSuccessfulCheckout

  def testInsufficientInventory(): Unit =
    println("Test 2: Insufficient Inventory")
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

      if response.statusCode == 400 then
        println(s"✓ CORRECTLY REJECTED")
        val result = read[CheckoutResponse](response.text())
        println(s"  Message: ${result.message}")
      else
        println(s"✗ UNEXPECTED: Request should have been rejected")
      end if
    catch
      case e: Exception =>
        println(s"✗ ERROR: ${e.getMessage}")
    end try

    println()
  end testInsufficientInventory

  def testInvalidPayment(): Unit =
    println("Test 3: Invalid Payment")
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

      if response.statusCode == 400 then
        println(s"✓ CORRECTLY REJECTED")
        val result = read[CheckoutResponse](response.text())
        println(s"  Message: ${result.message}")
      else
        println(s"✗ UNEXPECTED: Request should have been rejected")
      end if
    catch
      case e: Exception =>
        println(s"✗ ERROR: ${e.getMessage}")
    end try

    println()
  end testInvalidPayment

  def testMultipleItems(): Unit =
    println("Test 4: Multiple Items Checkout")
    println("-" * 50)

    val request = CheckoutRequest(
      items = List(
        CartItem("P2", 2, 29.99),
        CartItem("P3", 1, 79.99)
      ),
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
      println(s"  Total: $${result.total}")
      println(s"  Items: ${request.items.length}")
    catch
      case e: Exception =>
        println(s"✗ FAILED: ${e.getMessage}")
    end try

    println()
  end testMultipleItems

  @main def main(): Unit =
    println("""
╔═══════════════════════════════════════════════════════════╗
║         BIG BANG INTEGRATION TESTING                      ║
║         All Services Integrated at Once                   ║
╚═══════════════════════════════════════════════════════════╝
    """)

    waitForServices()

    println("=" * 60)
    println("Running Integration Tests")
    println("=" * 60)
    println()

    testSuccessfulCheckout()
    Thread.sleep(500)

    testInsufficientInventory()
    Thread.sleep(500)

    testInvalidPayment()
    Thread.sleep(500)

    testMultipleItems()

    println("=" * 60)
    println("Integration Tests Complete!")
    println("=" * 60)
    println()
    println("Key Observations:")
    println("  • All services were integrated at once")
    println("  • When tests fail, debugging is difficult")
    println("  • Hard to isolate which component has the issue")
    println("  • Good for small systems or as a final validation step")
    println()
  end main
end IntegrationTests
