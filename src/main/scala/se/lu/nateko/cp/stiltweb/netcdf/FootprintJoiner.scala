package se.lu.nateko.cp.stiltweb.netcdf

import java.nio.file.Path
import java.nio.file.Files
import ucar.nc2.NetcdfFile
import ucar.nc2.NetcdfFileWriter
import java.nio.file.StandardCopyOption._

object FootprintJoiner {

	def joinToTempFile(footprints: Iterator[Path]): Path = {
		val fs = footprints.to(LazyList)
		val first = fs.headOption.getOrElse(throw new Exception("No footprints available"))

		val target = Files.createTempFile("footPrintsJoin_", ".nc")

		try{
			Files.copy(first, target, REPLACE_EXISTING)
			target.toFile.deleteOnExit()

			val w = NetcdfFileWriter.openExisting(target.toString)
			try{
				val footVar = w.findVariable("foot")
				val timeVar = w.findVariable("time")

				for((footPath, i) <- fs.tail.zipWithIndex){
					val nc = NetcdfFile.open(footPath.toString)

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
}