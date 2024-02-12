package crdtactor

import scala.concurrent.duration.Duration
import scala.concurrent.duration.DurationInt
import scala.concurrent.Await

import org.apache.pekko.actor.typed.scaladsl.adapter._
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.util.Timeout
import org.apache.pekko.actor.typed.scaladsl.Behaviors

object Bootstrap {

  // Startup the actors and execute the workload
  def apply(): Unit =
    val N_ACTORS = 8

    Utils.setLoggerLevel("INFO")

    val system = ActorSystem("CRDTActor")

    // Create the actors
    val actors = (0 until N_ACTORS).map { i =>
      val name = s"CRDTActor-$i"
      val actorRef = system.spawn(
        Behaviors.setup[CRDTActor.Command] { ctx => new CRDTActor(i, ctx) },
        name
      )
      i -> actorRef
    }.toMap

    // Write actor addresses into the global state
    actors.foreach((id, actorRef) => Utils.GLOBAL_STATE.put(id, actorRef))

    // Start the actors
    actors.foreach((_, actorRef) => actorRef ! CRDTActor.Start)

    // Sleep for a few seconds, then quit :)
    Thread.sleep(5000)

    // Force quit
    System.exit(0)
}
