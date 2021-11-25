package se.lu.nateko.cp.stiltweb

import java.nio.file.Files
import java.time.LocalDate

import scala.jdk.CollectionConverters.IteratorHasAsScala

import se.lu.nateko.cp.stiltcluster._
import se.lu.nateko.cp.stiltweb.state.Archiver
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.BeforeAndAfterAll


class ArchiverTest extends AnyFunSuite with BeforeAndAfterAll{

	val tmp = Files.createTempDirectory("slotarchiver")

	override def afterAll(): Unit = {
		Util.deleteDirRecursively(tmp)
	}

	test("sending/receiving slots") {

		val sla = new Archiver(tmp, 180)
		val slot = StiltResultTest.sampleSlot

		def listDir = Files.walk(tmp).sorted().iterator().asScala
			.map(d => tmp.relativize(d).toString)
			.mkString("\n")
			.trim

		// The Archiver initialized with an empty directory, it
		// should therefore not know about any slots.
		assert(sla.load(slot) === None)

		// And it's slot directory should be empty.
		assert(listDir === "jobs\nslots\nstations")

		sla.save(StiltResultTest.sampleResult)
		// the slot should have been stored on disk.
		val exp2 = """jobs
					|slots
					|slots/46.55Nx007.98Ex00720
					|slots/46.55Nx007.98Ex00720/2012
					|slots/46.55Nx007.98Ex00720/2012/12
					|slots/46.55Nx007.98Ex00720/2012/12/2012x12x08x18
					|slots/46.55Nx007.98Ex00720/2012/12/2012x12x08x18/csv
					|slots/46.55Nx007.98Ex00720/2012/12/2012x12x08x18/foot
					|slots/46.55Nx007.98Ex00720/2012/12/2012x12x08x18/rdata
					|slots/46.55Nx007.98Ex00720/2012/12/2012x12x08x18/rdatafoot
					|slots/46.55Nx007.98Ex00720/2012/cache180_4096.txt
					|stations""".stripMargin
		assert(listDir === exp2)

	}

	test("two days have 16 3-hour slots"){
		val pos = StiltPosition(50.0, 10.0, 100)
		val job = Job("station", pos.lat, pos.lon, pos.alt, LocalDate.of(2012, 12, 7), LocalDate.of(2012, 12, 8), "username")
		val sla = new Archiver(tmp, 180)
		val slots = sla.calculateSlots(job)

		assert(slots.size === 16)
		val s0 = slots.head
		assert(s0.pos === pos)
		assert(s0.time === StiltTime(2012, 12, 7, 0))
		assert(slots.last.time === StiltTime(2012, 12, 8, 21))
	}
}
