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
	private var stiltRun: JobRun = null

	private var status = JobStatus.init(null)

	private val log = context.system.log

	def receive = {
		case run: JobRun =>
			stiltRun = run
			try{
				stiltProc = new ProcessRunner(stiltCommand(run, conf), conf.logSizeLimit)
				logsProc = new ProcessRunner(logWatchCommand(run, conf), conf.logSizeLimit)
				updateStatus()
				sender() ! status
				log.info("STARTED JOB RUN " + run)
				context become calculating
			}catch{
				case err: Throwable => status = JobStatus(
						id = run.runId,
						exitValue = Some(1),
						output = Nil,
						logs = Nil,
						errors = Seq(err.getMessage)
					)
					if(stiltProc != null) stiltProc.destroyForcibly()
					if(logsProc != null) logsProc.destroyForcibly()
			}
	}

	def calculating: Receive = {
		case GetStatus =>
			updateStatus()
			sender() ! status
			if(status.exitValue.isDefined) resetWorker()

		case CancelJob(id) if(id == stiltRun.runId) =>
			stiltProc.destroyForcibly()
			logsProc.destroyForcibly()
			updateStatus()
			sender() ! JobCanceled(status)
			resetWorker()
	}

	private def updateStatus(): Unit = if(status.exitValue.isEmpty){
		status = JobStatus(
			id = stiltRun.runId,
			exitValue = stiltProc.exitValue(),
			output = stiltProc.outputLines(),
			logs = logsProc.outputLines(),
			errors = stiltProc.errorLines() ++ logsProc.errorLines()
		)
	}

	private def resetWorker(): Unit = {
		stiltProc = null; logsProc = null; stiltRun = null
		context.unbecome()
	}
}


object Worker{

	def props(env: StiltEnv) = Props(classOf[Worker], env)

	def stiltCommand(run: JobRun, env: StiltEnv): Seq[String] = {
		//docker exec stilt_stilt_1 /bin/bash -c \
		// '/opt/STILT_modelling/start.stilt.sh HTM 56.10 13.42 150 20120615 20120616 testrun01 6'
		val job = run.job
		val script = new File(env.mainFolder, env.launchScript).getAbsolutePath

		Seq(
			"docker", "exec", env.containerName, "/bin/bash", "-c",
			s"$script ${job.siteId} ${geoStr(job.lat)} ${geoStr(job.lon)} ${job.alt} " +
			s"${dateStr(job.start)} ${dateStr(job.stop)} ${run.runId} ${run.parallelism}"
		)
	}

	def logWatchCommand(run: JobRun, env: StiltEnv): Seq[String] = {
		// cd /opt/STILT_modelling/testrun01 && tail -F stilt_01.HTM2012job_1.log -F stilt_02.HTM2012job_1.log ...
		val job = run.job

		val logFiles = (1 to run.parallelism).map{ n =>
			val par = n.formatted("%02d")
			s"./${run.runId}/stilt_${par}.${job.siteId}${job.start.getYear}${run.runId}.log"
		}
		val logList = logFiles.mkString("-F ", " -F ", "")

		Seq(
			"docker", "exec", env.containerName, "/bin/bash", "-c",
			s"cd ${env.mainFolder} && tail $logList"
		)
	}

	private val df = DateTimeFormatter.ofPattern("yyyyMMdd")
	private def dateStr(date: LocalDate) = date.format(df)
	private def geoStr(latOrLon: Double) = latOrLon.formatted("%.2f")
}
