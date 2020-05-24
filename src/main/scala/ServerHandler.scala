import PriceGenerator.{InternalPriceQuery, InternalPriceResponse}
import QuantityHandler.QueryQuantity
import Server.{NoPrices, PriceQuery, PriceResponse}
import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import akka.util.Timeout
import akka.pattern.ask

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Success

class ServerHandler(val quantityHandler: ActorRef) extends Actor with ActorLogging {

  implicit val timeout: Timeout = 300.milliseconds
  implicit val ec: ExecutionContext = context.dispatcher

  override def receive: Receive = {
    case query: InternalPriceQuery => {
      log.debug("Received query")
      val q1 = (context.actorOf(Props[PriceGenerator]) ? query).mapTo[InternalPriceResponse]
      val q2 = (context.actorOf(Props[PriceGenerator]) ? query).mapTo[InternalPriceResponse]
      val q = (quantityHandler ? PriceQuery(query.name)).mapTo[QueryQuantity].map(_.quantity)
      val f = q1.zipWith(q2)((r1, r2) => Seq(r1.price, r2.price).min).fallbackTo(q1.map(_.price)).fallbackTo(q2.map(_.price))
      val res = f.zip(q).fallbackTo(q)
      res.onComplete {
        case Success((res: Long, quantity: Long)) => query.sender ! PriceResponse(query.name, res, quantity)
        case Success(res: Long) => query.sender ! NoPrices(query.name, res)
        case _ => query.sender ! NoPrices(query.name, 0)
      }
      self ! PoisonPill
    }
  }
}
object ServerHandler {
  def props(quantityHandler: ActorRef): Props = Props(new ServerHandler(quantityHandler))
}