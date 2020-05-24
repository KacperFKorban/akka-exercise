import DBHandler.{UpdateMem, UpdateResult}
import QuantityHandler.QueryQuantity
import Server.PriceQuery
import akka.actor.{Actor, ActorRef, Props, Stash}

class QuantityHandler extends Actor with Stash {

  val dbHandler: ActorRef = context.actorOf(Props[DBHandler])

  dbHandler ! UpdateMem

  override def receive: Receive = {
    case UpdateResult(state) => {
      unstashAll()
      context.become(receiveWithState(state))
    }
    case _ => stash()
  }

  def receiveWithState(state: Map[String, Long]): Receive = {
    case pq@PriceQuery(name) => {
      val res: Long = state.getOrElse(name, 0)
      sender ! QueryQuantity(name, res)
      dbHandler ! pq
      context.become(receiveWithState(state.updated(name, res + 1)))
    }
  }
}

object QuantityHandler {
  case class QueryQuantity(name: String, quantity: Long)
}