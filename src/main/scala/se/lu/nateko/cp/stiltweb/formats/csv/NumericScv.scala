package se.lu.nateko.cp.stiltweb.formats.csv

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.util.ByteString

import scala.io.{Source => IoSource}

object NumericScv {

	def getJsonSource(scv: IoSource, columns: Seq[String]): Source[ByteString, NotUsed] = Source.fromIterator(() => {
		val lines = scv.getLines()
		val columnsLookup = lines.next().split(' ').zipWithIndex.toMap
		val positions = columns.map(columnsLookup.get).flatten

		val firstLine = {
			val line = lines.next()
			val allCells = line.split(' ')
			positions.map(allCells.apply).mkString("[\n[", ", ", "]")
		}
		
		val rowsJson: Iterator[String] = lines.map{line =>
			val allCells = line.split(' ')
			positions.map(allCells.apply).mkString(",\n[", ", ", "]")
		}

		Iterator(firstLine) ++ rowsJson ++ Iterator("\n]") map ByteString.apply
	})
}
