package stubs

import cask.main.Main
import domain.*
import upickle.default.*
import cask.main.MainRoutes

object PaymentStub extends MainRoutes:

  @cask.get("/health")
  def health() =
    ujson.Obj("status" -> "healthy", "service" -> "payment-stub")

  @cask.post("/process")
  def processPayment(r: cask.Request) =
    println("[STUB] Payment service called - returning fake success")
    val requestJson = r.text()
    val request     = read[PaymentRequest](requestJson)

    // Always return success for testing
    val response = PaymentResponse(
      transactionId = s"STUB-TXN-${System.currentTimeMillis()}",
      success = true,
      message = "[STUB] Payment processed successfully (stubbed)"
    )
    cask.Response(
      write(response),
      headers = Seq("Content-Type" -> "application/json")
    )
  end processPayment

  override def port: Int    = 8081
  override def host: String = "0.0.0.0"

  initialize()
end PaymentStub
