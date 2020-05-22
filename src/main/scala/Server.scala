import Server.PriceQuery
import PriceGenerator.InternalPriceQuery
import akka.actor.{Actor, Props}
import cats.effect.IO
import doobie.util.transactor.Transactor
import spray.json.DefaultJsonProtocol._
import spray.json._
import doobie.implicits._

class Server extends Actor {

  implicit val cs = IO.contextShift(context.dispatcher)

  val xa = Transactor.fromDriverManager[IO](
    "org.sqlite.JDBC", "jdbc:sqlite:queries.db", "", ""
  )

  sql"""
  CREATE TABLE IF NOT EXISTS queries (
    name TEXT NOT NULL UNIQUE,
    quantity  INTEGER
  )
  """.update.run.transact(xa).unsafeRunSync()

  override def receive: Receive = {
    case PriceQuery(name) => context.actorOf(ServerHandler.props(xa)) ! InternalPriceQuery(name, sender)
  }
}

object Server {
  sealed trait ServerResponse
  case class PriceQuery(name: String)
  case class PriceResponse(name: String, price: Long, quantity: Long) extends ServerResponse
  case class NoPrices(name: String, quantity: Long) extends ServerResponse

  implicit val priceResponseFormat = jsonFormat3(PriceResponse)
  implicit val noPricesFormat = jsonFormat2(NoPrices)
  implicit val serverResponseWriter = new RootJsonWriter[ServerResponse] {
    def write(obj: ServerResponse): JsValue = {
      obj match {
        case r: PriceResponse => priceResponseFormat.write(r)
        case n: NoPrices => noPricesFormat.write(n)
      }
    }
  }
}