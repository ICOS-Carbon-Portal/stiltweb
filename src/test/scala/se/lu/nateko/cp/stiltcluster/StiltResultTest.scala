package se.lu.nateko.cp.stiltcluster.test

import java.nio.file.{Files, Paths}

import org.scalatest.FunSuite
import se.lu.nateko.cp.stiltcluster.{StiltPosition, StiltResult, StiltSlot, StiltTime, StiltResultFileType}


class StiltResultTest extends FunSuite {
	val tim = StiltTime(2012, 12, 1, 0)
	val pos = StiltPosition(56.10, -13.42, 150)
	val slt = StiltSlot(tim, pos)


	test("Read output directory") {
		val f = "./Footprints/XXX/2012/foot2012x12x08x18x46.55Nx007.98Ex00720_aggreg.nc"
		val (prefix, slot, suffix) = StiltSlot.ofFilename(f)
		assert(prefix == "./Footprints/XXX/2012/foot")
		assert(suffix == "_aggreg.nc")

		val p = Paths.get(getClass.getResource("/stilt-sample-run/output").getFile)
		assert(Files.exists(p))

		val g = p.resolve("Footprints/XXX/2012/stiltresult2012x46.55Nx007.98Ex00720_1.csv")
		assert(Files.exists(g))

		val h = p.resolve("Footprints/XXX/2012/.RDatastiltresult2012x46.55Nx007.98Ex00720_1")
		assert(Files.exists(h))

		val r = StiltResult(slot, p)
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
