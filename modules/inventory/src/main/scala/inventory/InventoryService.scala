package inventory

import cask.main.Main
import domain.*
import upickle.default.*
import cask.main.MainRoutes
import cask.model.Request

object InventoryService extends MainRoutes:
  val databaseUrl = sys.env.getOrElse("DATABASE_URL", "http://localhost:8080")

  @cask.get("/health")
  def health() =
    ujson.Obj("status" -> "healthy", "service" -> "inventory")

  @cask.post("/check")
  def checkAvailability(r: Request) =
    val requestJson = r.text()
    val request     = read[InventoryRequest](requestJson)

    try
      val productResponse =
        requests.get(s"$databaseUrl/products/${request.productId}")
      val product = read[Product](productResponse.text())

      if product.stock >= request.quantity then
        val response =
          InventoryResponse(available = true, message = "Stock available")
        cask.Response(
          write(response),
          headers = Seq("Content-Type" -> "application/json")
        )
      else
        val response = InventoryResponse(
          available = false,
          message =
            s"Insufficient stock. Available: ${product.stock}, Requested: ${request.quantity}"
        )
        cask.Response(
          write(response),
          statusCode = 400,
          headers = Seq("Content-Type" -> "application/json")
        )
      end if
    catch
      case e: Exception =>
        val response = InventoryResponse(
          available = false,
          message = s"Error: ${e.getMessage}"
        )
        cask.Response(
          write(response),
          statusCode = 500,
          headers = Seq("Content-Type" -> "application/json")
        )
    end try
  end checkAvailability

  @cask.post("/reserve")
  def reserveItems(r: Request) =
    val itemsJson = r.text()
    val items     = read[List[CartItem]](itemsJson)

    try
      // Check and reserve all items
      items.foreach { item =>
        val updateData = ujson.Obj("quantity" -> item.quantity)
        val response = requests.post(
          s"$databaseUrl/products/${item.productId}/update-stock",
          data = updateData.render(),
          headers = Map("Content-Type" -> "application/json")
        )
        if response.statusCode != 200 then
          throw new Exception(
            s"Failed to reserve ${item.productId}: ${response.text()}"
          )
      }

      cask.Response(
        ujson.Obj(
          "success" -> true,
          "message" -> "Items reserved successfully"
        ).render(),
        headers = Seq("Content-Type" -> "application/json")
      )
    catch
      case e: Exception =>
        cask.Response(
          ujson.Obj("success" -> false, "error" -> e.getMessage).render(),
          statusCode = 400,
          headers = Seq("Content-Type" -> "application/json")
        )
    end try
  end reserveItems

  override def port: Int    = 8082
  override def host: String = "0.0.0.0"

  initialize()
end InventoryService
