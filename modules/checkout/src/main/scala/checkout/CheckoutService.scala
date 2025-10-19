package checkout

package checkout

import cask.main.Main
import domain.*
import upickle.default.*
import cask.main.MainRoutes

object CheckoutService extends MainRoutes:
  val paymentUrl   = sys.env.getOrElse("PAYMENT_URL", "http://localhost:8081")
  val inventoryUrl = sys.env.getOrElse("INVENTORY_URL", "http://localhost:8082")
  val databaseUrl  = sys.env.getOrElse("DATABASE_URL", "http://localhost:8080")

  @cask.get("/health")
  def health() =
    ujson.Obj("status" -> "healthy", "service" -> "checkout")

  @cask.post("/checkout")
  def checkout(r: cask.Request) =
    val requestJson = r.text()
    val request     = read[CheckoutRequest](requestJson)

    try
      // Step 1: Reserve inventory
      val reserveResponse = requests.post(
        s"$inventoryUrl/reserve",
        data = write(request.items),
        headers = Map("Content-Type" -> "application/json")
      )

      if reserveResponse.statusCode != 200 then
        throw new Exception(
          s"Inventory reservation failed: ${reserveResponse.text()}"
        )

      // Step 2: Process payment
      val total          = request.items.map(i => i.price * i.quantity).sum
      val paymentRequest = PaymentRequest(request.cardNumber, total)
      val paymentResponse = requests.post(
        s"$paymentUrl/process",
        data = write(paymentRequest),
        headers = Map("Content-Type" -> "application/json")
      )

      val payment = read[PaymentResponse](paymentResponse.text())
      if !payment.success then
        throw new Exception(s"Payment failed: ${payment.message}")

      // Step 3: Save order
      val order = Order(
        id = s"ORD-${System.currentTimeMillis()}",
        items = request.items,
        total = total,
        status = "COMPLETED"
      )

      requests.post(
        s"$databaseUrl/orders",
        data = write(order),
        headers = Map("Content-Type" -> "application/json")
      )

      val response = CheckoutResponse(
        orderId = order.id,
        total = total,
        status = "COMPLETED",
        message =
          s"Order completed successfully. Transaction: ${payment.transactionId}"
      )
      cask.Response(
        write(response),
        headers = Seq("Content-Type" -> "application/json")
      )

    catch
      case e: Exception =>
        val response = CheckoutResponse(
          orderId = "",
          total = 0.0,
          status = "FAILED",
          message = e.getMessage
        )
        cask.Response(
          write(response),
          statusCode = 400,
          headers = Seq("Content-Type" -> "application/json")
        )
    end try
  end checkout

  override def port: Int    = 8083
  override def host: String = "0.0.0.0"

  initialize()
end CheckoutService
