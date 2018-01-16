package se.lu.nateko.cp.stiltweb

import java.nio.file.{Files, Paths}

import org.scalatest.FunSuite
import se.lu.nateko.cp.stiltcluster.{StiltResult, StiltSlot, StiltResultFileType}


object StiltResultTest {
	val sampleFileName = "./Footprints/XXX/foot2012x12x08x18x46.55Nx007.98Ex00720_aggreg.nc"
	val (_prefix, sampleSlot, _suffix) = StiltSlot.ofFilename(sampleFileName)

	assert(_prefix == "./Footprints/XXX/foot")
	assert(_suffix == "_aggreg.nc")

	val sampleSlotDir = Paths.get(getClass.getResource("/stilt-sample-run/output").getFile)
	assert(Files.exists(sampleSlotDir))

	val sampleResult = StiltResult(sampleSlot, sampleSlotDir)
}


class StiltResultTest extends FunSuite {

	import StiltResultTest.{sampleSlotDir, sampleResult}

	test("Read output directory") {
		val g = sampleSlotDir.resolve("Footprints/XXX/stiltresult2012x46.55Nx007.98Ex00720_1.csv")
		assert(Files.exists(g))

		val h = sampleSlotDir.resolve("Footprints/XXX/.RDatastiltresult2012x46.55Nx007.98Ex00720_1")
		assert(Files.exists(h))

		val r = sampleResult
		assert(r.slot.year == 2012)
		assert(r.slot.month == 12)
		assert(r.slot.day == 8)
		assert(r.slot.hour == 18)
		assert(r.slot.lat == 46.55)
		assert(r.slot.lon == 7.98)
		assert(r.slot.alt == 720)

		assert(r.files.length == 3)
		val foot = r.files.find(rf => rf.typ == StiltResultFileType.Foot).get
		assert(foot.data.length == 29827)
		assert(foot.slot == r.slot)

		val rdata = r.files.find(rf => rf.typ == StiltResultFileType.RData).get
		assert(rdata.data.length == 2293810)
		assert(rdata.slot == r.slot)

		val rdatafoot = r.files.find(rf => rf.typ == StiltResultFileType.RDataFoot).get
		assert(rdatafoot.data.length == 13327)
		assert(rdatafoot.slot == r.slot)
	}
}
