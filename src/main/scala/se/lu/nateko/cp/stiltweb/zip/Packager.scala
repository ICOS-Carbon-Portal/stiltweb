package se.lu.nateko.cp.stiltweb.zip

import se.lu.nateko.cp.stiltcluster.StiltPosition
import se.lu.nateko.cp.stiltweb.ResultBatch
import ucar.ma2.DataType
import ucar.nc2.Attribute
import ucar.nc2.NetcdfFiles
import ucar.nc2.write.NetcdfFileFormat
import ucar.nc2.write.NetcdfFormatWriter

import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption._
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Using

object Packager:

	def joinToTempFile(footprints: Iterator[Path], batch: ResultBatch, pos: StiltPosition): Path =
		import batch.{stationId, fromDate, toDate}
		val fs = footprints.to(LazyList)
		val first = fs.headOption.getOrElse(throw new Exception("No footprints available"))

		val target = Files.createTempFile("footPrintsJoin_", ".nc")
		target.toFile.deleteOnExit()

		try
			var lastModified = Files.getLastModifiedTime(first)

			def updateLastModified(file: Path): Unit =
				val ft = Files.getLastModifiedTime(file)
				if lastModified.compareTo(ft) < 0 then lastModified = ft

			Files.copy(first, target, REPLACE_EXISTING)

			val globalAttrs = Seq(
				numAttribute("latstart", pos.lat, DataType.DOUBLE),
				numAttribute("lonstart", pos.lon, DataType.DOUBLE),
				numAttribute("aglstart", pos.alt, DataType.INT),
				strAttribute("title", s"STILT transport model results (footprints) for station $stationId from $fromDate to $toDate"),
			) ++ staticAttributes

			var wb = NetcdfFormatWriter.builder()
				.setFormat(NetcdfFileFormat.NETCDF4)
				.setNewFile(false)
				.setUseJna(true)
				.setLocation(target.toString)
			globalAttrs.foreach(a => wb = wb.addAttribute(a))

			Using(wb.build()): w =>
				globalAttrs.foreach(w.updateAttribute(null, _))

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
			Files.setLastModifiedTime(target, lastModified)
			target
		catch case err: Throwable =>
			Files.deleteIfExists(target)
			throw err
	end joinToTempFile

	private def numAttribute(name: String, value: Number, dt: DataType): Attribute =
		Attribute.builder().setName(name).setNumericValue(value, false).setDataType(dt).build()

	private def strAttribute(name: String, value: String): Attribute =
		Attribute.builder().setName(name).setStringValue(value).build()

	def zipResults(
		footprints: Iterator[Path],
		csvRows: Iterator[String],
		batch: ResultBatch,
		pos: StiltPosition
	)(using ExecutionContext): Future[Path] =
		val netcdfFut = Future(joinToTempFile(footprints, batch, pos))
		val csvRowsFut = Future(csvRows.toIndexedSeq)
		val fnamePrefix = resultFilePrefix(batch)

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

	def resultFilePrefix(batch: ResultBatch): String =
		s"${batch.stationId}_${batch.fromDate}_${batch.toDate}"

	private def staticAttributes = Seq(
		strAttribute("Conventions", "CF-1.6"),
		strAttribute("source", "STILT revision 721"),
		strAttribute("product_version", "1.0.0"),
		strAttribute("frequency", "3hr"),
		strAttribute("contact", "Ute Karstens, ICOS ERIC - Carbon Portal, ute.karstens@nateko.lu.se)"),
		strAttribute("institution", "ICOS ERIC - Carbon Portal, Physical Geography and Ecosystem Science, Lund University, Lund, Sweden"),
		strAttribute("license", "CC-BY 4.0"),
		strAttribute("creator", "Ute Karstens, ICOS ERIC - Carbon Portal,  orcid:0000-0002-8985-7742"),
		strAttribute("crs", "spherical earth with radius of 6371 km"),
		strAttribute("geospatial_lat_resolution", "1/12 degree"),
		strAttribute("geospatial_lon_resolution", "1/8 degree"),
		strAttribute("realm", "atmos"),
		strAttribute("references", "Lin et al., 2003, https://doi.org/10.1029/2002JD003161"),
		strAttribute("keywords", "atmospheric transport model, tracer transport, footprint"),
		strAttribute("summary", "STILT footprints for a European station location. The Stochastic Time Inverted Lagrangian Transport (STILT) model (Lin et al., 2003, https://doi.org/10.1029/2002JD003161) was driven by ECMWF-IFS meteorological analysis. Footprints were aggregate over 10 days backward in time to provide an overview of the region of influence. Footprints were produced using the STILT Footprint Tool at ICOS Carbon Portal (https://www.icos-cp.eu/data-services/tools/stilt-footprint) based on the STILT model code available at https://stilt-model.org/index.php/Main/HomePage")
	)
end Packager
