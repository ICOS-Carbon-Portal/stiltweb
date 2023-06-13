package se.lu.nateko.cp.stiltweb.zip

import java.nio.file.Path
import java.nio.file.Files
import ucar.nc2.NetcdfFiles
import java.nio.file.StandardCopyOption._
import ucar.nc2.write.NetcdfFormatWriter
import ucar.nc2.write.NetcdfFileFormat
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import java.io.FileOutputStream
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry
import java.security.MessageDigest

object Packager:

	def joinToTempFile(footprints: Iterator[Path]): Path =
		val fs = footprints.to(LazyList)
		val first = fs.headOption.getOrElse(throw new Exception("No footprints available"))

		val target = Files.createTempFile("footPrintsJoin_", ".nc")

		try
			var lastModified = Files.getLastModifiedTime(first)
			def updateLastModified(file: Path): Unit =
				val ft = Files.getLastModifiedTime(file)
				if lastModified.compareTo(ft) < 0 then lastModified = ft

			Files.copy(first, target, REPLACE_EXISTING)
			target.toFile.deleteOnExit()

			val w = NetcdfFormatWriter.builder()
				.setFormat(NetcdfFileFormat.NETCDF4)
				.setNewFile(false)
				.setLocation(target.toString)
				.build()
			try
				val footVar = w.findVariable("foot")
				val timeVar = w.findVariable("time")

				for((footPath, i) <- fs.tail.zipWithIndex){
					updateLastModified(footPath)
					val nc = NetcdfFiles.open(footPath.toString)

					val footArr = nc.findVariable("foot").read()
					val timeArr = nc.findVariable("time").read()

					nc.close()

					w.write(footVar, Array(i + 1, 0, 0), footArr)
					w.write(timeVar, Array(i + 1), timeArr)

				}
			finally
				w.close()
			Files.setLastModifiedTime(target, lastModified)
			target
		catch case err: Throwable =>
			Files.deleteIfExists(target)
			throw err
	end joinToTempFile

	def zipResults(
		footprints: Iterator[Path], csvRows: Iterator[String], fnamePrefix: String
	)(using ExecutionContext): Future[Path] =
		val netcdfFut = Future(joinToTempFile(footprints))
		val csvRowsFut = Future(csvRows.toIndexedSeq)

		for netcdfPath <- netcdfFut; csvRows <- csvRowsFut yield
			val lastModified = Files.getLastModifiedTime(netcdfPath)
			val zipPath = Files.createTempFile(s"StiltResult_${fnamePrefix}_", ".zip")
			val zipFile = zipPath.toFile
			zipFile.deleteOnExit()
			val fos = FileOutputStream(zipFile)
			val zos = ZipOutputStream(fos)
			def mkEntry(fnameSuffix: String, prefix: String = fnamePrefix + "_")(write: => Unit): Unit =
				val entry = ZipEntry(prefix + fnameSuffix)
				entry.setCreationTime(lastModified)
				entry.setLastModifiedTime(lastModified)
				entry.setLastAccessTime(lastModified)
				zos.putNextEntry(entry)
				write
				zos.closeEntry()
			try
				zos.setLevel(0)
				mkEntry("footprint.nc"):
					Files.copy(netcdfPath, zos)
				mkEntry("timeseries.csv"):
					csvRows.foreach:
						row => zos.write(row.getBytes()); zos.write('\n')
				inline val jsonFname = "stilt_model_metadata.json"
				mkEntry(jsonFname, ""):
					val in = getClass().getResourceAsStream("/" + jsonFname)
					if in == null then throw Exception(s"File $jsonFname was not present in the deployment package on the server")
					in.transferTo(zos)
			catch ex =>
				zos.close(); Files.deleteIfExists(zipPath); throw ex
			finally
				zos.close()
			zipPath
	end zipResults

	def md5Hex(file: Path)(using ExecutionContext): Future[String] = Future:
		val md = MessageDigest.getInstance("MD5")
		
		val is = Files.newInputStream(file)
		var read = 0
		val buff = new Array[Byte](8192)
		try
			while read != -1 do
				read = is.read(buff)
				if read > 0 then md.update(buff, 0, read)

			md.digest()
				.iterator
				.map:
					b => String.format("%02x", Int.box(255 & b))
				.mkString
		finally
			is.close()

end Packager
