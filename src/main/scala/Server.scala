import Server.PriceQuery
import PriceGenerator.InternalPriceQuery
import akka.actor.{Actor, ActorRef, Props}
import cats.effect.{ContextShift, IO}
import doobie.util.transactor.Transactor
import spray.json.DefaultJsonProtocol._
import spray.json._
import doobie.implicits._
import doobie.util.transactor.Transactor.Aux

class Server extends Actor {

  implicit val cs: ContextShift[IO] = IO.contextShift(context.dispatcher)

  val xa: Aux[IO, Unit] = Transactor.fromDriverManager[IO](
    "org.sqlite.JDBC", "jdbc:sqlite:queries.db", "", ""
  )

  sql"""
  CREATE TABLE IF NOT EXISTS queries (
    name TEXT NOT NULL UNIQUE,
    quantity  INTEGER
  )
  """.update.run.transact(xa).unsafeRunSync()

  val quantityHandler: ActorRef = context.actorOf(Props[QuantityHandler])

  override def receive: Receive = {
    case PriceQuery(name) => context.actorOf(ServerHandler.props(quantityHandler)) ! InternalPriceQuery(name, sender)
  }
}

object Server {
  sealed trait ServerResponse
  case class PriceQuery(name: String)
  case class PriceResponse(name: String, price: Long, quantity: Long) extends ServerResponse
  case class NoPrices(name: String, quantity: Long) extends ServerResponse

  implicit val priceResponseFormat: RootJsonFormat[PriceResponse] = jsonFormat3(PriceResponse)
  implicit val noPricesFormat: RootJsonFormat[NoPrices] = jsonFormat2(NoPrices)
  implicit val serverResponseWriter: RootJsonWriter[ServerResponse] = {
    case r: PriceResponse => priceResponseFormat.write(r)
    case n: NoPrices => noPricesFormat.write(n)
  }
}