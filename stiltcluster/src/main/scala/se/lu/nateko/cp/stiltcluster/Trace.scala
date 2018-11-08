package se.lu.nateko.cp.stiltcluster

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption._
import java.time.LocalDateTime


trait Trace {

	protected def traceFile: Path

	def trace(msg: String) = {
		val s = s"${LocalDateTime.now.toString()} - ${msg}\n"
		Files.write(traceFile, s.getBytes, CREATE, APPEND)
	}
}

