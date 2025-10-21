package stubs

import cask.main.Main
import domain.*
import upickle.default.*
import cask.main.MainRoutes
import cask.model.Request

object DatabaseStub extends MainRoutes:

  @cask.get("/health")
  def health() =
    ujson.Obj("status" -> "healthy", "service" -> "database-stub")

  @cask.get("/products/:id")
  def getProduct(id: String) =
    println(s"[STUB] Database get product called for: $id")
    // Return fake product data
    val product = Product(id, s"Stubbed Product $id", 99.99, 100)
    cask.Response(
      write(product),
      headers = Seq("Content-Type" -> "application/json")
    )
  end getProduct

  @cask.get("/products")
  def getAllProducts() =
    println("[STUB] Database get all products called")
    val products = List(
      Product("P1", "Stubbed Laptop", 999.99, 10),
      Product("P2", "Stubbed Mouse", 29.99, 50),
      Product("P3", "Stubbed Keyboard", 79.99, 30)
    )
    cask.Response(
      write(products),
      headers = Seq("Content-Type" -> "application/json")
    )
  end getAllProducts

  @cask.post("/products/:id/update-stock")
  def updateStock(id: String) =
    println(s"[STUB] Database update stock called for: $id")
    cask.Response(
      ujson.Obj("success" -> true, "newStock" -> 99).render(),
      headers = Seq("Content-Type" -> "application/json")
    )
  end updateStock

  @cask.post("/orders")
  def saveOrder(r: Request) =
    println("[STUB] Database save order called")
    val orderJson = r.text()
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
