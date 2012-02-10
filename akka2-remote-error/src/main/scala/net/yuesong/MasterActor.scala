package net.yuesong

import java.net.InetAddress

import akka.actor.actorRef2Scala
import akka.actor.{ ActorRef, ActorLogging, Actor }
import akka.remote.{ RemoteClientLifeCycleEvent, RemoteClientError, RemoteClientConnected }
import akka.util.duration.intToDurationInt

class MasterActor extends Actor with ActorLogging {

  var slaves: Seq[Slave] = Nil

  def receive = {
    case PingSlaves => slaves.foreach(_.actor ! Ping)
    case RemoteClientConnected(_, addr) =>
      println("RemoteClientConnected (%s)".format(addr))
    case RemoteClientError(e, _, addr) =>
      println("RemoteClientError (%s): %s)".format(addr, e.getMessage))
    case e: RemoteClientLifeCycleEvent =>
      println(e)
  }

  override def preStart = {
    slaves = List(
      Slave(context.actorFor(slavePath("Slave1System", 2553)), SlaveStatus.Unknown),
      Slave(context.actorFor(slavePath("Slave2System", 2554)), SlaveStatus.Unknown)
    )
    context.system.eventStream.subscribe(self, classOf[RemoteClientLifeCycleEvent])
    context.system.scheduler.schedule(2 seconds, 10 seconds, self, PingSlaves)
  }

  private def slavePath(slaveSystem: String, slavePort: Int) = {
    "akka://%s@%s:%s/user/slave".format(slaveSystem, InetAddress.getLocalHost.getHostAddress, slavePort)
  }

}

case object PingSlaves

case class Slave(actor: ActorRef, status: SlaveStatus.Value)

object SlaveStatus extends Enumeration {
  type SlaveStatus = Value
  val Unknown, Running, Down = Value
}