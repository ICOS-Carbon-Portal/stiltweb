package se.lu.nateko.cp.stiltcluster

import java.io.File


object StiltResultFileType extends Enumeration {
	val Foot, RDataFoot, RData = Value
}


class StiltResultFile private (slot: StiltSlot, typ: StiltResultFileType.Value){

	def toFile(jobName: String = "XXX"): File = {
		typ match {
			case StiltResultFileType.Foot =>
				// Footprints/XXX/2012/foot2012x12x01x00x56.10Nx013.42Ex00150_aggreg.nc
				new File(f"/Footprints/${jobName}/${slot.year}/foot${slot}_aggreg.nc")

		}
	}

}


object StiltResultFile {

	def Foot(slot: StiltSlot): StiltResultFile = {
		new StiltResultFile(slot, StiltResultFileType.Foot)
	}

	def RDataFoot(slot: StiltSlot): StiltResultFile = {
		new StiltResultFile(slot, StiltResultFileType.RDataFoot)
	}

	def RData(slot: StiltSlot): StiltResultFile = {
		new StiltResultFile(slot, StiltResultFileType.RData)
	}
}


/* Here is a sample of a stilt simulation output directory:

The simulation was started with:
  $ stilt run XXX 56.10 13.42 150 2012120100 2012120100

Stilt will creata a toplevel 'logs' directory and an 'output' directory. The
following is a listing of the 'output' directory:

Footprints
Footprints/XXX
Footprints/XXX/2012
Footprints/XXX/2012/.RDatastiltresult2012x56.10Nx013.42Ex00150_1
Footprints/XXX/2012/stiltresult2012x56.10Nx013.42Ex00150_1.csv
- Footprints/XXX/2012/foot2012x12x01x00x56.10Nx013.42Ex00150_aggreg.nc
- Footprints/XXX/2012/.RDatafoot2012x12x01x00x56.10Nx013.42Ex00150
Results
Results/XXX
Results/XXX/stiltresult2012x56.10Nx013.42Ex00150.csv
Results/XXX/.RData.XXX.2012.request
RData
RData/XXX
RData/XXX/2012
- RData/XXX/2012/.RData2012x12x01x00x56.10Nx013.42Ex00150
 */

// class StiltResultFile {
// }
object StiltResult {

	// def findResults(top: File): // Seq[StiltResultFile]
	// = {


	// }
}
