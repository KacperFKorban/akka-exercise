import Server.PriceQuery
import PriceGenerator.InternalPriceQuery
import akka.actor.{Actor, Props}
import spray.json.DefaultJsonProtocol._
import spray.json._

class Server extends Actor {
  override def receive: Receive = {
    case PriceQuery(name) => context.actorOf(Props[ServerHandler]) ! InternalPriceQuery(name, sender)
  }
}

object Server {
  sealed trait ServerResponse
  case class PriceQuery(name: String)
  case class PriceResponse(name: String, price: Long) extends ServerResponse
  case class NoPrices(name: String) extends ServerResponse

  implicit val priceResponseFormat = jsonFormat2(PriceResponse)
  implicit val noPricesFormat = jsonFormat1(NoPrices)
  implicit val serverResponseWriter = new RootJsonWriter[ServerResponse] {
    def write(obj: ServerResponse): JsValue = {
      obj match {
        case r: PriceResponse => priceResponseFormat.write(r)
        case n: NoPrices => noPricesFormat.write(n)
      }
    }
  }
}