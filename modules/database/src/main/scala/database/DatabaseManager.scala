package database

import slick.jdbc.PostgresProfile.api.*
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext.Implicits.global
import domain.*
import com.typesafe.config.ConfigFactory

case class OrderDBRecord(
    id: Option[String],
    items: String,
    total: Double,
    status: String
):
  def toOrder: Order =
    import upickle.default.*
    Order(id.get, read[List[CartItem]](items), total, status)
end OrderDBRecord

object OrderDBRecord:
  def fromOrder(o: Order) =
    import upickle.default.*
    OrderDBRecord(Some(o.id), write(o.items), o.total, o.status)

case class ProductDBRecord(
    id: Option[String],
    name: String,
    price: Double,
    stock: Int
):
  def toProduct() =
    Product(id.get, name, price, stock)
end ProductDBRecord

object ProductDBRecord:
  def fromProduct(p: Product) =
    ProductDBRecord(Some(p.id), p.name, p.price, p.stock)

end ProductDBRecord
class ProductTable(tag: Tag)
    extends Table[ProductDBRecord](tag, "products"):
  def id    = column[String]("id", O.PrimaryKey)
  def name  = column[String]("name")
  def price = column[Double]("price")
  def stock = column[Int]("stock")
  def *     = (id.?, name, price, stock).mapTo[ProductDBRecord]
end ProductTable

class OrderTable(tag: Tag)
    extends Table[OrderDBRecord](tag, "orders"):
  def id        = column[String]("id", O.PrimaryKey)
  def itemsJson = column[String]("items_json")
  def total     = column[Double]("total")
  def status    = column[String]("status")
  def *         = (id.?, itemsJson, total, status).mapTo[OrderDBRecord]
end OrderTable

object DatabaseManager:
  private val config  = ConfigFactory.load()
  private lazy val db = Database.forConfig("database", config)

  private val products = TableQuery[ProductTable]
  private val orders   = TableQuery[OrderTable]

  def initialize(): Unit =
    readyCheck()
    println("Database successfully initialized!")
  end initialize

  def readyCheck(): Unit =
    Await.result(db.run(DBIO.seq(sqlu"SELECT 1")), 10.seconds)
    println("Database ready")

  def getProduct(id: String): Future[Option[Product]] =
    db.run(products.filter(_.id === id).result.headOption.map(o =>
      o.map(p => p.toProduct())
    ))

  def getAllProducts(): Future[Iterable[Product]] =
    db.run(products.result.map(s => s.map(p => p.toProduct())))

  def updateStock(productId: String, quantity: Int): Future[Boolean] =
    val query =
      for
        productOpt <- products.filter(_.id === productId).result.headOption
        result <- products.filter(
          _.id === productId
        ).map(_.stock).update(productOpt.get.stock - quantity)
      yield result > 0

    db.run(query)
  end updateStock

  def saveOrder(order: Order): Future[Boolean] =
    import upickle.default.*
    val action = orders += OrderDBRecord.fromOrder(order)
    db.run(action).map(_ => true).recover:
      case _ => false
  end saveOrder

  def getOrder(id: String): Future[Option[Order]] =
    import upickle.default.*
    db.run(orders.filter(_.id === id).result.headOption).map:
      case Some(value) => Some(value.toOrder)
      case None        => None
  end getOrder

  def getAllOrders(): Future[List[Order]] =
    db.run(orders.result).map: rows =>
      rows.map(o =>
        o.toOrder
      ).toList
  end getAllOrders

  def close(): Unit =
    db.close()
end DatabaseManager
