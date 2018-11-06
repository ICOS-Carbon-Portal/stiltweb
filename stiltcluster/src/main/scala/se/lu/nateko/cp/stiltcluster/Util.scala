package se.lu.nateko.cp.stiltcluster

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }
import java.nio.file.StandardCopyOption._
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

	def writeFileAtomically(f: Path, data: Array[Byte]): Unit = {
		val tmp = Paths.get(f.toAbsolutePath.toString + ".tmp")
		Files.write(tmp, data, TRUNCATE_EXISTING, CREATE, WRITE)
		Files.move(tmp, f, ATOMIC_MOVE, REPLACE_EXISTING)
	}

	def writeFileAtomically(f: Path, data: String): Unit = writeFileAtomically(f, data.getBytes(StandardCharsets.UTF_8))

	def ensureDirectory(d: Path): Path = {
		if (! Files.isDirectory(d))
			Files.createDirectories(d)
		d
	}

	def iterateChildren(dir: Path): Iterator[Path] = {
		import scala.collection.JavaConverters.asScalaIteratorConverter
		Files.list(dir).iterator.asScala
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
