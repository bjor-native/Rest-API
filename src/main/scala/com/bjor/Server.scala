package com.bjor

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import com.bjor.controllers.PointController

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Server extends App with PointController {

  implicit val actorSystem: ActorSystem = ActorSystem("AkkaHTTPExampleServices")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  /*
  lazy val apiRoutes: Route = pathPrefix("api") {
    PointRoutes
  }

   */

  Http().bindAndHandle(PointRoutes, "localhost", 8080)
  logger.info("Starting the HTTP server at 8080")
  Await.result(actorSystem.whenTerminated, Duration.Inf)
}