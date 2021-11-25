package se.lu.nateko.cp.stiltweb.csv

import java.nio.file.Paths
import java.nio.file.Files
import org.scalatest.matchers.should.Matchers._
import org.scalatest.funsuite.AnyFunSuite

class ResultRowMakerTests extends AnyFunSuite{

	def resourcePath(path: String) = Paths.get(getClass.getResource(path).toURI)

	def rawRow: RawRow = {
		val inPath = resourcePath("/stiltresult2016x49.42Nx008.67Ex00030_1.csv")
		val inLines = Files.readAllLines(inPath)
		RawRow.parse(inLines.get(0), inLines.get(1))
	}

	def expectedRow: Map[String, Double] = {
		val inPath = resourcePath("/stiltresult2016x49.42Nx008.67Ex00030.csv")
		val inLines = Files.readAllLines(inPath)
		inLines.get(0).split(' ').zip(inLines.get(1).split(' ').map(toDouble)).toMap
	}

	def toDouble(s: String): Double = try{s.toDouble}catch {
		case _: NumberFormatException => Double.NaN
	}

	val disregardedVals = Set("date", "day", "isodate", "month", "year", "hour")

	test("makeRow works as expected"){
		val row = ResultRowMaker.makeRow(rawRow)
		val expRow = expectedRow
		val vnames = expRow.keySet.toSeq.sorted

		val maybeErrors: Seq[Option[String]] = for(vname <- vnames) yield{
			val expValue = expRow.get(vname).getOrElse(Double.NaN)
			val actValue = row.get(vname).getOrElse(Double.NaN)
			if(!(actValue === (expValue +- 1e-8)) && !disregardedVals.contains(vname))
				Some(s"$vname: expected $expValue -- got $actValue")
			else None
		}
		assert(maybeErrors.flatten === Nil)
	}
}
