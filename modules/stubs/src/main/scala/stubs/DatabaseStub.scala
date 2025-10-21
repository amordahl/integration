package stubs

import domain.*
import upickle.default.*
import cask.main.MainRoutes

object DatabaseStub extends MainRoutes:

  // Keep track of stock in memory for the stub
  private var productStock = scala.collection.mutable.Map(
    "P1" -> 10,
    "P2" -> 50,
    "P3" -> 30
  )

  @cask.get("/health")
  def health() =
    ujson.Obj("status" -> "healthy", "service" -> "database-stub")

  @cask.get("/products/:id")
  def getProduct(id: String) =
    println(s"[STUB] Database get product called for: $id")
    // Return fake product data with tracked stock
    val stock   = productStock.getOrElse(id, 100)
    val product = Product(id, s"Stubbed Product $id", 99.99, stock)
    cask.Response(
      write(product),
      headers = Seq("Content-Type" -> "application/json")
    )
  end getProduct

  @cask.get("/products")
  def getAllProducts() =
    println("[STUB] Database get all products called")
    val products = List(
      Product("P1", "Stubbed Laptop", 999.99, productStock("P1")),
      Product("P2", "Stubbed Mouse", 29.99, productStock("P2")),
      Product("P3", "Stubbed Keyboard", 79.99, productStock("P3"))
    )
    cask.Response(
      write(products),
      headers = Seq("Content-Type" -> "application/json")
    )
  end getAllProducts

  @cask.post("/products/:id/update-stock")
  def updateStock(id: String, request: cask.Request) =
    println(s"[STUB] Database update stock called for: $id")
    val body     = ujson.read(request.text())
    val quantity = body("quantity").num.toInt

    val currentStock = productStock.getOrElse(id, 0)

    if currentStock >= quantity then
      productStock(id) = currentStock - quantity
      cask.Response(
        ujson.Obj("success" -> true, "newStock" -> productStock(id)).render(),
        headers = Seq("Content-Type" -> "application/json")
      )
    else
      cask.Response(
        ujson.Obj("success" -> false, "error" -> "Insufficient stock").render(),
        statusCode = 400,
        headers = Seq("Content-Type" -> "application/json")
      )
    end if
  end updateStock

  @cask.post("/orders")
  def saveOrder(request: cask.Request) =
    println("[STUB] Database save order called")
    val orderJson = request.text()
    val order     = read[Order](orderJson)
    cask.Response(
      write(order),
      headers = Seq("Content-Type" -> "application/json")
    )
  end saveOrder

  @cask.get("/orders/:id")
  def getOrder(id: String) =
    println(s"[STUB] Database get order called for: $id")
    val order = Order(id, List(), 0.0, "STUBBED")
    cask.Response(
      write(order),
      headers = Seq("Content-Type" -> "application/json")
    )
  end getOrder

  @cask.get("/orders")
  def getAllOrders() =
    println("[STUB] Database get all orders called")
    cask.Response(
      write(List[Order]()),
      headers = Seq("Content-Type" -> "application/json")
    )
  end getAllOrders

  override def port: Int    = 8080
  override def host: String = "0.0.0.0"

  initialize()
end DatabaseStub
