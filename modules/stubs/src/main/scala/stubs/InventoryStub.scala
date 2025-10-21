package stubs

import cask.main.Main
import domain.*
import upickle.default.*
import cask.main.MainRoutes

object InventoryStub extends MainRoutes:

  @cask.get("/health")
  def health() =
    ujson.Obj("status" -> "healthy", "service" -> "inventory-stub")

  @cask.post("/check")
  def checkAvailability(r: cask.Request) =
    println("[STUB] Inventory check called - returning fake availability")
    val requestJson = r.text()
    val request     = read[InventoryRequest](requestJson)

    // Always return available for testing
    val response = InventoryResponse(
      available = true,
      message = "[STUB] Stock available (stubbed)"
    )
    cask.Response(
      write(response),
      headers = Seq("Content-Type" -> "application/json")
    )
  end checkAvailability

  @cask.post("/reserve")
  def reserveItems() =
    println("[STUB] Inventory reservation called - returning fake success")
    cask.Response(
      ujson.Obj(
        "success" -> true,
        "message" -> "[STUB] Items reserved (stubbed)"
      ).render(),
      headers = Seq("Content-Type" -> "application/json")
    )
  end reserveItems

  override def port: Int    = 8082
  override def host: String = "0.0.0.0"

  initialize()
end InventoryStub
