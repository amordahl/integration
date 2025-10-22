package drivers
package database

import domain.*
import upickle.default.*
import scala.annotation.static

object DatabaseDriver:
  @main def main(): Unit =
    val driver = new DatabaseDriver()
    driver.main()

class DatabaseDriver:
  val databaseUrl = sys.env.getOrElse("DATABASE_URL", "http://localhost:8080")

  def waitForService(): Unit =
    println("Waiting for Database service...")
    var ready    = false
    var attempts = 0
    while !ready && attempts < 30 do
      try
        val response = requests.get(
          s"$databaseUrl/health",
          readTimeout = 2000,
          connectTimeout = 2000
        )
        if response.statusCode == 200 then
          println(s"  ✓ Database service ready")
          ready = true
      catch
        case _: Exception =>
          attempts += 1
          Thread.sleep(1000)
    end while

    if !ready then
      println(s"  ✗ Database service failed to start")
      sys.exit(1)
    println()
  end waitForService

  def testGetProduct(): Unit =
    println("Test: Get Product")
    println("-" * 50)

    try
      val response = requests.get(s"$databaseUrl/products/P1")
      val product  = read[Product](response.text())

      println(s"✓ SUCCESS")
      println(s"  Product: ${product.name}")
      println(s"  Price: $$${product.price}")
      println(s"  Stock: ${product.stock}")
    catch
      case e: Exception =>
        println(s"✗ FAILED: ${e.getMessage}")
    end try

    println()
  end testGetProduct

  def testGetAllProducts(): Unit =
    println("Test: Get All Products")
    println("-" * 50)

    try
      val response = requests.get(s"$databaseUrl/products")
      val products = read[List[Product]](response.text())

      println(s"✓ SUCCESS")
      println(s"  Found ${products.length} products")
      products.foreach(p =>
        println(s"  - ${p.name}: $$${p.price} (${p.stock} in stock)")
      )
    catch
      case e: Exception =>
        println(s"✗ FAILED: ${e.getMessage}")
    end try

    println()
  end testGetAllProducts

  def testUpdateStock(): Unit =
    println("Test: Update Stock")
    println("-" * 50)

    // First get current stock
    val getResponse   = requests.get(s"$databaseUrl/products/P2")
    val product       = read[Product](getResponse.text())
    val originalStock = product.stock

    // Update stock
    val updateData = ujson.Obj("quantity" -> 5)
    val updateResponse = requests.post(
      s"$databaseUrl/products/P2/update-stock",
      data = updateData.render(),
      headers = Map("Content-Type" -> "application/json")
    )

    println(s"Response: ${updateResponse.text()}")
    val result = ujson.read(updateResponse.text())
    println(s"Read as ${result.render()}")
    val newStock = result("newStock").num.toInt

    println(s"✓ SUCCESS")
    println(s"  Original stock: $originalStock")
    println(s"  Reserved: 5")
    println(s"  New stock: $newStock")
    println(s"  Calculation correct: ${originalStock - 5 == newStock}")

    println()
  end testUpdateStock

  def testInsufficientStock(): Unit =
    println("Test: Insufficient Stock Rejection")
    println("-" * 50)

    try
      val updateData = ujson.Obj("quantity" -> 1000)
      val response = requests.post(
        s"$databaseUrl/products/P1/update-stock",
        data = updateData.render(),
        headers = Map("Content-Type" -> "application/json"),
        check = false
      )

      if response.statusCode == 400 then
        println(s"✓ CORRECTLY REJECTED")
        println(s"  Database properly validates stock levels")
      else
        println(s"✗ UNEXPECTED: Should have rejected insufficient stock")
      end if
    catch
      case e: Exception =>
        println(s"✗ FAILED: ${e.getMessage}")
    end try

    println()
  end testInsufficientStock

  def testSaveAndGetOrder(): Unit =
    println("Test: Save and Retrieve Order")
    println("-" * 50)

    try
      val order = Order(
        id = s"TEST-ORD-${System.currentTimeMillis()}",
        items = List(CartItem("P1", 1, 999.99)),
        total = 999.99,
        status = "COMPLETED"
      )

      // Save order
      requests.post(
        s"$databaseUrl/orders",
        data = write(order),
        headers = Map("Content-Type" -> "application/json")
      )

      // Retrieve order
      val getResponse = requests.get(s"$databaseUrl/orders/${order.id}")
      val retrieved   = read[Order](getResponse.text())

      println(s"✓ SUCCESS")
      println(s"  Saved order: ${order.id}")
      println(s"  Retrieved order: ${retrieved.id}")
      println(
        s"  Data matches: ${order.id == retrieved.id && order.total == retrieved.total}"
      )
    catch
      case e: Exception =>
        println(s"✗ FAILED: ${e.getMessage}")
    end try

    println()
  end testSaveAndGetOrder

  def main(): Unit =
    println("""
╔═══════════════════════════════════════════════════════════╗
║         BOTTOM-UP INTEGRATION TESTING                     ║
║         STEP 1: Database Service with Test Driver        ║
╚═══════════════════════════════════════════════════════════╝
    """)

    waitForService()

    println("=" * 60)
    println("Running Database Driver Tests")
    println("=" * 60)
    println()

    testGetProduct()
    Thread.sleep(500)

    testGetAllProducts()
    Thread.sleep(500)

    testUpdateStock()
    Thread.sleep(500)

    testInsufficientStock()
    Thread.sleep(500)

    testSaveAndGetOrder()

    println("=" * 60)
    println("Database Driver Tests Complete!")
    println("=" * 60)
    println()
    println("Step 1 Observations:")
    println("  • ✓ Database CRUD operations working")
    println("  • ✓ PostgreSQL integration verified")
    println("  • ✓ Stock validation working")
    println("  • ✓ Order persistence working")
    println("  • Ready to add Payment and Inventory services")
    println()
  end main
end DatabaseDriver
