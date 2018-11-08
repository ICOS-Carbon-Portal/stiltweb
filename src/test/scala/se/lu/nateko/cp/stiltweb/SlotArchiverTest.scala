package se.lu.nateko.cp.stiltweb

import java.nio.file.Files

import org.scalatest._
import se.lu.nateko.cp.stiltcluster._
import scala.collection.JavaConverters.asScalaIteratorConverter



class SlotArchiverTest extends FunSuite with BeforeAndAfterAll{

	val tmp = Files.createTempDirectory("slotarchiver")

	override def afterAll(){
		Util.deleteDirRecursively(tmp)
	}

	test("sending/receiving slots") {

		val sla = new SlotArchiver(tmp, 180)
		val slot = StiltResultTest.sampleSlot

		def listDir = Files.walk(tmp).sorted().iterator.asScala
			.map(d => tmp.relativize(d).toString)
			.mkString("\n")
			.trim

		// The SlotArchiver have just started in an empty directory, it
		// should therefore not know about any slots.
		assert(sla.load(slot) === None)

		// And it's slot directory should be empty.
		assert(listDir === "slots")

		sla.save(StiltResultTest.sampleResult)
		// the slot should have been stored on disk.
		val exp2 = """slots
					|slots/46.55Nx007.98Ex00720
					|slots/46.55Nx007.98Ex00720/2012
					|slots/46.55Nx007.98Ex00720/2012/12
					|slots/46.55Nx007.98Ex00720/2012/12/2012x12x08x18
					|slots/46.55Nx007.98Ex00720/2012/12/2012x12x08x18/csv
					|slots/46.55Nx007.98Ex00720/2012/12/2012x12x08x18/foot
					|slots/46.55Nx007.98Ex00720/2012/12/2012x12x08x18/rdata
					|slots/46.55Nx007.98Ex00720/2012/12/2012x12x08x18/rdatafoot
					|slots/46.55Nx007.98Ex00720/2012/cache180_4096.txt""".stripMargin
		assert(listDir === exp2)

	}
}
