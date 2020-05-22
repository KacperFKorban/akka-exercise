import DBHandler.QueryQuantity
import Server.PriceQuery
import akka.actor.{Actor, PoisonPill, Props}
import cats.effect.IO
import doobie.util.transactor.Transactor
import doobie.implicits._

class DBHandler(val xa: Transactor.Aux[IO, Unit]) extends Actor {

  def insertQuery(name: String): Int =
    sql"insert into queries (name, quantity) values ($name, 1)".update.run.transact(xa).unsafeRunSync()

  def updateQuery(name: String): Int =
    sql"update queries set quantity = quantity + 1 where name = $name".update.run.transact(xa).unsafeRunSync()

  def quantityByName(name: String): Option[Long] =
    sql"select quantity from queries where name = $name".query[Long].to[List].transact(xa).unsafeRunSync().headOption

  override def receive: Receive = {
    case PriceQuery(name) => {
      val q = quantityByName(name)
      q.fold(insertQuery(name))(_ => updateQuery(name))
      sender ! QueryQuantity(name, q.getOrElse(0))
      self ! PoisonPill
    }
  }
}

object DBHandler {
  case class QueryQuantity(name: String, quantity: Long)
  def props(xa: Transactor.Aux[IO, Unit]): Props = Props(new DBHandler(xa))
}