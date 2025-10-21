package checkout

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
  def checkout(request: cask.Request) =
    val requestJson     = request.text()
    val checkoutRequest = read[CheckoutRequest](requestJson)

    try
      // Step 1: Reserve inventory
      val reserveResponse = requests.post(
        s"$inventoryUrl/reserve",
        data = write(checkoutRequest.items),
        headers = Map("Content-Type" -> "application/json")
      )

      if reserveResponse.statusCode != 200 then
        throw new Exception(
          s"Inventory reservation failed: ${reserveResponse.text()}"
        )

      val reserveResult    = ujson.read(reserveResponse.text())
      val inventoryMessage = reserveResult("message").strOpt.getOrElse("")

      // Step 2: Process payment
      val total = checkoutRequest.items.map(i => i.price * i.quantity).sum
      val paymentRequest = PaymentRequest(checkoutRequest.cardNumber, total)
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
        items = checkoutRequest.items,
        total = total,
        status = "COMPLETED"
      )

      requests.post(
        s"$databaseUrl/orders",
        data = write(order),
        headers = Map("Content-Type" -> "application/json")
      )

      // Propagate stub messages if present
      val messages = List(
        if payment.message.contains("[STUB]") then Some(payment.message)
        else None,
        if inventoryMessage.contains("[STUB]") then Some(inventoryMessage)
        else None
      ).flatten

      val finalMessage = if messages.nonEmpty then
        s"Order completed. ${messages.mkString(" ")} Transaction: ${payment.transactionId}"
      else
        s"Order completed successfully. Transaction: ${payment.transactionId}"

      val response = CheckoutResponse(
        orderId = order.id,
        total = total,
        status = "COMPLETED",
        message = finalMessage
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
