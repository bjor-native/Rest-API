package com.bjor.controllers

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.caching.scaladsl.Cache
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpMethods, StatusCodes, Uri}
import akka.http.scaladsl.server.{RequestContext, Route, RouteResult}
import akka.http.scaladsl.server.Directives.{pathPrefix, _}
import com.bjor.controllers.PointController.QueryPoint
import com.bjor.repository.PointRepository
import com.bjor.repository.PointRepository.{Point, Sales}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.util.{Failure, Success}

object PointController {

  case class QueryPoint(numberOfSales: String, index: String, region: String, id: String)

  object PointJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {

    implicit val pointFormat: RootJsonFormat[Point] = jsonFormat4(Point.apply)
    implicit val pointQueryFormat: RootJsonFormat[QueryPoint] = jsonFormat4(QueryPoint.apply)
    implicit val salesFormat: RootJsonFormat[Sales] = jsonFormat2(Sales.apply)
  }
}

trait PointController
  extends PointRepository {

  implicit def actorSystem: ActorSystem

  lazy val logger: LoggingAdapter = Logging(actorSystem, classOf[PointController])

  import PointRepository._
  import com.bjor.controllers.PointController.PointJsonProtocol._

  lazy val simpleKeyer: PartialFunction[RequestContext, Uri] = {
    case r: RequestContext if r.request.method == HttpMethods.GET => r.request.uri
  }

  def complexKeyer(payload: String): PartialFunction[RequestContext, String] = {
    case r: RequestContext if r.request.method == HttpMethods.POST => s"${r.request.uri}_${payload}"
  }

  import akka.http.scaladsl.server.directives.CachingDirectives._
  import spray.json._

  lazy val myCache: Cache[Uri, RouteResult] = routeCache[Uri]
  lazy val myComplexCache: Cache[String, RouteResult] = routeCache[String]

  lazy val PointRoutes: Route = pathPrefix("api") {
    pathPrefix("id") {
      get {
        path(Segment) { id =>
          onComplete(getPointById(id)) {
            case Success(point) =>
              logger.info(s"Got the point records given the point id ${id}")
              complete(StatusCodes.OK, point)
            case Failure(throwable) =>
              logger.error(s"Failed to get the point record given the point id ${id}")
              throwable match {
                case e: PointNotFoundException => complete(StatusCodes.NotFound, "No point found")
                case e: DubiousPointRecordsException => complete(StatusCodes.NotFound, "Dubious records found.")
                case _ => complete(StatusCodes.InternalServerError, "Failed to get the point.")
              }
          }
        } ~ pathPrefix("region") {
          get {
            path(Segment) { name =>
              onComplete(getPointByRegion(name)) {
                case Success(point) =>
                  logger.info(s"Got the point records given the point name ${name}")
                  complete(StatusCodes.OK, point)
                case Failure(throwable) =>
                  logger.error(s"Failed to get the point record given the point name ${name}")
                  throwable match {
                    case e: PointNotFoundException => complete(StatusCodes.NotFound, "No point found")
                    case e: DubiousPointRecordsException => complete(StatusCodes.NotFound, "Dubious records found.")
                    case _ => complete(StatusCodes.InternalServerError, "Failed to get the point.")
                  }
              }
            }
          }
        } ~ pathPrefix("sales-region") {
          get {
            path(Segment) { sale =>
              onComplete(getSalesQ(sale)) {
                case Success(point) =>
                  logger.info(s"Got the point records given the point sales ${sale}")
                  complete(StatusCodes.OK, point)
                case Failure(throwable) =>
                  logger.error(s"Failed to get the point record given the point sales ${sale}")
                  throwable match {
                    case e: PointNotFoundException => complete(StatusCodes.NotFound, "No point found")
                    case e: DubiousPointRecordsException => complete(StatusCodes.NotFound, "Dubious records found.")
                    case _ => complete(StatusCodes.InternalServerError, "Failed to get the points.")
                  }
              }
            }
          }
        }
      } ~ pathPrefix("index-start-with") {
        get {
          path(Segment) { idx =>
            onComplete(getIndexStartWith(idx)) {
              case Success(point) =>
                logger.info(s"Got the point records given the point index start with ${idx}")
                complete(StatusCodes.OK, point)
              case Failure(throwable) =>
                logger.error(s"Failed to get the point record given the point index start with ${idx}")
                throwable match {
                  case e: PointNotFoundException => complete(StatusCodes.NotFound, "No point found")
                  case e: DubiousPointRecordsException => complete(StatusCodes.NotFound, "Dubious records found.")
                  case _ => complete(StatusCodes.InternalServerError, "Failed to get the point.")
                }
            }
          }
        }
      } ~ pathPrefix("sum-sales-region") {
        get {
          path(Segment) {
            name =>
              onComplete(getAllSalesRegion(name)) {
                case Success(point) =>
                  logger.info(s"Got the point records given the point sum sales ${name}")
                  complete(StatusCodes.OK, point)
                case Failure(throwable) =>
                  logger.error(s"Failed to get the point record given the point sum sales ${name}")
                  throwable match {
                    case e: PointNotFoundException => complete(StatusCodes.NotFound, "No point found")
                    case e: DubiousPointRecordsException => complete(StatusCodes.NotFound, "Dubious records found.")
                    case _ => complete(StatusCodes.InternalServerError, "Failed to get the point.")

                  }
              }
          }
        }
      }
    } ~ post {
      pathPrefix("query") {
        entity(as[QueryPoint]) { q =>
          onComplete(queryPoint(q.numberOfSales, q.index, q.region, q.id)) {
            case Success(point) =>
              logger.info("Got the point records for the search query.")
              complete(StatusCodes.OK, point)
            case Failure(throwable) =>
              logger.error("Failed to get the points with the given query condition.")
              complete(StatusCodes.InternalServerError, "Failed to query the points.")
          }
        }
      }
    }
  }
}