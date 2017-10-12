package se.lu.nateko.cp.stiltweb

import se.lu.nateko.cp.stiltcluster.Job
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import scala.sys.process._


object RunStilt {
	// The stilt CLI expects a slot looking like 2012010100 - the last two
	// digits being the hour in three hour offsets (00, 03, 06 etc)
	val slot_date_fmt = DateTimeFormatter.ofPattern("yyyyMMdd")

	def date_to_slot(date:LocalDate, hour:String = "00"): String = {
		s"${date.format(slot_date_fmt)}${hour}"
	}

	def build_calcslots_cmd(job:Job): String = {
		// Example: "stilt calcslots 2012010100 2012010309"
		s"stilt calcslots ${date_to_slot(job.start)} ${date_to_slot(job.stop)}"
	}

	def build_run_cmd(job: Job, slot: String): String = {
		s"stilt run ${job.siteId} ${job.lat.toString} ${job.lon.toString} ${job.alt} $slot $slot"
	}


	def run_cmd(cmd: String): Seq[String] = {
		// Run the command return a sequence of the nonempty lines of stdout
		cmd.!!.split("\n").map(_.trim).filter(! _.isEmpty)
	}

	def cmd_calcslots(job: Job): Seq[String] = {
		run_cmd(build_calcslots_cmd(job))
	}

	def cmd_run(job: Job, slot: String): String = {
		run_cmd(build_run_cmd(job, slot))(0)
	}

}
