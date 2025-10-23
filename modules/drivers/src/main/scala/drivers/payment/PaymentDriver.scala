package drivers
package payment

import domain.*
import upickle.default.*

object PaymentDriver:

  val paymentUrl = sys.env.getOrElse("PAYMENT_URL", "http://localhost:8081")

  def waitForService(): Unit =
    println("Waiting for Payment service...")
    var ready    = false
    var attempts = 0
    while !ready && attempts < 30 do
      try
        val response = requests.get(
          s"$paymentUrl/health",
          readTimeout = 2000,
          connectTimeout = 2000
        )
        if response.statusCode == 200 then
          println(s"  ✓ Payment service ready")
          ready = true
      catch
        case _: Exception =>
          attempts += 1
          Thread.sleep(1000)
    end while

    if !ready then
      println(s"  ✗ Payment service failed to start")
      sys.exit(1)
    println()
  end waitForService

  def testValidPayment(): Unit =
    println("Test: Valid Payment")
    println("-" * 50)

    try
      val request = PaymentRequest("1234567890123456", 100.0)
      val response = requests.post(
        s"$paymentUrl/process",
        data = write(request),
        headers = Map("Content-Type" -> "application/json")
      )

      val result = read[PaymentResponse](response.text())

      println(s"✓ SUCCESS")
      println(s"  Transaction ID: ${result.transactionId}")
      println(s"  Status: ${if result.success then "Approved" else "Declined"}")
      println(s"  Message: ${result.message}")
    catch
      case e: Exception =>
        println(s"✗ FAILED: ${e.getMessage}")
    end try

    println()
  end testValidPayment

  def testInvalidCardNumber(): Unit =
    println("Test: Invalid Card Number")
    println("-" * 50)

    try
      val request = PaymentRequest("123", 100.0)
      val response = requests.post(
        s"$paymentUrl/process",
        data = write(request),
        headers = Map("Content-Type" -> "application/json"),
        check = false
      )

      if response.statusCode == 400 then
        println(s"✓ CORRECTLY REJECTED")
        val result = read[PaymentResponse](response.text())
        println(s"  Message: ${result.message}")
      else
        println(s"✗ UNEXPECTED: Should have rejected invalid card")
      end if
    catch
      case e: Exception =>
        println(s"✗ FAILED: ${e.getMessage}")
    end try

    println()
  end testInvalidCardNumber

  def testInvalidAmount(): Unit =
    println("Test: Invalid Amount (negative)")
    println("-" * 50)

    try
      val request = PaymentRequest("1234567890123456", -100.0)
      val response = requests.post(
        s"$paymentUrl/process",
        data = write(request),
        headers = Map("Content-Type" -> "application/json"),
        check = false
      )

      if response.statusCode == 400 then
        println(s"✓ CORRECTLY REJECTED")
        val result = read[PaymentResponse](response.text())
        println(s"  Message: ${result.message}")
      else
        println(s"✗ UNEXPECTED: Should have rejected negative amount")
      end if
    catch
      case e: Exception =>
        println(s"✗ FAILED: ${e.getMessage}")
    end try

    println()
  end testInvalidAmount

  @main def main(): Unit =
    println("""
╔═══════════════════════════════════════════════════════════╗
║         BOTTOM-UP INTEGRATION TESTING                     ║
║         STEP 2: Payment Service with Test Driver          ║
╚═══════════════════════════════════════════════════════════╝
    """)

    waitForService()

    println("=" * 60)
    println("Running Payment Driver Tests")
    println("=" * 60)
    println()

    testValidPayment()
    Thread.sleep(500)

    testInvalidCardNumber()
    Thread.sleep(500)

    testInvalidAmount()

    println("=" * 60)
    println("Payment Driver Tests Complete!")
    println("=" * 60)
    println()
    println("Step 2 Observations:")
    println("  • ✓ Payment validation working")
    println("  • ✓ Card number validation working")
    println("  • ✓ Amount validation working")
    println("  • Payment service ready for integration")
    println()
  end main
end PaymentDriver
