package com.bjor.repository

import com.bjor.repository.Utils.{CSVWrapper, getCSV}

import java.io.{FileWriter, PrintWriter}
import scala.concurrent.Future
import scala.io.BufferedSource

object PointRepository {

  case class Point(numberOfSales: String, index: String, region: String, id: String)

  case class Sales(region: String, sum: String)

  class PointNotFoundException extends Throwable("No employee found in the database.")

  class DubiousPointRecordsException extends Throwable("Dubious Employee records found given the Employee ID.")
}


trait PointRepository {

  import PointRepository._
  import akka.pattern.after
  import scala.concurrent.duration._
  import com.bjor.repository.RepositoryContext._

  def fetchDBWithDelay(): Future[Seq[Point]] = {
    //val randomDuration = (Math.random() * 5 + 3).toInt.seconds
    Future {
      getCSV
    }
  }

  def getPointById(id: String): Future[Point] = fetchDBWithDelay().map { db =>
    val data = db.filter(_.id == id)
    if (data.isEmpty)
      throw new PointNotFoundException
    else if (data.isEmpty)
      throw new DubiousPointRecordsException
    else
      data.head
  }

  def getPointByRegion(region: String): Future[Seq[Point]] = fetchDBWithDelay().map { db =>
    val data = db.filter(_.region == region)
    if (data.isEmpty)
      throw new PointNotFoundException
    else
      data.seq
  }

  def getSalesQ(i: String): Future[Seq[Point]] = fetchDBWithDelay().map { db =>
    val data = db.filter(_.numberOfSales.toInt > i.toInt)
    if (data.isEmpty)
      throw new PointNotFoundException
    else
      data.seq
  }

  def getAllSalesRegion(region: String): Future[Sales] = fetchDBWithDelay().map { db =>
    var sum: BigInt = 0
    val data = db.filter(_.region == region)
    data.foreach(sum += _.numberOfSales.toInt)
    val dbs = Sales(data.head.region, sum.toString)
    if (data.isEmpty)
      throw new PointNotFoundException
    else
      dbs
  }

  def getIndexStartWith(index: String): Future[Seq[Point]] = fetchDBWithDelay().map { db =>
    val data = db.filter(_.index > index)
    if (data.isEmpty)
      throw new PointNotFoundException
    else
      data.seq
  }

  def queryPoint(numberOfSales: String, index: String, region: String, id: String): Future[Seq[Point]] = {
    fetchDBWithDelay().map { db =>
      val tmp = Point(numberOfSales = numberOfSales, index = index, region = region, id = id)

      new FileWriter("test_data.csv", true) {
        write("\n" + tmp.toCSV);
        close()
      }
      db :+ tmp
    }
  }
}
