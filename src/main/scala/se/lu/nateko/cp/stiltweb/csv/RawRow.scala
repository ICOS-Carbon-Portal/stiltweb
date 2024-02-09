package se.lu.nateko.cp.stiltweb.csv

import spray.json.JsNumber
import spray.json.JsObject
import spray.json.JsString

import scala.collection.mutable.Buffer

class RawRow private(val vals: Map[Variable, Double], val stiltMeta: String):
	import RawRow._

	def copy(toVar: String, fromVar: String): Assignment = toVar -> vals(PlainVariable(fromVar))
	def copy(varName: String): Assignment = copy(varName, varName)
	def sum(toVar: String, fromVars: Seq[String]): Assignment =
		toVar -> fromVars.map(v => vals(PlainVariable(v))).sum

	def byFuelReport(subFuel: Option[String]): Map[Tracer, Map[Fuel, Double]] = vals.keys
		.collect:
			case fi: FuelInfoVariable if fi.tracer != Tracer.othergas && subFuel.fold(true)(_ == fi.fuelSubtype) => fi
		.groupBy(_.tracer).view.mapValues(
			_.groupMapReduce(_.fuel)(vals.apply)(_ + _)
		).toMap

	val cementReport: Map[Tracer, Double] = vals.keys
		.collect:
			case pcv: PlainCategoryVariable if pcv.isCement && pcv.tracer != Tracer.othergas => pcv
		.groupMapReduce(_.tracer)(vals.apply)(_ + _)

	def byCategoryReport(gas: Tracer, filter: Category => Boolean): Double = vals.keys
		.collect:
			case cv: CategoryVariable if cv.tracer == gas && filter(cv.category) => vals(cv)
		.sum

	def toJson = JsObject:
		vals.map((k, v) => k.name -> JsNumber(v)) + (StiltMetaColName -> JsString(stiltMeta))


end RawRow

object RawRow:
	val BlacklistedFragment = "ffm"
	val StiltMetaColName = "metadata"

	type Assignment = (String, Double)

	def parse(header: String, values: String): RawRow =

		val colNames = header.split(' ')
		val rowValues = values.split(' ')
		assert(colNames.length == rowValues.length, "Number of column names and number of values must be equal when parsing STILT CSV output")

		val numVals = Buffer.empty[(Variable, Double)]
		var stiltMeta: String = ""

		colNames.iterator.zip(rowValues.iterator).foreach: (colName, valStr) =>
			val col = trimQuotes(colName)
			if col == StiltMetaColName then
				stiltMeta = trimQuotes(valStr)

			else if !col.contains(BlacklistedFragment) then
				val valTrimmed = valStr.trim
				val numVal = if(valTrimmed.isEmpty) Double.NaN else valTrimmed.toDouble
				numVals.append(Variable(col) -> numVal)

		RawRow(numVals.toMap, stiltMeta)
	end parse

	def trimQuotes(s: String): String = s.trim.stripPrefix("\"").stripSuffix("\"")
end RawRow
