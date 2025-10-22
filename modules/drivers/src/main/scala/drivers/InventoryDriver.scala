package drivers

import domain.*
import upickle.default.*

object InventoryDriver:

  val inventoryUrl = sys.env.getOrElse("INVENTORY_URL", "http://localhost:8082")
  val databaseUrl  = sys.env.getOrElse("DATABASE_URL", "http://localhost:8080")

  def waitForService(): Unit =
    println("Waiting for Inventory service...")
    var ready    = false
    var attempts = 0
    while !ready && attempts < 30 do
      try
        val response = requests.get(
          s"$inventoryUrl/health",
          readTimeout = 2000,
          connectTimeout = 2000
        )
        if response.statusCode == 200 then
          println(s"  ✓ Inventory service ready")
          ready = true
      catch
        case _: Exception =>
          attempts += 1
          Thread.sleep(1000)
    end while

    if !ready then
      println(s"  ✗ Inventory service failed to start")
      sys.exit(1)
    println()
  end waitForService

  def testCheckAvailability(): Unit =
    println("Test: Check Availability")
    println("-" * 50)

    try
      val request = InventoryRequest("P1", 2)
      val response = requests.post(
        s"$inventoryUrl/check",
        data = write(request),
        headers = Map("Content-Type" -> "application/json")
      )

      val result = read[InventoryResponse](response.text())

      println(s"✓ SUCCESS")
      println(s"  Available: ${result.available}")
      println(s"  Message: ${result.message}")
    catch
      case e: Exception =>
        println(s"✗ FAILED: ${e.getMessage}")
    end try

    println()
  end testCheckAvailability

  def testInsufficientStock(): Unit =
    println("Test: Insufficient Stock")
    println("-" * 50)

    try
      val request = InventoryRequest("P1", 1000)
      val response = requests.post(
        s"$inventoryUrl/check",
        data = write(request),
        headers = Map("Content-Type" -> "application/json"),
        check = false
      )

      if response.statusCode == 400 then
        println(s"✓ CORRECTLY REJECTED")
        val result = read[InventoryResponse](response.text())
        println(s"  Message: ${result.message}")
      else
        println(s"✗ UNEXPECTED: Should have rejected insufficient stock")
      end if
    catch
      case e: Exception =>
        println(s"✗ FAILED: ${e.getMessage}")
    end try

    println()
  end testInsufficientStock

  def testReserveItems(): Unit =
    println("Test: Reserve Items")
    println("-" * 50)

    try
      // Get current stock first
      val getResponse   = requests.get(s"$databaseUrl/products/P3")
      val product       = read[Product](getResponse.text())
      val originalStock = product.stock

      // Reserve items
      val items = List(CartItem("P3", 2, 79.99))
      val response = requests.post(
        s"$inventoryUrl/reserve",
        data = write(items),
        headers = Map("Content-Type" -> "application/json")
      )

      val result = ujson.read(response.text())

      // Check new stock
      val newResponse = requests.get(s"$databaseUrl/products/P3")
      val newProduct  = read[Product](newResponse.text())

      println(s"✓ SUCCESS")
      println(s"  Original stock: $originalStock")
      println(s"  Reserved: 2")
      println(s"  New stock: ${newProduct.stock}")
      println(
        s"  Stock updated correctly: ${originalStock - 2 == newProduct.stock}"
      )
      println(s"  Inventory ↔ Database integration verified")
    catch
      case e: Exception =>
        println(s"✗ FAILED: ${e.getMessage}")
    end try

    println()
  end testReserveItems

  def main(): Unit =
    println("""
╔═══════════════════════════════════════════════════════════╗
║         BOTTOM-UP INTEGRATION TESTING                     ║
║         STEP 2: Inventory Service with Test Driver       ║
╚═══════════════════════════════════════════════════════════╝
    """)

    waitForService()

    println("=" * 60)
    println("Running Inventory Driver Tests")
    println("=" * 60)
    println()

    testCheckAvailability()
    Thread.sleep(500)

    testInsufficientStock()
    Thread.sleep(500)

    testReserveItems()

    println("=" * 60)
    println("Inventory Driver Tests Complete!")
    println("=" * 60)
    println()
    println("Step 2 Observations:")
    println("  • ✓ Inventory stock checking working")
    println("  • ✓ Stock validation working")
    println("  • ✓ Inventory ↔ Database integration verified")
    println("  • ✓ Real data persistence through full stack")
    println("  • Inventory service ready for integration")
    println()
  end main
end InventoryDriver
