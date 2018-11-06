package se.lu.nateko.cp.stiltweb

import java.nio.file.Files

import org.scalatest._
import se.lu.nateko.cp.stiltcluster._



class SlotArchiverTest extends FunSuite {

	test("sending/receiving slots") {

		val tmp = Files.createTempDirectory("slotarchiver")
		val sla = new SlotArchiver(tmp, 180)
		val slot = StiltResultTest.sampleSlot

		// The SlotArchiver have just started in an empty directory, it
		// should therefore not know about any slots.
		assert(sla.load(slot) === None)

		// And it's slot directory should be empty.
		val exp1 = """.
					 |./slots""".stripMargin
		assert(Util.listDirTree(tmp) == exp1)

		sla.save(StiltResultTest.sampleResult)
		// the slot should have been stored on disk.
		val exp2 = """.
					 |./slots
					 |./slots/46.55Nx007.98Ex00720
					 |./slots/46.55Nx007.98Ex00720/2012
					 |./slots/46.55Nx007.98Ex00720/2012/12
					 |./slots/46.55Nx007.98Ex00720/2012/12/2012x12x08x18
					 |./slots/46.55Nx007.98Ex00720/2012/12/2012x12x08x18/csv
					 |./slots/46.55Nx007.98Ex00720/2012/12/2012x12x08x18/foot
					 |./slots/46.55Nx007.98Ex00720/2012/12/2012x12x08x18/rdata
					 |./slots/46.55Nx007.98Ex00720/2012/12/2012x12x08x18/rdatafoot
					 |./slots/46.55Nx007.98Ex00720/2012/cache180_4096.txt""".stripMargin
		assert(Util.listDirTree(tmp) == exp2)

		Util.deleteTmpDirTree(tmp)
	}
}
