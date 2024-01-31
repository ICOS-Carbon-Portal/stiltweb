/* Representation of single stilt Slot.

 A "slot" is the smallest unit of simulation when using stilt. While
 theoretically variable in length, both our frontend and the simulation code is
 filled with assumptions that a slot is 3 hours long.

 When the user submits a Stilt job through the web frontend, we turn around and
 asks the simulation how many slots are in the given date range. This module
 represents those slots.

 This stilt simulation outputs files using a particular naming convention:
 For example:
   foot2012x12x01x00x56.10Nx013.42Ex00150_aggreg.nc

 The objects in this module all know how to convert themselves to/from such a
 string representation.
 */

package se.lu.nateko.cp.stiltcluster

import java.time.LocalDateTime

case class StiltPosition(lat: Double, lon: Double, alt: Int):
	import StiltPosition.{latFmt, lonFmt}
	// This is the format used by the (output files) original stilt software
	override def toString(): String =
		val s_lat = lat.abs.latFmt + (if (lat < 0) "S" else "N")
		val s_lon = lon.abs.lonFmt + (if (lon < 0) "W" else "E")
		val s_alt = f"${alt}%05d"
		s_lat + "x" + s_lon + "x" + s_alt // 2012x12x01x00x56.10Nx013.42Ex00150


object StiltPosition:

	extension (d: Double)
		def rootFormat(fmt: String): String = String.format(java.util.Locale.ROOT, fmt, d)
		def latFmt = d.abs.rootFormat("%05.2f")
		def lonFmt = d.abs.rootFormat("%06.2f")

	val re = """(\d+\.\d+)([NS])x(\d+\.\d+)([EW])x(\d+)""".r

	def unapply(s: String): Option[StiltPosition] = s match
		case re(latS, latC, lonS, lonC, alt) =>

			val lat = latS.toDouble * (if (latC == "N") 1 else -1)
			val lon = lonS.toDouble * (if (lonC == "E") 1 else -1)

			Some(StiltPosition(lat, lon, alt.toInt))
		case _ => None

end StiltPosition


case class StiltTime(year: Int, month: Int, day: Int, hour: Int) {

	// This is the format used by the (output files) original stilt software
	override def toString() = {
		f"${year}x${month}%02dx${day}%02dx${hour}%02d" // "2012x12x01x06"
	}

	def toJava = LocalDateTime.of(year, month, day, hour, 0)
}

object StiltTime {
	// The stilt software (both the original our our) is full of assumptions
	// that a "slot" is three hours long.
	final val validHours = List("00", "03", "06", "09", "12", "15", "18", "21")
	final val re = """(\d{4})x(\d{2})x(\d{2})x(\d{2})""".r // 2012x12x01x00

	def unapply(slot: String): Option[StiltTime] = slot match {
		case re(year, month, day, hour) if validHours contains hour =>
			Some(StiltTime(year.toInt, month.toInt, day.toInt, hour.toInt))
		case _ =>
			None
	}

	def fromJava(dt: LocalDateTime) = StiltTime(dt.getYear, dt.getMonthValue, dt.getDayOfMonth, dt.getHour)
}


case class StiltSlot(time: StiltTime, pos: StiltPosition) {
	def year = time.year
	def month = time.month
	def day = time.day
	def hour = time.hour

	def lat = pos.lat
	def lon = pos.lon
	def alt = pos.alt

	def ===(other: StiltSlot) = this == other

	override def toString(): String = {
		time.toString + "x" + pos.toString // "2012x12x01x00x56.10Nx013.42Ex00150"
	}
}


object StiltSlot {

	// s looks like "2012x12x01x00x56.10Nx013.42Ex00150"
	def unapply(s: String): Option[StiltSlot] = for(
		time <- StiltTime.unapply(s.substring(0, 13));
		pos <- StiltPosition.unapply(s.substring(14))
	) yield StiltSlot(time, pos)

}
