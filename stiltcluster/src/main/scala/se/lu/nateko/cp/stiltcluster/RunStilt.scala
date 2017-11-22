package se.lu.nateko.cp.stiltcluster

import java.time.format.DateTimeFormatter
import java.time.LocalDate
import scala.sys.process._


object RunStilt {

	val constSiteID = "XXX"

	// The stilt CLI expects a slot looking like 2012010100 - the last two
	// digits being the hour in three hour offsets (00, 03, 06 etc)
	val slot_date_fmt = DateTimeFormatter.ofPattern("yyyyMMdd")

	def date_to_slot(date:LocalDate, hour:String = "00"): String = {
		s"${date.format(slot_date_fmt)}${hour}"
	}

	def build_calcslots_cmd(start: LocalDate, stop: LocalDate): String = {
		// Example: "stilt calcslots 2012010100 2012010309"
		s"stilt calcslots ${date_to_slot(start)} ${date_to_slot(stop)}"
	}

	def build_run_cmd(slot: StiltSlot): String = {
		val time = f"""${slot.year}%04d${slot.month}%02d${slot.day}%02d${slot.hour}%02d"""
		s"stilt run ${constSiteID} ${slot.lat.toString} ${slot.lon.toString} ${slot.alt} ${time} ${time}"
	}


	def run_cmd(cmd: String): Seq[String] = {
		// Run the command and return a sequence of the nonempty lines of stdout.
		cmd.!!.split("\n").map(_.trim).filter(! _.isEmpty)
	}

	def cmd_calcslots(start: LocalDate, stop: LocalDate): Seq[String] = {
		run_cmd(build_calcslots_cmd(start, stop))
	}

	def cmd_run(slot: StiltSlot): String = {
		run_cmd(build_run_cmd(slot))(0)
	}

}
