import PriceGenerator.{InternalPriceQuery, InternalPriceResponse}
import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill}

import scala.util.Random

class PriceGenerator extends Actor with ActorLogging {
  override def receive: Receive = {
    case InternalPriceQuery(name, _) => {
      Thread.sleep(Random.nextInt(400) + 100)
      val p = Random.nextInt(10) + 1
      log.debug(p.toString)
      sender ! InternalPriceResponse(name, p)
      self ! PoisonPill
    }
  }
}

object PriceGenerator {
  case class InternalPriceQuery(name: String, sender: ActorRef)
  case class InternalPriceResponse(name: String, price: Long)
}
