package se.lu.nateko.cp.stiltweb.csv

import java.nio.file.Paths
import java.nio.file.Files
import org.scalatest.matchers.should.Matchers._
import org.scalatest.funsuite.AnyFunSuite
import spray.json.JsNumber
import spray.json.JsObject
import spray.json.JsString
import spray.json.enrichString

class ResultRowMakerTests extends AnyFunSuite:

	def resourcePath(path: String) = Paths.get(getClass.getResource(path).toURI)

	def rawRow: RawRow = {
		val inPath = resourcePath("/stiltresult2009x51.97Nx004.93Ex00020_raw.csv")
		val inLines = Files.readAllLines(inPath)
		RawRow.parse(inLines.get(0), inLines.get(1))
	}

	def expectedRow: JsObject =
		val inPath = resourcePath("/stiltresult2009x51.97Nx004.93Ex00020.json")
		Files.readString(inPath).parseJson.asJsObject


	def toDouble(s: String): Double = try{s.toDouble}catch {
		case _: NumberFormatException => Double.NaN
	}

	val disregardedVals = Set("date", "day", "isodate", "month", "year", "hour")

	test("makeRow works as expected"):
		val row = ResultRowMaker.makeRow(rawRow)
		val expRow = expectedRow
		assert(row.fields.keySet === expRow.fields.keySet)

		for (col, v) <- row.fields do
			(v, expRow.fields(col)) match
				case (JsNumber(n), JsNumber(expNum)) =>
					val d = n.toDouble
					val expD = expNum.toDouble
					assert(d === expD +- 1e-10)

				case (JsString(s), JsString(expS)) =>
					assert(s === expS)

				case bad => fail(s"Unexpected combination of actual/expected CSV row values: $bad")

end ResultRowMakerTests
