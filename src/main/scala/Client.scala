import Server.{NoPrices, PriceQuery, PriceResponse}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}

class Client(val server: ActorRef) extends Actor with ActorLogging {
  override def receive: Receive = {
    case product: String => server ! PriceQuery(product)
    case pr: PriceResponse => log.info(s"Price of ${pr.name} is ${pr.price}")
    case NoPrices(name) => log.info(s"No prices available for product $name")
  }
}

object Client {
  def props(server: ActorRef): Props = Props(new Client(server))
}