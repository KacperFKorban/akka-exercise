import Server.{NoPrices, PriceQuery, PriceResponse}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}

class Client(val server: ActorRef) extends Actor with ActorLogging {
  override def receive: Receive = {
    case product: String => server ! PriceQuery(product)
    case pr: PriceResponse => println(s"Price of ${pr.name} is ${pr.price}, was queried ${pr.quantity} times before")
    case NoPrices(name, quantity) => println(s"No prices available for product $name, was queried $quantity times before")
  }
}

object Client {
  def props(server: ActorRef): Props = Props(new Client(server))
}