package se.lu.nateko.cp.stiltweb.csv

import org.scalatest.FunSuite
import java.nio.file.Paths
import java.nio.file.Files
import org.scalatest.Matchers._

class ResultRawMakerTests extends FunSuite{

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
		val vnames = row.keySet ++ expRow.keySet.toSeq.sorted

		for(vname <- vnames){
			val expValue = expRow.get(vname).getOrElse(Double.NaN)
			val actValue = row.get(vname).getOrElse(Double.NaN)
			if(!(actValue === (expValue +- 1e-8)) && !disregardedVals.contains(vname))
				println(s"$vname: expected $expValue -- got $actValue")
		}
	}
}
