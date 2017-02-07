package se.lu.nateko.cp.stiltcluster

import akka.actor.Actor
import akka.actor.Props
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.io.File

class Worker(conf: StiltEnv) extends Actor{

	import Worker._

	private var stiltProc: ProcessRunner = null
	private var logsProc: ProcessRunner = null
	private var running: StiltJob = null

	def receive = {
		case job: StiltJob =>
			stiltProc = new ProcessRunner(stiltCommand(job, conf), conf.logSizeLimit)
			logsProc = new ProcessRunner(logWatchCommand(job, conf), conf.logSizeLimit)
			running = job
			sender() ! getStatus
			context become calculating
	}

	def calculating: Receive = {
		case CancelJob(id) if(id == running.jobId) =>
			stiltProc.kill()
			logsProc.kill()
	}

	private def getStatus = JobStatus(
		id = running.jobId,
		output = stiltProc.outputLines().toIndexedSeq,
		logs = logsProc.outputLines().toIndexedSeq,
		errors = (stiltProc.errorLines() ++ logsProc.errorLines()).toIndexedSeq
	)
	//docker exec stilt_stilt_1 /bin/bash -c "/opt/STILT_modelling/start.stilt.sh HTM 20120615 20120616 testrun01 6
	// /opt/STILT_modelling/testrun01/stilt_01.HTM2012testrun01.log
}


object Worker{

	def props(env: StiltEnv) = Props(classOf[Worker], env)

	def stiltCommand(job: StiltJob, env: StiltEnv): String = {

		val script = new File(env.mainFolder, env.launchScript).getAbsolutePath

		s"docker exec ${env.containerName} /bin/bash -c '$script ${job.siteId} " +
			s"${job.start.format(df)} ${job.start.format(df)} ${job.parallelism}'"
	}

	def logWatchCommand(job: StiltJob, env: StiltEnv): String = {
		val jobFolder = new File(env.mainFolder, job.jobId).getAbsolutePath

		val logFiles = (1 to job.parallelism).map{ n =>
			val par = job.parallelism.formatted("%02d")
			s"stilt_${par}.${job.siteId}${job.start.getYear}${job.jobId}.log"
		}
		val logList = logFiles.mkString("-F ", " -F ", "")

		s"docker exec ${env.containerName} /bin/bash -c 'tail $logList'"
	}

	private val df = DateTimeFormatter.ofPattern("yyyyMMdd")
	private def dateStr(date: LocalDate) = date.format(df)
}
