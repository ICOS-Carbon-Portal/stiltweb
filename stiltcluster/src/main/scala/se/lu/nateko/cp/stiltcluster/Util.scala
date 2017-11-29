package se.lu.nateko.cp.stiltcluster

import java.io.{BufferedWriter, File, FileOutputStream, FileWriter}
import java.nio.file.{Files, Path}
import java.nio.file.StandardOpenOption._
import java.time.LocalDateTime



trait Trace {

	var traceFile : Option[Path] = None

	def traceSetPath(f: Path): Unit = {
		traceFile = Some(f)
	}

	def trace(msg: String) = {
		assert(! traceFile.isEmpty)
		val s = s"${LocalDateTime.now.toString()} - ${msg}\n"
		Files.write(traceFile.get, s.getBytes, CREATE, APPEND)
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
			Files.createDirectory(d)
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
