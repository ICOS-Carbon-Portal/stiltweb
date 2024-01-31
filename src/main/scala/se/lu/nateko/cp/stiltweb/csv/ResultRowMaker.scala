package se.lu.nateko.cp.stiltweb.csv

import se.lu.nateko.cp.stiltweb.csv.RawRow.Assignment
import spray.json.JsObject
import spray.json.JsNumber
import spray.json.JsString


object ResultRowMaker:

	val plainCopyCols = Vector("ident", "latstart", "lonstart", "aglstart", "zi", "wind.dir")

	val renameCopyCols = Vector(
		"ubar" -> "wind.u",
		"vbar" -> "wind.v",
		"wbar" -> "wind.w",
		"co2ini" -> "co2.background",
		"coini" -> "co.background"
	)

	val vegetationTypes = Vector("evergreen", "decid", "mixfrst", "shrb", "savan", "crop", "grass", "others")

	val rowPostProcessingSummations: Vector[(Seq[String], String)] = Vector(
		Seq("co2.bio.resp", "co2.bio.gee") -> "co2.bio",
		Seq("co2.background", "co2.bio", "co2.fuel", "co2.cement") -> "co2.stilt",
		Seq("co.background", "co.fuel", "co.cement") -> "co.stilt",
		Seq("ch4.agriculture", "ch4.waste", "ch4.energy", "ch4.other_categories") -> "ch4.anthropogenic",
		Seq("ch4.wetlands", "ch4.soil_uptake", "ch4.wildfire", "ch4.other_natural") -> "ch4.natural",
		Seq("ch4.background", "ch4.anthropogenic", "ch4.natural") -> "ch4.stilt"
	)

	def makeRow(in: RawRow): JsObject =

		def fuelAssignments(gas: Tracer): Vector[Assignment] =
			for((fuel, sum) <- in.byFuelReport(None)(gas).toVector) yield s"$gas.fuel.$fuel" -> sum

		def otherFuelAssignment(gas: Tracer, specifier: String, label: String): Option[Assignment] =
			in.byFuelReport(Some(specifier))(gas).get(Fuel.otherfuel).map(s"$gas.fuel.$label" -> _)

		def categoryAssignment(gas: Tracer, categName: String, filter: Category => Boolean): Assignment =
			s"$gas.$categName" -> in.byCategoryReport(gas, filter)

		val cementAssignments = for((gas, sum) <- in.cementReport) yield s"$gas.cement" -> sum

		val co2FuelAssignments = fuelAssignments(Tracer.co2)
		val coFuelAssignments = fuelAssignments(Tracer.co)

		val initialAssignments: Vector[Assignment] =
			plainCopyCols.map(in.copy) ++
			renameCopyCols.map{
				case (from, to) => in.copy(to, from)
			} ++
			Seq(Tracer.co, Tracer.co2).flatMap{gas =>
				Seq(
					categoryAssignment(gas, "energy", _.isCoxEnergy),
					categoryAssignment(gas, "industry", _.isCoxIndustry),
					categoryAssignment(gas, "transport", _.isCoxTransport),
					categoryAssignment(gas, "residential", _.isCoxResidential),
					categoryAssignment(gas, "other_categories", _.isCoxOther)
				)
			} ++
			cementAssignments ++
			Seq(Tracer.co2, Tracer.co).flatMap(otherFuelAssignment(_, "solid_waste", "waste")) ++
			co2FuelAssignments ++
			coFuelAssignments ++
			Seq(
				categoryAssignment(Tracer.ch4, "agriculture", _.isCh4Agriculture),
				categoryAssignment(Tracer.ch4, "waste", _.isCh4Waste),
				categoryAssignment(Tracer.ch4, "energy", _.isCh4Energy),
				categoryAssignment(Tracer.ch4, "other_categories", _.isCh4Other),
				in.copy("ch4.background", "ch4ini"),
				in.copy("ch4.wildfire", "ch4fire"),
				in.copy("ch4.soil_uptake", "ch4uptake"),
				in.sum("ch4.wetlands", Seq("ch4wet", "ch4peat")),
				in.sum("ch4.other_natural", Seq("ch4soil", "ch4lakes", "ch4ocean")),
			).map{
				case (col, v) => col -> v * 1000 //ppm to ppb for CH4
			} :+
			"co2.fuel" -> co2FuelAssignments.map(_._2).sum :+
			"co.fuel" -> coFuelAssignments.map(_._2).sum :+
			in.sum("co2.bio.resp", vegetationTypes.map("resp" + _)) :+
			in.sum("co2.bio.gee", vegetationTypes.map("gee" + _)) :+
			in.sum("rn", Seq("rnini", "rn")) :+
			in.sum("rn.noah", Seq("rn_noahini", "rn_noah")) :+
			in.sum("rn.era", Seq("rn_eraini", "rn_era"))

		val numVals = rowPostProcessingSummations.foldLeft(initialAssignments.toMap):
			case (map, (summedVars, targetVar)) =>
				val sum = summedVars.map(map.apply).sum
				map + (targetVar -> sum)

		JsObject(numVals.map(_ -> JsNumber(_)) + (RawRow.StiltMetaColName -> JsString(in.stiltMeta)))

	end makeRow

end ResultRowMaker
