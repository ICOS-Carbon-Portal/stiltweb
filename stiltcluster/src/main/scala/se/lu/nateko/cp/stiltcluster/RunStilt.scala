package se.lu.nateko.cp.stiltcluster

import scala.sys.process._
import se.lu.nateko.cp.stiltcluster.StiltPosition.{latFmt, lonFmt}


object RunStilt:

	val constSiteID = "XXX"

	// The stilt CLI expects a slot looking like 2012010100 - the last two
	// digits being the hour in three hour offsets (00, 03, 06 etc)
	def buildStiltCommand(slot: StiltSlot): Seq[String] =
		val time = f"""${slot.year}%04d${slot.month}%02d${slot.day}%02d${slot.hour}%02d"""
		//s"stilt run ${constSiteID} ${slot.lat.toString} ${slot.lon.toString} ${slot.alt} ${time} ${time}"
		Seq(
			"stilt",
			"run",
			constSiteID,
			slot.lat.latFmt,
			slot.lon.lonFmt,
			slot.alt.toString,
			time,
			time
		)


	/**
	  * Run OS command with arguments
	  *
	  * @param cmd array of command and its arguments
	  * @return sequence of non-empty lines of processes stdout
	  */
	def runCommand(cmd: Seq[String]): Seq[String] =
		cmd.!!.split("\n").map(_.trim).filter(! _.isEmpty).toIndexedSeq


	def computeSlot(slot: StiltSlot): String =
		runCommand(buildStiltCommand(slot))(0)


end RunStilt
