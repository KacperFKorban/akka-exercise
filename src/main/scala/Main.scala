import Server.{NoPrices, PriceQuery, PriceResponse, ServerResponse}
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.Materializer
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.HttpRequest
import akka.pattern.ask
import akka.util.Timeout
import org.jsoup.Jsoup

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn._

object Main extends App {

  implicit val system: ActorSystem = ActorSystem("system")
  implicit val materializer: Materializer = Materializer.createMaterializer(system)
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val timeout: Timeout = 1.second

  val server = system.actorOf(Props[Server], "server")

  val route =
    concat(
      pathPrefix("price" / Remaining) { name =>
        get {
          val res = (server ? PriceQuery(name)).fallbackTo(Future { NoPrices(name) }).mapTo[ServerResponse]
          complete(res)
        }
      },
      pathPrefix("review" / Remaining) { name =>
        get {
          complete(Http.get(system).singleRequest(HttpRequest(uri = s"https://www.opineo.pl/?szukaj=$name&s=2")).flatMap{ response =>
            response.entity.toStrict(timeout.duration)
          }.map { entity =>
            Jsoup
              .parse(entity.data.utf8String)
              .body()
              .getElementById("page")
              .getElementById("content")
              .getElementById("screen")
              .getElementsByClass("pls")
              .get(0)
              .getElementsByClass("shl_i pl_i")
              .get(0)
              .getElementsByClass("pl_attr")
              .get(0)
              .getElementsByTag("li")
              .eachText()
              .toArray
              .map(_.toString)
              .mkString("\n")
          })
        }
      }
    )

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  val clients: Map[String, ActorRef] =
    (1 to 5)
      .map(i => s"client$i")
      .map(name => name -> system.actorOf(Client.props(server), name))
      .toMap

  println("Started app with http server on http://localhost:8080/")
  println(s"Type :q to stop...")
  println("Command pattern: clientN productName")
  var running = true
  while (running) {
    val msg = readLine()
    val tokens = msg.split(' ')
    if (msg == ":q") {
      running = false
      bindingFuture
        .flatMap(_.unbind())
        .onComplete(_ => system.terminate())
    } else if(tokens.size != 2) {
      println("Command pattern: clientN productName")
    } else {
      val clientName = tokens.head
      if (!"client[1-5]+".r.matches(clientName)) {
        println("Command pattern: clientN productName")
      } else {
        val productName = tokens(1)
        val client = clients(clientName)
        client ! productName
      }
    }
  }

}
