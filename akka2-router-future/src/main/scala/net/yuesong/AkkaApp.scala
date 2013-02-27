package net.yuesong

import scala.compat.Platform.currentTime
import scala.util.Random
import org.slf4j.LoggerFactory
import com.typesafe.config.ConfigFactory
import akka.actor.actorRef2Scala
import akka.actor.{ Props, ActorSystem, ActorRef, Actor }
import akka.pattern._
import akka.routing.RoundRobinRouter
import akka.util.Timeout
import akka.util.duration._
import akka.dispatch.Future

object AkkaApp extends App {

  val system = ActorSystem("MySystem", ConfigFactory.load.getConfig("myapp"))
  val boss = system.actorOf(Props[Boss], "boss")

  boss ! Order(40)

  sys.addShutdownHook(system.shutdown)
}

class Boss extends Actor {

  private val log = LoggerFactory.getLogger(getClass)

  var worker: ActorRef = _
  var incomplete: Int = 0

  override def preStart = {
    worker = context.actorOf(Props[Worker]
      .withRouter(RoundRobinRouter(10))
      .withDispatcher("akka.actor.task-dispatcher"),
      "worker")
  }

  def receive = {
    case o @ Order(amount) =>
      log.debug("{}: received {}", self.path, o)
      incomplete += amount
      for(i <- 1 to amount) worker ! newRandomTask
    case Completed(t)=>
      log.debug("{}: received {}", self.path, t)
      incomplete -= 1
      if (incomplete == 0) log.info("Completed all tasks")
    case v =>
      log.warn("What's this: {}?", v)
  }

  private def newRandomTask = {
    Random.nextInt(10) match {
      case 0 => DangerousTask(Random.nextInt(4000) + 1000)
      case _ => SafeTask(Random.nextInt(4000) + 1000)
    }
  }

}

class Worker extends Actor {

  private val log = LoggerFactory.getLogger(getClass)

  def receive = {
    case task: Task =>
      log.debug("{}: received {}", self.path, task)
      sender ! Worker.perform(task)
  }

}

object Worker {

  private val log = LoggerFactory.getLogger(getClass)

  def perform(task: Task): TaskResult = {
    val start = currentTime
    Thread.sleep(task.load)
    if (task.isInstanceOf[SafeTask]) {
      log.debug("{} processed in {} ms", task, currentTime - start)
      Completed(task)
    } else {
      sys.error("%s blew up after %s ms".format(task, currentTime - start))
    }
  }
}

case class Order(amount: Int)

trait Task { val load: Int }
case class SafeTask(load: Int) extends Task
case class DangerousTask(load: Int) extends Task

trait TaskResult { val task: Task }
case class Completed(task: Task) extends TaskResult
case class Failed(task: Task) extends TaskResult
