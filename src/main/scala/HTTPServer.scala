import Server.{NoPrices, PriceQuery, ServerResponse}
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{complete, concat, get, pathPrefix}
import org.jsoup.Jsoup
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.HttpRequest
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.{ExecutionContext, Future}

class HTTPServer(server: ActorRef) {
  def route(implicit ec: ExecutionContext, timeout: Timeout, system: ActorSystem) = {
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
            try {
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
            } catch {
              case _: Exception => ""
            }
          })
        }
      }
    )
  }
}
