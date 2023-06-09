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

	def joinToTempFile(footprints: Iterator[Path]): Path = {
		val fs = footprints.to(LazyList)
		val first = fs.headOption.getOrElse(throw new Exception("No footprints available"))

		val target = Files.createTempFile("footPrintsJoin_", ".nc")

		try{
			Files.copy(first, target, REPLACE_EXISTING)
			target.toFile.deleteOnExit()

			val w = NetcdfFormatWriter.builder()
				.setFormat(NetcdfFileFormat.NETCDF4)
				.setNewFile(false)
				.setLocation(target.toString)
				.build()
			try{
				val footVar = w.findVariable("foot")
				val timeVar = w.findVariable("time")

				for((footPath, i) <- fs.tail.zipWithIndex){
					val nc = NetcdfFiles.open(footPath.toString)

					val footArr = nc.findVariable("foot").read()
					val timeArr = nc.findVariable("time").read()

					nc.close()

					w.write(footVar, Array(i + 1, 0, 0), footArr)
					w.write(timeVar, Array(i + 1), timeArr)

				}
			}finally{
				w.close()
			}
			target
		}catch{
			case err: Throwable =>
				Files.deleteIfExists(target)
				throw err
		}
	}

	def zipResults(
		footprints: Iterator[Path], csvRows: Iterator[String], fnamePrefix: String
	)(using ExecutionContext): Future[Path] =
		val netcdfFut = Future(joinToTempFile(footprints))

		val zosFut = Future:
			val zipPath = Files.createTempFile(s"StiltResult_${fnamePrefix}_", ".zip")
			val zipFile = zipPath.toFile
			zipFile.deleteOnExit()
			val fos = FileOutputStream(zipFile)
			val zos = ZipOutputStream(fos)
			try
				zos.setLevel(0)
				zos.putNextEntry(ZipEntry(s"${fnamePrefix}_timeseries.csv"))
				csvRows.foreach:
					row => zos.write(row.getBytes()); zos.write('\n')
				zos.closeEntry()
			catch ex =>
				zos.close(); Files.deleteIfExists(zipPath); throw ex
			zipPath -> zos

		for netcdfPath <- netcdfFut; (zipPath, zos) <- zosFut yield
			try
				zos.putNextEntry(ZipEntry(s"${fnamePrefix}_footprint.nc"))
				Files.copy(netcdfPath, zos)
				zos.closeEntry()
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
