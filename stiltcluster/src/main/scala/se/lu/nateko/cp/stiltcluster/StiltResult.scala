/** Handle the output files from a stilt simulation.

 The original stilt simulation software produces a lot of output files.  Below
 is a listing of a stilt simulation output directory.


Footprints
Footprints/XXX
Footprints/XXX/.RDatastiltresult2012x56.10Nx013.42Ex00150_1
Footprints/XXX/stiltresult2012x56.10Nx013.42Ex00150_1.csv
Footprints/XXX/foot2012x12x01x00x56.10Nx013.42Ex00150_aggreg.nc
Footprints/XXX/.RDatafoot2012x12x01x00x56.10Nx013.42Ex00150
Results
Results/XXX
Results/XXX/stiltresult2012x56.10Nx013.42Ex00150.csv
Results/XXX/stiltresult2012x56.10Nx013.42Ex00150_1.csv
Results/XXX/.RData.XXX.2012.request
RData
RData/XXX
RData/XXX/.RData2012x12x01x00x56.10Nx013.42Ex00150

 The above files were produced by a simulation started with this command:
   $ stilt run XXX 56.10 13.42 150 2012120100 2012120100

 We (the stiltweb software) is only interested in the following files:

Footprints/XXX/foot2012x12x01x00x56.10Nx013.42Ex00150_aggreg.nc
Footprints/XXX/.RDatafoot2012x12x01x00x56.10Nx013.42Ex00150
RData/XXX/.RData2012x12x01x00x56.10Nx013.42Ex00150
Results/XXX/stiltresult2012x56.10Nx013.42Ex00150_1.csv

 The above files are those that are valuable to cache between runs of stilt.
 */

package se.lu.nateko.cp.stiltcluster


import java.io.FileNotFoundException
import java.nio.file.{ Files, Path, Paths }


/** STILT output file types, named after the corresponding final (i.e. long-term storage) file names */
object StiltResultFileType extends Enumeration {
	val Foot = Value("foot")
	val RDataFoot = Value("rdatafoot")
	val RData = Value("rdata")
	val CSV = Value("csv")
}



/**
 * A single stilt output file. We don't need to keep it's original filename
 * around since that can be generated from the slot and file type. The 'data'
 * member is the actual file data.
 */
case class StiltResultFile (slot: StiltSlot, typ: StiltResultFileType.Value, data: Array[Byte])

object StiltResultFile {

	import StiltResultFileType._

	def calcFileName(slot: StiltSlot, typ: StiltResultFileType.Value, jobId: String): Path = {
		typ match {

			case Foot =>
				// Footprints/XXX/foot2012x12x01x00x56.10Nx013.42Ex00150_aggreg.nc
				Paths.get(f"Footprints/${jobId}/foot${slot}_aggreg.nc")

			case RDataFoot =>
				// Footprints/XXX/.RDatafoot2012x12x01x00x56.10Nx013.42Ex00150
				Paths.get(f"Footprints/${jobId}/.RDatafoot${slot}")

			case RData =>
				// RData/XXX/.RData2012x12x01x00x56.10Nx013.42Ex00150
				Paths.get(f"RData/${jobId}/.RData${slot}")

			case CSV =>
				// Results/XXX/stiltresult2012x56.10Nx013.42Ex00150_1.csv
				Paths.get(f"Results/${jobId}/stiltresult${slot.year}x${slot.pos}_1.csv")

		}
	}
}



/**
 *  The result of a stilt computation for a single slot. It's just a slot and the
 * four valuable output files.
 *
 * Instances of this are sent on the wire (as Akka messages) from the backend
 * (computation) nodes to the frontend (web).
 */
case class StiltResult (slot: StiltSlot, files: Seq[StiltResultFile])

object StiltResult {

	/** Read the (valuable) output files from a stilt simulation.

	 A stilt simulation produces many output files, but we only need four
	 specific files, one of each type. The stilt simulation software allows you
	 to specify a "job id", the original purpose was to separate several jobs
	 since stilt used a single giant output directory. Since stiltweb uses
	 generated output directories, we always set job id to "XXX".
	 */
	def readOutputFiles(slot: StiltSlot, dir: Path, jobId: String = "XXX"): Seq[StiltResultFile] =
		StiltResultFileType.values.toSeq.map { typ =>

			val relPath = StiltResultFile.calcFileName(slot, typ, jobId)
			val absPath = dir.resolve(relPath)

			if(!Files.exists(absPath)) throw new FileNotFoundException(
				s"File ${relPath.getFileName} was not found after calculation. The calculation must have failed."
			)

			val data = Files.readAllBytes(absPath)
			new StiltResultFile(slot, typ, data)
		}


	def apply(slot: StiltSlot, dir: Path): StiltResult = {
		new StiltResult(slot, readOutputFiles(slot, dir))
	}
}
