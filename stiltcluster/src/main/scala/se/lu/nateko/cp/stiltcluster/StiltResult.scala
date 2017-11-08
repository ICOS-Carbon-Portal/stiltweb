/* Handle the output files from a stilt simulation.

 The original stilt simulation software produces a lot of output files.  Below
 is a listing of a stilt simulation output directory.

Footprints
Footprints/XXX
Footprints/XXX/2012
Footprints/XXX/2012/.RDatastiltresult2012x56.10Nx013.42Ex00150_1
Footprints/XXX/2012/stiltresult2012x56.10Nx013.42Ex00150_1.csv
Footprints/XXX/2012/foot2012x12x01x00x56.10Nx013.42Ex00150_aggreg.nc
Footprints/XXX/2012/.RDatafoot2012x12x01x00x56.10Nx013.42Ex00150
Results
Results/XXX
Results/XXX/stiltresult2012x56.10Nx013.42Ex00150.csv
Results/XXX/.RData.XXX.2012.request
RData
RData/XXX
RData/XXX/2012
RData/XXX/2012/.RData2012x12x01x00x56.10Nx013.42Ex00150

 The above files were produced by a simulation started with this command:
   $ stilt run XXX 56.10 13.42 150 2012120100 2012120100

 This software (stiltweb) is only interested in the following files:

Footprints/XXX/2012/foot2012x12x01x00x56.10Nx013.42Ex00150_aggreg.nc
Footprints/XXX/2012/.RDatafoot2012x12x01x00x56.10Nx013.42Ex00150
RData/XXX/2012/.RData2012x12x01x00x56.10Nx013.42Ex00150
 */

package se.lu.nateko.cp.stiltcluster


import java.nio.file.{ Files, Path, Paths }


/* On of the three output file types we're interested in. The types are named
 * after the prefix in the file names
 */
object StiltResultFileType extends Enumeration {
	val Foot, RDataFoot, RData = Value
}


case class StiltResultFile (slot: StiltSlot,
							typ: StiltResultFileType.Value,
							data: Array[Byte])



object StiltResultFile {

	/* Read the - required - output files from a stilt simulation.

	 A stilt simulation produces many output files, but we only need three
	 specific files, one of each type. The stilt simulation software allows you
	 to specify a "job id", the original purpose was to separate several jobs
	 since stilt used a single giant output directory. Since stiltweb uses
	 generated output directories, we always set job id to "XXX".
	 */
	def readOutputFiles(slot: StiltSlot,
						dir: Path,
						jobId:String = "XXX"): Seq[StiltResultFile] = {
		import StiltResultFileType._
		Seq(Foot, RDataFoot, RData).map { typ =>
			val relPath = calcFileName(slot, typ, jobId)
			val absPath = dir.resolve(relPath)
			val data = Files.readAllBytes(absPath)
			new StiltResultFile(slot, typ, data)
		}
	}

	def calcFileName(slot: StiltSlot,
					 typ: StiltResultFileType.Value,
					 jobId: String = "XXX"): Path = {
		typ match {
			case StiltResultFileType.Foot =>
				// Footprints/XXX/2012/foot2012x12x01x00x56.10Nx013.42Ex00150_aggreg.nc
				Paths.get(f"Footprints/${jobId}/${slot.year}/foot${slot}_aggreg.nc")
			case StiltResultFileType.RDataFoot =>
				// Footprints/XXX/2012/.RDatafoot2012x12x01x00x56.10Nx013.42Ex00150
				Paths.get(f"Footprints/${jobId}/${slot.year}/.RDatafoot${slot}")
			case StiltResultFileType.RData =>
				// RData/XXX/2012/.RData2012x12x01x00x56.10Nx013.42Ex00150
				Paths.get(f"RData/${jobId}/${slot.year}/.RData${slot}")
		}
	}
}


case class StiltResult (val slot: StiltSlot, files: Seq[StiltResultFile])

object StiltResult {

	def apply(slot: StiltSlot, dir: Path): StiltResult = {
		new StiltResult(slot, StiltResultFile.readOutputFiles(slot, dir))
	}
}
