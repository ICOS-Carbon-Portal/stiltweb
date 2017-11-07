package se.lu.nateko.cp.stiltcluster

import java.nio.file.{Files, Path}

class LocalStiltFile (slot: StiltSlot, src: Path, typ: StiltResultFileType.Value) {

	def linkToOriginalFileName(dir: Path) = {
		val relPath = StiltResultFile.calcFileName(slot, typ)
		val absPath = dir.resolve(relPath)
		Files.createSymbolicLink(absPath, src)
	}

}


class LocallyAvailableSlot (val slot: StiltSlot, files: Seq[LocalStiltFile] ) {

	def link(dir: Path) = {
		files.foreach { _.linkToOriginalFileName(dir) }
	}

	def equals(o: StiltSlot) = {
		this.slot == o
	}

}


object LocallyAvailableSlot {

	def apply (slot: StiltSlot, sdir: Path) = {
		// FIXME
		new LocallyAvailableSlot(slot, Seq())
	}
}
