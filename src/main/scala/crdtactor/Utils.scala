package crdtactor

import org.slf4j.{Logger => SLLogger}
import org.slf4j.{LoggerFactory => SLLoggerFactory}
import ch.qos.logback.classic.{Level => LBLevel}
import ch.qos.logback.classic.{Logger => LBLogger}
import ch.qos.logback.classic.{LoggerContext => LBLoggerContext}
import org.apache.pekko.actor.Address
import org.apache.pekko.cluster.ddata.SelfUniqueAddress
import org.apache.pekko.cluster.UniqueAddress
import java.util.concurrent.ThreadLocalRandom

object Utils {

  // State which is safe to use from different threads, uses inefficient locking mechanism
  class SynchronizedState {
    private val state = scala.collection.mutable.Map.empty[Any, Any]
    private val lock = new Object

    def get[K, V](key: K): Option[V] = lock.synchronized:
      state.get(key).asInstanceOf[Option[V]]

    def put[K, V](key: K, value: V): Unit = lock.synchronized:
      state.put(key, value)

    def remove[K, V](key: K): Option[V] = lock.synchronized:
      state.remove(key).asInstanceOf[Option[V]]

    def getAll[K, V](): scala.collection.immutable.Map[K, V] =
      lock.synchronized:
        state.asInstanceOf[scala.collection.mutable.Map[K, V]].toMap
  }

  // Global state which can be shared between the actors
  // Note: this is an "anti-pattern", use it only for the bootstrapping process
  final val GLOBAL_STATE = new SynchronizedState()

  private val r = new scala.util.Random
  def randomString(): String =
    r.nextString(2)

  def randomInt(): Int =
    r.nextInt(16)

  // Set the logger level which is used by Pekko
  def setLoggerLevel(level: String): Unit =
    val loggerContext: LBLoggerContext =
      SLLoggerFactory.getILoggerFactory().asInstanceOf[LBLoggerContext]
    val rootLogger: ch.qos.logback.classic.Logger =
      loggerContext.getLogger(SLLogger.ROOT_LOGGER_NAME)
    level match
      case "DEBUG" => rootLogger.setLevel(LBLevel.DEBUG)
      case "INFO"  => rootLogger.setLevel(LBLevel.INFO)
      case "WARN"  => rootLogger.setLevel(LBLevel.WARN)
      case "ERROR" => rootLogger.setLevel(LBLevel.ERROR)
      case "OFF"   => rootLogger.setLevel(LBLevel.OFF)
      case _ => throw new IllegalArgumentException(s"Unknown log level: $level")

  // Creates a unique address for use as node identifiers in the CRDT library
  // Example: 
  // val selfNode = Utils.nodeFactory()
  // crdtstate.put(selfNode, key, value)
  def nodeFactory() =
    SelfUniqueAddress(
      UniqueAddress(
        Address("crdtactor", "crdt"),
        ThreadLocalRandom.current.nextLong()
      )
    )
}
