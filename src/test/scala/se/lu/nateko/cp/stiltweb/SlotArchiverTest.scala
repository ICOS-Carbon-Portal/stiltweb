package se.lu.nateko.cp.stiltweb

import akka.actor.{ ActorSystem, Props }
import akka.testkit.{ TestKit, ImplicitSender }
import java.nio.file.{ Files, Path }
import se.lu.nateko.cp.stiltcluster._
import org.scalatest._



object SlotArchiverTest {
	import scala.sys.process._

	def listDirTree(dir: Path): String = {
		val cmd = Seq("bash", "-c", s"cd '${dir}' && find | sort")
		cmd.!!
	}

	def deleteTmpDirTree(dir: Path): Unit = {
		assert(dir.getParent.toString == "/tmp")
		val cmd = Seq("rm", "-rf", "--", dir.toString)
		cmd.!!
	}

}



class SlotArchiverTest extends TestKit(ActorSystem()) with FunSuiteLike with ImplicitSender {

	def afterAll = system.terminate()

	test("sending/receiving slots") {
		import SlotArchiverTest._

		val tmp = Files.createTempDirectory("slotarchiver")
		val sla = system.actorOf(Props(new SlotArchiver(tmp)), name="slotarchiver")
		val slot = StiltResultTest.sampleSlot

		// The SlotArchiver have just started in an empty directory, it
		// should therefore not know about any slots.
		sla ! RequestSingleSlot(slot)
		expectMsg(SlotUnAvailable(slot))

		// And it's slot directory should be empty.
		val exp1 = """.
					 |./slots
					 |""".stripMargin
		assert(listDirTree(tmp) == exp1)

		sla ! SlotCalculated(StiltResultTest.sampleResult)
		expectMsgPF() {
			case SlotAvailable(local) =>
				// Since the SlotArchiver now responds with a slot, that
				// slot should have been stored on disk.
				val exp2 = """.
							 |./slots
							 |./slots/46.55Nx007.98Ex00720
							 |./slots/46.55Nx007.98Ex00720/2012
							 |./slots/46.55Nx007.98Ex00720/2012/12
							 |./slots/46.55Nx007.98Ex00720/2012/12/2012x12x08x18
							 |./slots/46.55Nx007.98Ex00720/2012/12/2012x12x08x18/foot
							 |./slots/46.55Nx007.98Ex00720/2012/12/2012x12x08x18/rdata
							 |./slots/46.55Nx007.98Ex00720/2012/12/2012x12x08x18/rdatafoot
							 |""".stripMargin
				assert(listDirTree(tmp) == exp2)

				// Now we want to link that slot to a job directory.
				val dst = Files.createTempDirectory("linkedslot")
				local.link(dst)
				val exp3 = """.
								|./Footprints
								|./Footprints/XXX
								|./Footprints/XXX/2012
								|./Footprints/XXX/2012/foot2012x12x08x18x46.55Nx007.98Ex00720_aggreg.nc
								|./Footprints/XXX/2012/.RDatafoot2012x12x08x18x46.55Nx007.98Ex00720
								|./RData
								|./RData/XXX
								|./RData/XXX/2012
								|./RData/XXX/2012/.RData2012x12x08x18x46.55Nx007.98Ex00720
								|""".stripMargin
				assert(listDirTree(dst) == exp3)

				deleteTmpDirTree(tmp)
				deleteTmpDirTree(dst)
			}
	}
}
