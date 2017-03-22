package se.lu.nateko.cp.stiltcluster

import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Cancellable
import akka.actor.Props

import scala.collection.immutable.Seq
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

class Worker(conf: StiltEnv, master: ActorRef) extends Actor{

	import Worker._

	private var stiltProc: ProcessRunner = null
	private var logsProc: ProcessRunner = null
	private var stiltRun: JobRun = null

	private var status = JobStatus.init(null)
	private var pulse: Cancellable = null

	private val log = context.system.log
	implicit val exeCtxt =  context.system.dispatcher

	override def preStart(): Unit = {
	}

	/* Start our two subprocesses. One will run the STILT simulation and the other
	 * will monitor the STILT log files. In each case we can specify a debug
	 * command to be run instead. */
	private def startStiltSubProcesses(): Unit = {
		stiltProc = conf.debugRun match {
			case Some(dbgCmd) => {
				val cmd = debugCommand(dbgCmd, stiltRun)
				log.info(s"Starting new job by running debug script as '${cmd.mkString(" ")}'")
				new ProcessRunner(cmd, conf.logSizeLimit)
			}
			case None =>
				new ProcessRunner(stiltCommand(stiltRun, conf), conf.logSizeLimit)
		}

		logsProc = conf.debugLog match {
			case Some(logDbgCmd) =>
				val cmd = debugCommand(logDbgCmd, stiltRun)
				log.info(s"Faking log output from new job by running ${cmd.mkString(" ")}'")
				new ProcessRunner(cmd, conf.logSizeLimit)
			case None =>
				new ProcessRunner(logWatchCommand(stiltRun, conf), conf.logSizeLimit)
		}
	}

	def receive = {
		case run: JobRun =>

			stiltRun = run

			try{
				startStiltSubProcesses()

				updateStatus()
				master ! status

				log.info("STARTED JOB RUN " + run)

				pulse = context.system.scheduler.schedule(2 seconds, 2 seconds, self, Tick)
				context become calculating
			}catch{
				case err: Throwable =>

					log.warning("FAILED STARTING JOB RUN " + stiltRun)
					log.warning("... " + err.getMessage)

					status = JobStatus(
						id = run.job.id,
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
		case Tick =>

			val oldStatus = status
			updateStatus()

			if(status != oldStatus){
				master ! status
				if(status.exitValue.isDefined) {
					log.info("FINISHED JOB RUN " + stiltRun)
					resetWorker()
				}
			}

		case CancelJob(id) =>
			if(id == stiltRun.job.id){
				log.info(s"Worker cancelling job ${id}")
				stiltProc.destroyForcibly()
				logsProc.destroyForcibly()
				removeJobDirectories(conf, stiltRun.job.id) match {
					case Failure(err) => log.error(err.getMessage)
					case _ =>
				}
				master ! JobCanceled(id)
				resetWorker()
			} else {
				log.error(s"Cannot cancel job ${id} - since I'm not the owner (I own job ${stiltRun.job.id})")
			}
	}

	private def updateStatus(): Unit = if(status.exitValue.isEmpty){
		status = JobStatus(
			id = stiltRun.job.id,
			exitValue = stiltProc.exitValue(),
			output = stiltProc.outputLines(),
			logs = logsProc.outputLines(),
			errors = stiltProc.errorLines() ++ logsProc.errorLines()
		)
	}

	private def resetWorker(): Unit = {
		pulse.cancel()
		stiltProc = null; logsProc = null; stiltRun = null; pulse = null
		context.unbecome()
	}
}


object Worker{

	def props(env: StiltEnv, master: ActorRef) = Props(classOf[Worker], env, master)

	private val Tick = "Tick"

	private def removeJobDirectories(env: StiltEnv, jobId: String ): Try[Unit] = {
		val logsFolder = s"${env.mainFolder}/${jobId}"
		val outputFolder = s"${env.mainFolder}/Output/${jobId}"
		val cmd = Seq("docker", "exec", env.containerName, "/bin/bash", "-c",
					  s"rm -rf '${logsFolder}' '${outputFolder}'")

		Try{
			val proc = new ProcessBuilder(cmd: _*).start()
			val done: Boolean = proc.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)
			if(done){
				val exitCode: Int = proc.exitValue
				if(exitCode == 0) Success(())
				else {
					val errStream = proc.getErrorStream
					val errBytes = Array.ofDim[Byte](errStream.available())
					errStream.read(errBytes)
					errStream.close()
					val errMsg = new String(errBytes, "UTF-8")
					Failure(new Exception(s"Exit code was $exitCode, the stderr was\n$errMsg"))
				}
			} else Failure(new Exception(s"Folder removal timed out"))
		}.flatten
	}

	def debugCommand(script: String, run: JobRun): Seq[String] = {
		val job = run.job
		Seq(
			s"${script}",
			s"${job.siteId} ${geoStr(job.lat)} ${geoStr(job.lon)} ${job.alt} " +
			s"${dateStr(job.start)} ${dateStr(job.stop)} ${run.job.id} ${run.parallelism}"
		)
	}

	def stiltCommand(run: JobRun, env: StiltEnv): Seq[String] = {
		//docker exec stilt_stilt_1 /bin/bash -c \
		// '/opt/STILT_modelling/start.stilt.sh HTM 56.10 13.42 150 20120615 20120616 testrun01 6'
		val job = run.job
		val script = new File(env.mainFolder, env.launchScript).getAbsolutePath

		Seq(
			"docker", "exec", env.containerName, "/bin/bash", "-c",
			s"$script ${job.siteId} ${geoStr(job.lat)} ${geoStr(job.lon)} ${job.alt} " +
			s"${dateStr(job.start)} ${dateStr(job.stop)} ${run.job.id} ${run.parallelism}"
		)
	}

	def logWatchCommand(run: JobRun, env: StiltEnv): Seq[String] = {
		// cd /opt/STILT_modelling/testrun01 && tail -F stilt_01.HTM2012job_1.log -F stilt_02.HTM2012job_1.log ...
		val job = run.job

		val logFiles = s"./${run.job.id}/prepare_input.${job.siteId}${run.job.id}.log" +:
			(1 to run.parallelism).map{ n =>
				val par = n.formatted("%02d")
				s"./${run.job.id}/stilt_${par}.${job.siteId}${job.start.getYear}${run.job.id}.log"
			}
		val logList = logFiles.mkString("-F ", " -F ", "")

		Seq(
			"docker", "exec", env.containerName, "/bin/bash", "-c",
			s"sleep 1 && cd ${env.mainFolder} && tail $logList"
		)
	}

	private val df = DateTimeFormatter.ofPattern("yyyyMMdd")
	private def dateStr(date: LocalDate) = date.format(df)
	private def geoStr(latOrLon: Double) = latOrLon.formatted("%.2f")
}
