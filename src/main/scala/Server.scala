import Server.PriceQuery
import PriceGenerator.InternalPriceQuery
import akka.actor.{Actor, Props}

class Server extends Actor {
  override def receive: Receive = {
    case PriceQuery(name) => context.actorOf(Props[ServerHandler]) ! InternalPriceQuery(name, sender)
  }
}

object Server {
  case class PriceQuery(name: String)
  case class PriceResponse(name: String, price: Long)
  case class NoPrices(name: String)
}