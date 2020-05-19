import akka.actor.{ActorRef, ActorSystem, Props}

import scala.io.StdIn._

object Main extends App {

  implicit val system: ActorSystem = ActorSystem("system")

  val server = system.actorOf(Props[Server], "server")

  val clients: Map[String, ActorRef] =
    (1 to 5)
      .map(i => s"client$i")
      .map(name => name -> system.actorOf(Client.props(server), name))
      .toMap

  println("Started app")
  println("Command pattern: clientN productName")
  while (true) {
    val tokens = readLine().split(' ')
    if(tokens.size != 2) {
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
