import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.Materializer
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.io.Source

object Main extends App {

  implicit val system: ActorSystem = ActorSystem("system")
  implicit val materializer: Materializer = Materializer.createMaterializer(system)
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val timeout: Timeout = 1.second

  val server = system.actorOf(Props[Server], "server")

  val httpServer = new HTTPServer(server)

  val bindingFuture = Http().bindAndHandle(httpServer.route, "localhost", 8080)

  val clients: Map[String, ActorRef] =
    (1 to 5)
      .map(i => s"client$i")
      .map(name => name -> system.actorOf(Client.props(server), name))
      .toMap

  println("Started app with http server on http://localhost:8080/")
  println(s"Type :q to stop...")
  println("Command pattern: clientN productName or clientN productName1;clientM productName2")
  Source.fromInputStream(System.in).getLines.foreach { msg =>
    val tokens = msg.split(';').map(_.split(' '))
    if (msg == ":q") {
      bindingFuture
        .flatMap(_.unbind())
        .onComplete(_ => {
          system.terminate()
          System.exit(0)
        })
    } else if(tokens.exists(_.length != 2)) {
      println("Command pattern: clientN productName or clientN productName1;clientM productName2")
    } else {
      tokens.foreach { toks =>
        val clientName = toks.head
        if (!"client[1-5]+".r.matches(clientName)) {
          println("Command pattern: clientN productName or clientN productName1;clientM productName2")
        } else {
          val productName = toks(1)
          val client = clients(clientName)
          client ! productName
        }
      }
    }
  }

}
