package com.bjor.repository

import com.bjor.repository.PointRepository.Point

import scala.concurrent.Future
import scala.io.BufferedSource

object Utils {

  def getCSV: Seq[Point] = {
    val f: BufferedSource = scala.io.Source.fromFile("test_data.csv")
    val line: String = f.mkString
    val lines: Array[String] = line.split('\n')
    val entities: Seq[Point] =
      lines.map { line =>
        line.split(';').map(_.trim) match {
          case Array(year, mark, model, comment) =>
            Point(year, mark, model, comment)
        }
      }.toSeq
    f.close()
    entities
  }

  implicit class CSVWrapper(val prod: Product) extends AnyVal {
    def toCSV: String = prod.productIterator.map {
      case Some(value) => value
      case None => ""
      case rest => rest
    }.mkString(";")
  }
}
