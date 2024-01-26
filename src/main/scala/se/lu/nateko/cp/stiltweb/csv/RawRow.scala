package se.lu.nateko.cp.stiltweb.csv

class RawRow private(val vals: Map[Variable, Double]) {
	import RawRow._

	def copy(toVar: String, fromVar: String): Assignment = toVar -> vals(PlainVariable(fromVar))
	def copy(varName: String): Assignment = copy(varName, varName)
	def sum(toVar: String, fromVars: Seq[String]): Assignment =
		toVar -> fromVars.map(v => vals(PlainVariable(v))).sum

	def byFuelReport(subFuel: Option[String]): Map[Tracer, Map[Fuel, Double]] = vals.keys
		.collect{
			case fi: FuelInfoVariable if fi.tracer != Tracer.othergas && subFuel.fold(true)(_ == fi.fuelSubtype) => fi
		}
		.groupBy(_.tracer).view.mapValues(
			_.groupMapReduce(_.fuel)(vals.apply)(_ + _)
		).toMap

	val cementReport: Map[Tracer, Double] = vals.keys
		.collect{
			case pcv: PlainCategoryVariable if pcv.isCement && pcv.tracer != Tracer.othergas => pcv
		}
		.groupMapReduce(_.tracer)(vals.apply)(_ + _)

	def byCategoryReport(filter: Category => Boolean): Map[Tracer, Double] = vals.keys
		.collect{
			case cv: CategoryVariable if cv.tracer != Tracer.othergas => cv
		}
		.filter(fiv => filter(fiv.category))
		.groupMapReduce(_.tracer)(vals.apply)(_ + _)

}

object RawRow{
	val BlacklistedFragment = "ffm"

	type Assignment = (String, Double)

	def parse(header: String, values: String): RawRow = {

		val colNames = header.split(' ')
		val rowValues = values.split(' ')
		assert(colNames.length == rowValues.length, "Number of column names and number of values must be equal when parsing STILT CSV output")

		val pairs = colNames.iterator.zip(rowValues.iterator).collect{
			case (colName, valStr) if !colName.contains(BlacklistedFragment) =>
				val col = colName.stripPrefix("\"").stripSuffix("\"")
				val valTrimmed = valStr.trim
				val numVal = if(valTrimmed.isEmpty) Double.NaN else valTrimmed.toDouble
				Variable(col) -> numVal
		}
		new RawRow(pairs.toMap)
	}
}
