import Server.{PriceQuery, PriceResponse}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}

class Client(val server: ActorRef) extends Actor with ActorLogging {
  override def receive: Receive = {
    case product: String => server ! PriceQuery(product)
    case pr: PriceResponse => log.info(s"Price of ${pr.name} is ${pr.price}")
  }
}

object Client {
  def props(server: ActorRef): Props = Props(new Client(server))
}