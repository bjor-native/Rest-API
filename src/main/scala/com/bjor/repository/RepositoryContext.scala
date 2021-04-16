package com.bjor.repository

import akka.actor.{ActorSystem, Scheduler}
import scala.concurrent.ExecutionContextExecutor

object RepositoryContext {
  lazy val actorSystem: ActorSystem = ActorSystem("RepositoryContext")
  lazy val scheduler: Scheduler = actorSystem.scheduler
  implicit lazy val executionContext: ExecutionContextExecutor = actorSystem.dispatcher
}