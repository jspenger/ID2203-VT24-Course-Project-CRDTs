package crdtactor

import org.apache.pekko.actor.typed.scaladsl.ActorContext
import org.apache.pekko.actor.typed.scaladsl.AbstractBehavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.cluster.ddata
import org.apache.pekko.cluster.ddata.DeltaReplicatedData
import org.apache.pekko.cluster.ddata.ReplicatedData
import org.apache.pekko.cluster.ddata.ReplicatedDelta
import org.apache.pekko.cluster.ddata.SelfUniqueAddress
import org.apache.pekko.actor.typed.ActorRef

object CRDTActor {
  // The type of messages that the actor can handle
  sealed trait Command

  // Messages containing the CRDT delta state exchanged between actors
  case class DeltaMsg(from: ActorRef[Command], delta: ReplicatedDelta)
      extends Command

  // Triggers the actor to start the computation (do this only once!)
  case object Start extends Command

  // Triggers the actor to consume an operation (do this repeatedly!)
  case object ConsumeOperation extends Command
}

import CRDTActor.*

class CRDTActor(
    id: Int,
    ctx: ActorContext[Command]
) extends AbstractBehavior[Command](ctx) {
  // The CRDT state of this actor, mutable var as LWWMap is immutable
  private var crdtstate = ddata.LWWMap.empty[String, Int]

  // The CRDT address of this actor/node, used for the CRDT state to identify the nodes
  private val selfNode = Utils.nodeFactory()

  // Hack to get the actor references of the other actors, check out `lazy val`
  // Careful: make sure you know what you are doing if you are editing this code
  private lazy val others =
    Utils.GLOBAL_STATE.getAll[Int, ActorRef[Command]]()

  // Note: you probably want to modify this method to be more efficient
  private def broadcastAndResetDeltas(): Unit =
    val deltaOption = crdtstate.delta
    deltaOption match
      case None => ()
      case Some(delta) =>
        crdtstate = crdtstate.resetDelta // May be omitted
        others.foreach { //
          (name, actorRef) =>
            actorRef !
              DeltaMsg(ctx.self, delta)
        }

  // This is the event handler of the actor, implement its logic here
  // Note: the current implementation is rather inefficient, you can probably
  // do better by not sending as many delta update messages
  override def onMessage(msg: Command): Behavior[Command] = msg match
    case Start =>
      ctx.log.info(s"CRDTActor-$id started")
      ctx.self ! ConsumeOperation // start consuming operations
      Behaviors.same

    case ConsumeOperation =>
      val key = Utils.randomString()
      val value = Utils.randomInt()
      ctx.log.info(s"CRDTActor-$id: Consuming operation $key -> $value")
      crdtstate = crdtstate.put(selfNode, key, value)
      ctx.log.info(s"CRDTActor-$id: CRDT state: $crdtstate")
      broadcastAndResetDeltas()
      ctx.self ! ConsumeOperation // continue consuming operations, loops sortof
      Behaviors.same

    case DeltaMsg(from, delta) =>
      ctx.log.info(s"CRDTActor-$id: Received delta from ${from.path.name}")
      // Merge the delta into the local CRDT state
      crdtstate = crdtstate.mergeDelta(delta.asInstanceOf) // do you trust me?
      Behaviors.same
  Behaviors.same
}
