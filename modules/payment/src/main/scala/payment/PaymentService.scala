package payment

import cask.main.Main
import domain.*
import upickle.default.*
import cask.MainRoutes

object PaymentService extends MainRoutes:

  @cask.get("/health")
  def health() =
    ujson.Obj("status" -> "healthy", "service" -> "payment")

  @cask.post("/process")
  def processPayment(r: cask.Request) =
    val requestJson = r.text()
    val request     = read[PaymentRequest](requestJson)

    // Validate payment
    if request.cardNumber.length == 16 && request.amount > 0 then
      val response = PaymentResponse(
        transactionId = s"TXN-${System.currentTimeMillis()}",
        success = true,
        message = "Payment processed successfully"
      )
      cask.Response(
        write(response),
        headers = Seq("Content-Type" -> "application/json")
      )
    else
      val response = PaymentResponse(
        transactionId = "",
        success = false,
        message = "Invalid payment information"
      )
      cask.Response(
        write(response),
        statusCode = 400,
        headers = Seq("Content-Type" -> "application/json")
      )
    end if
  end processPayment

  override def port: Int    = 8081
  override def host: String = "0.0.0.0"

  initialize()
end PaymentService
