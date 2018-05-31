package se.lu.nateko.cp.stiltcluster

import java.io.{BufferedWriter, File, FileOutputStream, FileWriter}
import java.nio.file.{Files, Path}
import java.nio.file.StandardOpenOption._
import java.time.LocalDateTime



trait Trace {

	protected def traceFile: Path

	def trace(msg: String) = {
		val s = s"${LocalDateTime.now.toString()} - ${msg}\n"
		Files.write(traceFile, s.getBytes, CREATE, APPEND)
	}
}



object Util {
	import scala.sys.process._

	def writeFileAtomically(f: File, data: Array[Byte]) = {
		val t = new File(f.toString + ".tmp")
		val o = new FileOutputStream(t)
		o.write(data)
		o.close
		t.renameTo(f)
	}

	def writeFileAtomically(f: File, data: String) = {
		val t = new File(f.toString + ".tmp")
		val w = new BufferedWriter(new FileWriter(t))
		w.write(data)
		w.close
		t.renameTo(f)
	}

	def createEmptyFile(dir: File, file: String): Unit = {
		val f = new File(dir, file)
		f.createNewFile()
	}

	def fileExists(dir: File, file: String): Boolean = {
		new File(dir, file).exists
	}

	def createSymbolicLink(link: File, target: File) = {
		Files.createSymbolicLink(link.toPath(), target.toPath())
	}

	def ensureDirectory(d: Path): Path = {
		if (! Files.isDirectory(d))
			Files.createDirectories(d)
		d
	}

	def listDirTree(dir: Path): String = {
		val cmd = Seq("bash", "-c", s"cd '${dir}' && find | sort")
		cmd.!!
	}

	def deleteTmpDirTree(dir: Path): Unit = {
		assert(dir.getParent.toString == "/tmp")
		val cmd = Seq("rm", "-rf", "--", dir.toString)
		cmd.!!
	}

}
