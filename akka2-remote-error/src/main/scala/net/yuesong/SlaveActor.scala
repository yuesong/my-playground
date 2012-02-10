package net.yuesong

import akka.actor.{ ActorLogging, Actor }

class SlaveActor extends Actor with ActorLogging {

  def receive = {
    case Ping =>
  }

}

case object Ping