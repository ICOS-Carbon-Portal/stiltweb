package se.lu.nateko.cp.stiltweb

import akka.actor.{ ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestActors, TestKit }
import java.nio.file.Files
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }
import se.lu.nateko.cp.stiltcluster.{ RequestSingleSlot, SlotArchiver, SlotUnAvailable, StiltSlot }


class SlotArchiverSpec() extends TestKit(ActorSystem("SlotArchiverSpec")) with ImplicitSender
		with WordSpecLike with Matchers with BeforeAndAfterAll {

	override def afterAll {
		TestKit.shutdownActorSystem(system)
	}

	"A SlotArchive actor" must {

		"Respond with slot unavailable" in {
			val tmp = Files.createTempDirectory("slotarchiver")
			val sla = system.actorOf(Props(new SlotArchiver(tmp)), name="slotarchiver")
			val slot = StiltResultTest.sampleSlot
			sla ! RequestSingleSlot(slot)
			expectMsg(SlotUnAvailable(slot))
		}
	}

}
