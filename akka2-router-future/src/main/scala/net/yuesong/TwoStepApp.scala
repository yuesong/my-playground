package net.yuesong.twostep

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
import java.util.concurrent.atomic.AtomicInteger
import akka.actor.Status

object TwoStepApp extends App {

  private val log = LoggerFactory.getLogger(getClass)

  val system = ActorSystem("TwoStepAppSystem", ConfigFactory.load.getConfig("two-step-app"))
  val queryService = system.actorOf(Props[QueryService]
    .withRouter(RoundRobinRouter(16))
    .withDispatcher("akka.actor.query-dispatcher"),
    "query-service")
  val dspService = system.actorOf(Props[DspService]
    .withRouter(RoundRobinRouter(32))
    .withDispatcher("akka.actor.dsp-dispatcher"),
    "dsp-service")
  val boss = system.actorOf(Props(new Boss(queryService, dspService)), "boss")

  boss ! 20

  sys.addShutdownHook(system.shutdown)
}

class Boss(queryService: ActorRef, dspService: ActorRef) extends Actor {

  private val log = LoggerFactory.getLogger(getClass)

  def receive = {
    case num: Int =>
      log.info("{}: received {}", self.path, num)
      for (i <- 1 to num) {
        val task = newRandomTask
        context.actorOf(Props(new Worker(queryService, dspService))) ! task
      }
  }

  private def newRandomTask = Task(Task.nextId, Random.nextInt(4000) + 1000)

}

class Worker(queryService: ActorRef, dspService: ActorRef) extends Actor {

  private val log = LoggerFactory.getLogger(getClass)

  def receive = {
    case msg @ Task(_, spec) =>
      log.info("{}: processing {}", self.path, msg)
      val future = for {
        QueryResult(data) <- query(spec)
        result <- send(data)
      } yield (msg, result)
      future pipeTo self

    case (task: Task, result: Seq[Boolean]) =>
      val ok = result.count(_ == true)
      val fail = result.length - ok
      log.info("%s: completed %s with ok=%s, fail=%s".format(self.path, task, ok, fail))

    case msg =>
      log.warn("%s: received unknown message (%s): %s".format(self.path, msg.getClass, msg))
  }

  private def query(spec: Int): Future[QueryResult] = {
    //    log.info("{}: query {}", self.path, spec)
    implicit val timeout = Timeout(30 seconds)
    (queryService ? Query(spec)).mapTo[QueryResult]
  }

  private def send(data: Seq[Any]): Future[Seq[Boolean]] = {
    //    log.info("{}: send {}", self.path, data)
    implicit val timeout = Timeout(30 seconds)
    implicit val executor = context.dispatcher
    Future.traverse(data)(value => (dspService ? Send(value)).mapTo[Boolean])
  }

}

class QueryService extends Actor {
  private val log = LoggerFactory.getLogger(getClass)

  def receive = {
    case msg @ Query(spec) =>
      log.trace("{}: received {}", self.path, msg)
      try {
        Thread.sleep(spec)
        if (Random.nextInt(10) == 0) throw sys.error("Error processing " + msg)
        sender ! QueryResult(1 to spec / 100)
      } catch {
        case e =>
          sender ! Status.Failure(e)
      }
      log.trace("{}: replied to {}", self.path, sender)
  }
}

class DspService extends Actor {

  private val log = LoggerFactory.getLogger(getClass)

  def receive = {
    case msg: Send =>
      log.trace("{}: received {}", self.path, msg)
      Thread.sleep(Random.nextInt(100) + 100)
      val result = if (Random.nextInt(10) == 0) false else true
      sender ! result
      log.trace("{}: replied to {}", self.path, sender)
  }
}

case class Task(id: Int, spec: Int)
object Task {
  private val _id = new AtomicInteger(0)
  def nextId: Int = _id.incrementAndGet
}

case class Query(spec: Int)
case class QueryResult(data: Seq[Any])
case class Send(value: Any)
