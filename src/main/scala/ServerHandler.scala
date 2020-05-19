import PriceGenerator.InternalPriceQuery
import Server.PriceResponse
import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import akka.util.Timeout
import akka.pattern.ask

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.Success

class ServerHandler extends Actor with ActorLogging {

  implicit val timeout: Timeout = 300.milliseconds
  implicit val ec: ExecutionContext = context.dispatcher

  override def receive: Receive = {
    case ipq: InternalPriceQuery => {
      log.debug("Received query")
      val q1 = (context.actorOf(Props[PriceGenerator]) ? ipq).asInstanceOf[Future[PriceResponse]]
      val q2 = (context.actorOf(Props[PriceGenerator]) ? ipq).asInstanceOf[Future[PriceResponse]]
      val f = q1.zipWith(q2)((r1, r2) => Seq(r1.price, r2.price).min).fallbackTo(q1.map(_.price)).fallbackTo(q2.map(_.price))
      f.onComplete {
        case Success(res) => ipq.sender ! PriceResponse(ipq.name, res)
        case _ =>
      }
      self ! PoisonPill
    }
  }
}
