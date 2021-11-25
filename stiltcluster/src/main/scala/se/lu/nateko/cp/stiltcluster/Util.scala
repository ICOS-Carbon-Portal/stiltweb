package se.lu.nateko.cp.stiltcluster

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }
import java.nio.file.StandardCopyOption._
import java.nio.file.StandardOpenOption._
import java.util.Comparator
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import scala.util.Random

object Util {

	def writeFileAtomically(f: Path, data: Array[Byte]): Unit = {
		val tmp = Paths.get(f.toAbsolutePath.toString + ".tmp_" + Random.nextInt().toString)
		Files.write(tmp, data, CREATE, WRITE)
		Files.move(tmp, f, ATOMIC_MOVE, REPLACE_EXISTING)
	}

	def writeFileAtomically(f: Path, data: String): Unit = writeFileAtomically(f, data.getBytes(StandardCharsets.UTF_8))

	def ensureDirectory(d: Path): Path = {
		if (! Files.isDirectory(d)) Files.createDirectories(d)
		d
	}

	def iterateChildren(dir: Path): Iterator[Path] = {
		import scala.jdk.CollectionConverters.IteratorHasAsScala
		Files.list(dir).iterator.asScala
	}

	def deleteDirRecursively(dir: Path): Unit = Files.walk(dir)
		.sorted(Comparator.reverseOrder[Path])
		.forEach(Files.delete)


	def zipFolder(dir: Path, excludeFileNames: String*): Array[Byte] = {

		val bos = new ByteArrayOutputStream
		val zos = new ZipOutputStream(bos, StandardCharsets.UTF_8)
		val blackList = excludeFileNames.toSet

		Files.walk(dir)
			.filter(path => !Files.isDirectory(path) && !blackList.contains(path.getFileName.toString))
			.forEach{path =>
				val relPath = dir.relativize(path).toString
				zos.putNextEntry(new ZipEntry(relPath))
				zos.write(Files.readAllBytes(path))
				zos.closeEntry()
			}
		zos.close()
		bos.close()
		bos.toByteArray()
	}
}
