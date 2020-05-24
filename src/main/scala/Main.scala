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

  val msgPattern: String = "Command pattern: clientN productName1;clientM productName2"
  println("Started app with http server on http://localhost:8080/")
  println(s"Type :q to stop...")
  println(msgPattern)
  Source.fromInputStream(System.in).getLines.foreach {
    case ":q" =>
      system.terminate()
      System.exit(0)
    case msg =>
      msg
        .split(';')
        .map(_.trim.split(' '))
        .filter(_.length == 2)
        .foreach { toks =>
          val clientName = toks.head
          val productName = toks(1)
          clients.get(clientName).fold(println(s"[Wrong input] $msgPattern"))(_ ! productName)
        }
  }
}
