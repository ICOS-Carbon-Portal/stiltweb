package se.lu.nateko.cp.stiltweb.csv

sealed trait Variable{
	def name: String
}

sealed trait CategoryVariable extends Variable{
	def tracer: Tracer
	def category: Category
}

case class PlainVariable(name: String) extends Variable
case class PlainCategoryVariable(name: String, tracer: Tracer, category: Category, specifier: String) extends CategoryVariable{
	def isCement: Boolean = specifier == Variable.cementName
}
case class FuelInfoVariable(name: String, tracer: Tracer, category: Category, fuel: Fuel, fuelSubtype: String) extends CategoryVariable


object Variable{

	val cementName = "cement"
	val fuelVarPattern = """^([^.]+)\.([^.]+)\.([^_]+)_(.+)$""".r
	val categoryVarPattern = """^([^.]+)\.([^.]+)\.(.+)$""".r

	def apply(name: String): Variable = name match{
		case fuelVarPattern(Tracer(gas), cat, Fuel(fuel), subFuel) =>
			if(fuel == Fuel.otherfuel){
				//need to use the full specifier instead of subFuel here to avoid duplicates
				val categoryVarPattern(_, _, specifier) = name : @unchecked
				FuelInfoVariable(name, gas, Category(cat), fuel, specifier)
			}
			else FuelInfoVariable(name, gas, Category(cat), fuel, subFuel)
		case categoryVarPattern(Tracer(gas), cat, specifier) =>
			PlainCategoryVariable(name, gas, Category(cat), specifier)
		case _ =>
			PlainVariable(name)
	}

	val varNamesForPackaging = IndexedSeq(
		"co2.stilt",  "co2.background", "co2.bio", "co2.bio.gee", "co2.bio.resp",
		"co2.fuel", "co2.fuel.oil", "co2.fuel.coal", "co2.fuel.gas", "co2.fuel.bio", "co2.fuel.waste",
		"co2.energy", "co2.transport",  "co2.industry", "co2.residential", "co2.cement", "co2.other_categories",
		"ch4.stilt", "ch4.background", "ch4.anthropogenic", "ch4.agriculture", "ch4.waste", "ch4.energy", "ch4.other_categories",
		"ch4.natural", "ch4.wetlands", "ch4.soil_uptake", "ch4.wildfire", "ch4.other_natural"
	)
}

enum Tracer:
	case co2, co, ch4, othergas

object Tracer:
	def unapply(name: String): Option[Tracer] = Some:
		try Tracer.valueOf(name)
		catch case _: Throwable => Tracer.othergas

enum Fuel:
	case coal, gas, oil, bio, otherfuel

object Fuel:
	def unapply(name: String): Option[Fuel] = Some:
		try Fuel.valueOf(name)
		catch case _: Throwable => Fuel.otherfuel

object CategoryNames:
	// "cox" stands for "carbon oxide", that is, CO2 and CO
	val ch4_agricultures = Set("4a", "4b", "4c", "4f")
	val cox_energies = Set("1a1a","1a1bcr")
	val ch4_energies = Set("1a1a", "1a1bcr", "1b1", "1b2b")
	val cox_transports = Set("1a3b", "1a3ce", "1a3a+1c1", "1a3d+1c2")
	val cox_industries = Set("1a2+6cd", "2a", "2befg+3", "2c")
	val cox_residentials = Set("1a4")
	val ch4_wastes = Set("6a", "6b")
	val cox_others = Set("1b2abc", "7a", "4f")
	val ch4_others = Set("1a3b", "1a3ce", "1a3a+1c1", "1a3d+1c2", "1a2+6cd", "2befg+3", "2c", "1a4", "1b2ac", "7a")
	//val ch4_other_naturals = Set("ch4soil","ch4lakes","ch4ocean")
	//val ch4_wetlands = Set("ch4wet", "ch4peat")
	//val ch4_wildfires = Set("ch4fire")
	//val ch4_soil_uptakes = Set("ch4uptake")

case class Category(name: String):
	import CategoryNames._
	def isCoxEnergy = cox_energies.contains(name)
	def isCh4Energy = ch4_energies.contains(name)
	def isCoxTransport = cox_transports.contains(name)
	def isCoxIndustry = cox_industries.contains(name)
	def isCoxResidential = cox_residentials.contains(name)
	def isCoxOther = cox_others.contains(name)
	def isCh4Other = ch4_others.contains(name)
