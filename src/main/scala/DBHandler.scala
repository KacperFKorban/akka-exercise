import DBHandler.{UpdateMem, UpdateResult}
import Server.PriceQuery
import akka.actor.Actor
import cats.effect.IO
import doobie.util.transactor.Transactor
import doobie.implicits._

class DBHandler extends Actor {

  implicit val cs = IO.contextShift(context.dispatcher)

  val xa = Transactor.fromDriverManager[IO](
    "org.sqlite.JDBC", "jdbc:sqlite:queries.db", "", ""
  )

  def insertQuery(name: String): Unit =
    sql"insert into queries (name, quantity) values ($name, 1)".update.run.transact(xa).unsafeRunAsyncAndForget()

  def updateQuery(name: String): Unit =
    sql"update queries set quantity = quantity + 1 where name = $name".update.run.transact(xa).unsafeRunAsyncAndForget()

  def quantityByName(name: String): Option[Long] =
    sql"select quantity from queries where name = $name".query[Long].to[List].transact(xa).unsafeRunSync().headOption

  def all(): Map[String, Long] =
    sql"select name, quantity from queries".query[(String, Long)].toMap.transact(xa).unsafeRunSync()

  override def receive: Receive = {
    case UpdateMem => sender ! UpdateResult(all())
    case PriceQuery(name) => {
      val q = quantityByName(name)
      q.fold(insertQuery(name))(_ => updateQuery(name))
    }
  }
}

object DBHandler {
  case object UpdateMem
  case class UpdateResult(state: Map[String, Long])
}