package se.lu.nateko.cp.stiltweb.csv

import org.scalatest.funsuite.AnyFunSuite

class VariablesTests extends AnyFunSuite:

	def testFuelVar(
		name: String, gas: Tracer, catName: String, fuel: Fuel, subFuel: String
	)(extraTests: Category => Unit = _ => ()) =

		val fiv = Variable(name) match
			case good: FuelInfoVariable => good
			case bad => fail(s"expected FuelInfoVariable, got $bad")
		assert(fiv.name === name)
		assert(fiv.tracer === gas)
		assert(fiv.category.name === catName)
		assert(fiv.fuel === fuel)
		assert(fiv.fuelSubtype === subFuel)
		extraTests(fiv.category)

	test("fuel info variable parsing"):
		testFuelVar("co2.1a3b.oil_heavy", Tracer.co2, "1a3b", Fuel.oil, "heavy"): cat =>
			assert(cat.isCoxTransport)

		testFuelVar("co.1a2+6cd.bio_solid", Tracer.co, "1a2+6cd", Fuel.bio, "solid"): cat =>
			assert(cat.isCoxIndustry)

		testFuelVar("ch4.1a2+6cd.coal_brown", Tracer.ch4, "1a2+6cd", Fuel.coal, "brown"): cat =>
			assert(cat.isCh4Other)

		testFuelVar("co2.1a1a.solid_waste", Tracer.co2, "1a1a", Fuel.otherfuel, "solid_waste"): cat =>
			assert(cat.isCoxEnergy)


	test("plain category variable parsing"):
		val othersCo2 = Variable("co2.2befg+3.others")match
			case good: PlainCategoryVariable => good
			case bad => fail(s"expected PlainCategoryVariable, got $bad")
		assert(othersCo2.category.name == "2befg+3")
		assert(othersCo2.tracer == Tracer.co2)
		assert(othersCo2.specifier == "others")


end VariablesTests
