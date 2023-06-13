package se.lu.nateko.cp.stiltweb.csv

import Gas.Gas
import Fuel.Fuel

sealed trait Variable{
	def name: String
}

sealed trait CategoryVariable extends Variable{
	def tracer: Gas
	def category: Category
}

case class PlainVariable(name: String) extends Variable
case class PlainCategoryVariable(name: String, tracer: Gas, category: Category, specifier: String) extends CategoryVariable{
	def isCement: Boolean = specifier == Variable.cementName
}
case class FuelInfoVariable(name: String, tracer: Gas, category: Category, fuel: Fuel, fuelSubtype: String) extends CategoryVariable


object Variable{

	val cementName = "cement"
	val fuelVarPattern = """^([^.]+)\.([^.]+)\.([^_]+)_(.+)$""".r
	val categoryVarPattern = """^([^.]+)\.([^.]+)\.(.+)$""".r

	def apply(name: String): Variable = name match{
		case fuelVarPattern(Gas(gas), cat, Fuel(fuel), subFuel) =>
			if(fuel == Fuel.OtherFuel){
				//need to use the full specifier instead of subFuel here to avoid duplicates
				val categoryVarPattern(_, _, specifier) = name : @unchecked
				FuelInfoVariable(name, gas, Category(cat), fuel, specifier)
			}
			else FuelInfoVariable(name, gas, Category(cat), fuel, subFuel)
		case categoryVarPattern(Gas(gas), cat, specifier) =>
			PlainCategoryVariable(name, gas, Category(cat), specifier)
		case _ =>
			PlainVariable(name)
	}

	val varNamesForPackaging = IndexedSeq(
		"co2.stilt",  "co2.background", "co2.bio", "co2.bio.gee", "co2.bio.resp",
		"co2.fuel", "co2.fuel.oil", "co2.fuel.coal", "co2.fuel.gas", "co2.fuel.bio", "co2.fuel.waste",
		"co2.energy", "co2.transport",  "co2.industry", "co2.residential", "co2.cement", "co2.other_categories"
	)
}

abstract class EnumerationWithDefault(defaultName: String) extends Enumeration{
	protected val defaultValue = Value(defaultName)

	def unapply(name: String): Option[Value] = Some(apply(name))
	def apply(name: String): Value = values.find(_.toString == name).getOrElse(defaultValue)
}

object Gas extends EnumerationWithDefault("othergas"){
	type Gas = Value
	val CO2 = Value("co2")
	val CO = Value("co")
	val OtherGas = defaultValue
}

object Fuel extends EnumerationWithDefault("otherfuel"){
	type Fuel = Value
	val Coal = Value("coal")
	val Gas = Value("gas")
	val Oil = Value("oil")
	val Bio = Value("bio")
	val OtherFuel = defaultValue
}

object CategoryNames{
	val energies = Set("1a1a","1a1bcr")
	val transports = Set("1a3b","1a3ce","1a3a+1c1","1a3d+1c2")
	val industries = Set("1a2+6cd","2a","2befg+3","2c")
	val residentials = Set("1a4")
	val others = Set("1b2abc","7a","4f")
}

case class Category(name: String){
	import CategoryNames._
	def isEnergy = energies.contains(name)
	def isTransport = transports.contains(name)
	def isIndustry = industries.contains(name)
	def isResidential = residentials.contains(name)
	def isOther = others.contains(name)
}
