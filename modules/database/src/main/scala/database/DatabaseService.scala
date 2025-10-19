package database

import cask.main.Main
import domain.*
import upickle.default.*
import scala.concurrent.Await
import scala.concurrent.duration.*
import scala.util.{Failure, Success}
import cask.main.MainRoutes
import cask.model.Request

object DatabaseService extends MainRoutes:

  // Initialize database on startup
  DatabaseManager.initialize()

  @cask.get("/health")
  def health() =
    ujson.Obj("status" -> "healthy", "service" -> "database")

  @cask.get("/products/:id")
  def getProduct(id: String) =
    val result = Await.result(DatabaseManager.getProduct(id), 5.seconds)
    result match
      case Some(product) =>
        cask.Response(
          write(product),
          headers = Seq("Content-Type" -> "application/json")
        )
      case None =>
        cask.Response("Product not found", statusCode = 404)
    end match
  end getProduct

  @cask.get("/products")
  def getAllProducts() =
    val products = Await.result(DatabaseManager.getAllProducts(), 5.seconds)
    cask.Response(
      write(products),
      headers = Seq("Content-Type" -> "application/json")
    )
  end getAllProducts

  @cask.post("/products/:id/update-stock")
  def updateStock(id: String, quantity: Int) =
    val success =
      Await.result(DatabaseManager.updateStock(id, quantity), 5.seconds)

    if success then
      val product = Await.result(DatabaseManager.getProduct(id), 5.seconds)
      cask.Response(
        ujson.Obj(
          "success"  -> true,
          "newStock" -> product.map(_.stock).getOrElse(0)
        ).render(),
        headers = Seq("Content-Type" -> "application/json")
      )
    else
      cask.Response(
        ujson.Obj(
          "success" -> false,
          "error"   -> "Insufficient stock or product not found"
        ).render(),
        statusCode = 400,
        headers = Seq("Content-Type" -> "application/json")
      )
    end if
  end updateStock

  @cask.post("/orders")
  def saveOrder(r: Request) =
    val orderJson = r.text()
    val order     = read[Order](orderJson)

    val success = Await.result(DatabaseManager.saveOrder(order), 5.seconds)
    if success then
      cask.Response(
        write(order),
        headers = Seq("Content-Type" -> "application/json")
      )
    else
      cask.Response("Failed to save order", statusCode = 500)
    end if
  end saveOrder

  @cask.get("/orders/:id")
  def getOrder(id: String) =
    val result = Await.result(DatabaseManager.getOrder(id), 5.seconds)
    result match
      case Some(order) =>
        cask.Response(
          write(order),
          headers = Seq("Content-Type" -> "application/json")
        )
      case None =>
        cask.Response("Order not found", statusCode = 404)
    end match
  end getOrder

  @cask.get("/orders")
  def getAllOrders() =
    val orders = Await.result(DatabaseManager.getAllOrders(), 5.seconds)
    cask.Response(
      write(orders),
      headers = Seq("Content-Type" -> "application/json")
    )
  end getAllOrders

  override def port: Int    = 8080
  override def host: String = "0.0.0.0"

  initialize()

  // Cleanup on shutdown
  sys.addShutdownHook:
    DatabaseManager.close()
end DatabaseService
