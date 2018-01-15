/* Handle the output files from a stilt simulation.

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

 The above files are those that are valuable to cache between runs of stilt.
 */

package se.lu.nateko.cp.stiltcluster


import java.nio.file.{ Files, Path, Paths }


/* One of the three output file types we're interested in. The types are named
 * after the prefix in the file names */
object StiltResultFileType extends Enumeration {
	val Foot, RDataFoot, RData = Value
}



/* A single stilt output file. We don't need to keep it's original filename
 * around since that can be generated from the slot and file type. The 'data'
 * member is the actual file data.
 */
case class StiltResultFile (slot: StiltSlot,
							typ: StiltResultFileType.Value,
							data: Array[Byte])

object StiltResultFile {


	def calcFileName(slot: StiltSlot,
					 typ: StiltResultFileType.Value,
					 jobId: String = "XXX"): Path = {
		typ match {
			case StiltResultFileType.Foot =>
				// Footprints/XXX/foot2012x12x01x00x56.10Nx013.42Ex00150_aggreg.nc
				Paths.get(f"Footprints/${jobId}/foot${slot}_aggreg.nc")
			case StiltResultFileType.RDataFoot =>
				// Footprints/XXX/.RDatafoot2012x12x01x00x56.10Nx013.42Ex00150
				Paths.get(f"Footprints/${jobId}/.RDatafoot${slot}")
			case StiltResultFileType.RData =>
				// RData/XXX/.RData2012x12x01x00x56.10Nx013.42Ex00150
				Paths.get(f"RData/${jobId}/.RData${slot}")
		}
	}
}



/* The result of a stilt computation for a single slot. It's just a slot and the
 * three valuable output files.
 *
 * Instances of this are sent on the wire (as Akka messages) from the backend
 * (computation) nodes to the frontend (web).
 */
case class StiltResult (slot: StiltSlot, files: Seq[StiltResultFile])
case class StiltFailure(slot: StiltSlot)

object StiltResult {

	import StiltResultFileType._
	val requiredFileTypes = Seq(Foot, RDataFoot, RData)

	/* Read the (valuable) output files from a stilt simulation.

	 A stilt simulation produces many output files, but we only need three
	 specific files, one of each type. The stilt simulation software allows you
	 to specify a "job id", the original purpose was to separate several jobs
	 since stilt used a single giant output directory. Since stiltweb uses
	 generated output directories, we always set job id to "XXX".
	 */
	def readOutputFiles(slot: StiltSlot,
						dir: Path,
						jobId:String = "XXX"): Seq[StiltResultFile] = {
		requiredFileTypes.map { typ =>
			val relPath = StiltResultFile.calcFileName(slot, typ, jobId)
			val absPath = dir.resolve(relPath)
			val data = Files.readAllBytes(absPath)
			new StiltResultFile(slot, typ, data)
		}
	}

	def apply(slot: StiltSlot, dir: Path): StiltResult = {
		new StiltResult(slot, readOutputFiles(slot, dir))
	}
}
