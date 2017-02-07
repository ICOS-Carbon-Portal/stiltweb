package se.lu.nateko.cp.stiltcluster

import java.io.InputStream

class ProcessRunner(command: String, logSizeBound: Int) {

	private val process = new ProcessBuilder(command).start()

	private val outStream = process.getInputStream
	private val errStream = process.getErrorStream
	private var isClosed = false

	private val outLog = new LogLineQueue(logSizeBound)
	private val errLog = new LogLineQueue(logSizeBound)

	def exitValue(): Option[Int] = if(process.isAlive) None else {
		closeStreams()
		Some(process.exitValue)
	}

	def kill(): Unit = if(process.isAlive){
		process.destroyForcibly()
		process.waitFor()
		closeStreams()
	}

	def outputLines(): Seq[String] = {tick(); outLog.lines}
	def errorLines(): Seq[String] = {tick(); errLog.lines}

	private def tick(): Unit = {
		if(process.isAlive) readStreams() else closeStreams()
	}

	private def closeStreams(): Unit = if(!isClosed){
		readStreams()
		outStream.close()
		errStream.close()
		outLog.flush()
		errLog.flush()
		isClosed = true
	}

	private def readStreams(): Unit = {
		ProcessRunner.readAvailable(outLog, outStream)
		ProcessRunner.readAvailable(errLog, errStream)
	}
}

object ProcessRunner{

	def readAvailable(target: LogLineQueue, from: InputStream): Unit = {
		val available = from.available()

		if(available > 0){
			val portion = Array.ofDim[Byte](available)
			from.read(portion)
			target.append(portion)
		}
	}

}
