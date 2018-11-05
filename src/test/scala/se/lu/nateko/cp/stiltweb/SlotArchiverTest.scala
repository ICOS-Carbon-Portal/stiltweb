package se.lu.nateko.cp.stiltweb

import java.nio.file.{ Files, Path }

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest._
import se.lu.nateko.cp.stiltcluster._



class SlotArchiverTest extends TestKit(ActorSystem()) with FunSuiteLike with ImplicitSender {

	def afterAll = system.terminate()

	/* When tracing is enabled we create "trace.log" files. We want to ignore those
	 * when comparing directory trees.
	 */
	def listDirTreeNoTrace (dir: Path) = {
		Util.listDirTree(dir).split("\n").filterNot(_.contains("trace.log")).mkString("\n")
	}

	test("sending/receiving slots") {

		val tmp = Files.createTempDirectory("slotarchiver")
		val sla = system.actorOf(Props(new SlotArchiver(tmp, 180)), name="slotarchiver")
		val slot = StiltResultTest.sampleSlot

		// The SlotArchiver have just started in an empty directory, it
		// should therefore not know about any slots.
		sla ! RequestSingleSlot(slot)
		expectMsg(SlotUnAvailable(slot))

		// And it's slot directory should be empty.
		val exp1 = """.
					 |./slots""".stripMargin
		assert(listDirTreeNoTrace(tmp) == exp1)

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
							 |./slots/46.55Nx007.98Ex00720/2012/12/2012x12x08x18/csv
							 |./slots/46.55Nx007.98Ex00720/2012/12/2012x12x08x18/foot
							 |./slots/46.55Nx007.98Ex00720/2012/12/2012x12x08x18/rdata
							 |./slots/46.55Nx007.98Ex00720/2012/12/2012x12x08x18/rdatafoot
							 |./slots/46.55Nx007.98Ex00720/2012/cache180_4096.txt""".stripMargin
				assert(listDirTreeNoTrace(tmp) == exp2)

				// Now we want to link that slot to a job directory.
				val dst = Files.createTempDirectory("linkedslot")
				local.link(dst)
				val exp3 = """.
								|./Footprints
								|./Footprints/XXX
								|./Footprints/XXX/foot2012x12x08x18x46.55Nx007.98Ex00720_aggreg.nc
								|./Footprints/XXX/.RDatafoot2012x12x08x18x46.55Nx007.98Ex00720
								|./RData
								|./RData/XXX
								|./RData/XXX/.RData2012x12x08x18x46.55Nx007.98Ex00720
								|./Results
								|./Results/XXX
								|./Results/XXX/stiltresult2012x46.55Nx007.98Ex00720_1.csv""".stripMargin
				assert(listDirTreeNoTrace(dst) == exp3)

				Util.deleteTmpDirTree(tmp)
				Util.deleteTmpDirTree(dst)
			}
	}
}
