package domain
import upickle.default.*

case class CartItem(productId: String, quantity: Int, price: Double)
    derives ReadWriter
case class PaymentRequest(cardNumber: String, amount: Double) derives ReadWriter
case class PaymentResponse(
    transactionId: String,
    success: Boolean,
    message: String = ""
) derives ReadWriter
case class InventoryRequest(productId: String, quantity: Int) derives ReadWriter
case class InventoryResponse(available: Boolean, message: String)
    derives ReadWriter
case class CheckoutRequest(items: List[CartItem], cardNumber: String)
    derives ReadWriter
case class CheckoutResponse(
    orderId: String,
    total: Double,
    status: String,
    message: String = ""
) derives ReadWriter

// DB Entities
case class Order(
    id: String,
    items: List[CartItem],
    total: Double,
    status: String
) derives ReadWriter

case class Product(id: String, name: String, price: Double, stock: Int)
    derives ReadWriter
