package net.yuesong

import com.typesafe.config.ConfigFactory

import akka.actor.{ Props, ActorSystem }

abstract class AkkaApp(configName: String) {
  val system = ActorSystem(configName.capitalize + "System", ConfigFactory.load.getConfig(configName))
  sys.addShutdownHook(system.shutdown)
}

object MasterApp extends AkkaApp("master") with App {
  val actor = system.actorOf(Props[MasterActor], "master")
}

object Slave1App extends SlaveApp("slave1")
object Slave2App extends SlaveApp("slave2")

class SlaveApp(configName: String) extends AkkaApp(configName) with App {
  val actor = system.actorOf(Props[SlaveActor], "slave")
}
