package se.lu.nateko.cp.stiltweb

import se.lu.nateko.cp.stiltcluster.Job
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import scala.sys.process._


object RunStilt {
	// The stilt CLI expects a slot looking like 2012010100 - the last two
	// digits being the hour in three hour offsets (00, 03, 06 etc)
	val slot_date_fmt = DateTimeFormatter.ofPattern("yyyyMMdd")

	def date_to_slot(date:LocalDate, slot:String = "00"): String = {
		s"${date.format(slot_date_fmt)}${slot}"
	}

	def job_to_calcslots_cmd(job:Job, cmd:String = "stilt"): String = {
		// Example: "stilt calcslots 2012010100 2012010309"
		s"stilt calcslots ${date_to_slot(job.start)} ${date_to_slot(job.stop)}"
	}

	def run_cmd(cmd: String): Seq[String] = {
		// Run the command return a sequence of the nonempty lines of stdout
		cmd.!!.split("\n").map(_.trim).filter(! _.isEmpty)
	}

	def cmd_calcslots(job: Job): Seq[String] = {
		run_cmd(job_to_calcslots_cmd(job))
	}

	// def job_to_run_cmd(job:Job, cmd:String = "stilt"): String = {
	//	val start = date_to_slot(job.start)
	//	val stop = date_to_slot(job.stop)

	//	/* Example:
	//	 * stilt calcslots HTM 56.10 13.42 150 2012061500 2012061500
	//	 */
	//	val args = s"$job.siteId $job.lat.toString $job.lon.toString $start $stop"
	//	"stilt calcslots " + args
	// }

	// def run_calcslots_cmd(cmd: String): Seq[String] = {

	// }

}
