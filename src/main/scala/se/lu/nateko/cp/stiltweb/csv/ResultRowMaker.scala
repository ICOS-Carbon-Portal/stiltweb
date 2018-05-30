package se.lu.nateko.cp.stiltweb.csv

import se.lu.nateko.cp.stiltweb.csv.RawRow.Assignment

object ResultRowMaker {

	val plainCopyCols = Vector("ident", "latstart", "lonstart", "aglstart", "zi", "wind.dir")

	val renameCopyCols = Vector(
		"ubar" -> "wind.u",
		"vbar" -> "wind.v",
		"wbar" -> "wind.w",
		"co2ini" -> "co2.background",
		"coini" -> "co.background"
	)

	val vegetationTypes = Vector("evergreen", "decid", "mixfrst", "shrb", "savan", "crop", "grass", "others")

	def makeRow(in: RawRow): Map[String, Double] = {

		def fuelAssignments(gas: Gas.Gas): Vector[Assignment] =
			for((fuel, sum) <- in.byFuelReport(gas).toVector) yield s"$gas.fuel.$fuel" -> sum

		def categoryAssignments(categName: String, filter: Category => Boolean): Vector[Assignment] =
			for((gas, sum) <- in.byCategoryReport(filter).toVector) yield s"$gas.$categName" -> sum

		val cementAssignments = for((gas, sum) <- in.cementReport) yield s"$gas.cement" -> sum

		val co2FuelAssignments = fuelAssignments(Gas.CO2)
		val coFuelAssignments = fuelAssignments(Gas.CO)

		val initialAssignments: Vector[Assignment] =
			plainCopyCols.map(in.copy) ++
			renameCopyCols.map{
				case (from, to) => in.copy(to, from)
			} ++
			categoryAssignments("energy", _.isEnergy) ++
			categoryAssignments("industry", _.isIndustry) ++
			categoryAssignments("transport", _.isTransport) ++
			categoryAssignments("others", _.isOther) ++
			cementAssignments ++
			co2FuelAssignments ++
			coFuelAssignments :+
			"co2.fuel" -> co2FuelAssignments.map(_._2).sum :+
			"co.fuel" -> coFuelAssignments.map(_._2).sum :+
			in.sum("co2.bio.resp", vegetationTypes.map("resp" + _)) :+
			in.sum("co2.bio.gee", vegetationTypes.map("gee" + _)) :+
			in.sum("rn", Seq("rnini", "rn")) :+
			in.sum("rn.noah", Seq("rn_noahini", "rn_noah")) :+
			in.sum("rn.era", Seq("rn_eraini", "rn_era"))

		val rowPostProcessingSummations: Vector[(Seq[String], String)] = Vector(
			Seq("co2.bio.resp", "co2.bio.gee") -> "co2.bio",
			Seq("co2.background", "co2.bio", "co2.fuel", "co2.cement") -> "co2.stilt",
			Seq("co.background", "co.fuel", "co.cement") -> "co.stilt"
		)

		rowPostProcessingSummations.foldLeft(initialAssignments.toMap){
			case (map, (summedVars, targetVar)) =>
				val sum = summedVars.map(map.apply).sum
				map + (targetVar -> sum)
		}
	}
}
