package se.lu.nateko.cp.stiltweb.csv

import se.lu.nateko.cp.stiltweb.StiltResultsPresenter.*
import spray.json.*
import spray.json.DefaultJsonProtocol.*

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.MonthDay

object RowCache{
	val BytesPerRow = 4096
	type CachedRow = (LocalDayTime, JsObject)
}

class LocalDayTime(val day: MonthDay, val time: LocalTime)

object LocalDayTime{
	def apply(dt: LocalDateTime) = new LocalDayTime(MonthDay.from(dt), dt.toLocalTime)
}

class RowCache(rowFactory: () => Iterator[RowCache.CachedRow], parentFolder: Path, year: Int, slotStepInMinutes: Int) {
	import RowCache.BytesPerRow

	if !Files.exists(parentFolder) then throw NoSuchFileException(
		s"There appears to be no results for year $year"
	)
	private val cachePath = parentFolder.resolve(s"cache${slotStepInMinutes}_${BytesPerRow}.txt")
	private val yearStart = LocalDateTime.of(year, 1, 1, 0, 0)

	val maxNRows = {
		val stop = LocalDateTime.of(year + 1, 1, 1, 0, 0)
		(Duration.between(yearStart, stop).toMinutes / slotStepInMinutes).toInt
	}

	def getRows(from: Option[LocalDayTime], to: Option[LocalDayTime]): IndexedSeq[SlotCsvRow] =
		ensureCacheInitialized()
		fetchFromCache(from, to)


	def writeRow(dt: LocalDayTime, row: JsObject): Unit = {
		ensureCacheInitialized()
		val js = ByteBuffer.wrap(row.compactPrint.getBytes)
		val fc = FileChannel.open(cachePath, StandardOpenOption.WRITE)
		try{
			fc.write(js, slotByteOffset(dt))
		}finally{
			fc.close()
		}
	}

	def toLocalDateTime(dt: LocalDayTime) = LocalDateTime.of(dt.day.atYear(year), dt.time)
	private def toYearMinute(dt: LocalDayTime) = Duration.between(yearStart, toLocalDateTime(dt)).toMinutes.toInt

	private def rowNumber(dt: LocalDayTime): Long =
		Duration.between(yearStart, toLocalDateTime(dt)).toMinutes / slotStepInMinutes

	private def slotByteOffset(dt: LocalDayTime): Long = rowNumber(dt) * BytesPerRow

	private def fetchFromCache(from: Option[LocalDayTime], to: Option[LocalDayTime]): IndexedSeq[SlotCsvRow] =

		val fromMinute = from.map(toYearMinute).getOrElse(0)
		val toMinute = to.map(toYearMinute).getOrElse((maxNRows - 1) * slotStepInMinutes)

		val fc = FileChannel.open(cachePath, StandardOpenOption.READ)
		try
			val buff = ByteBuffer.allocate(BytesPerRow)
			val iter: Iterator[SlotCsvRow] = (0 until maxNRows).map(_ * slotStepInMinutes)
				.filter(minute => minute >= fromMinute && minute <= toMinute)
				.iterator
				.map: minute =>
					buff.rewind()
					fc.read(buff, (minute / slotStepInMinutes) * BytesPerRow.toLong)
					yearStart.plusMinutes(minute.toLong) -> (
						try
							new String(buff.array()).parseJson.asJsObject("Expected a JSON object")
						catch
							case err: Throwable => JsObject:
								"errorReadingSlotCache" -> JsString(err.getMessage)
					)
			iter.toIndexedSeq
		finally
			fc.close()

	end fetchFromCache


	private def ensureCacheInitialized(): Unit = {
		import java.nio.file.StandardOpenOption._

		if(Files.exists(cachePath)) return

		try{
			val fc = FileChannel.open(cachePath, CREATE_NEW, WRITE)

			val rowArr = Array.fill(BytesPerRow)(' '.toByte)
			rowArr(BytesPerRow - 1) = '\n'.toByte
			val buff = ByteBuffer.wrap(rowArr)

			try{
				//+ 1 just in case of an extra last slot present, which used to be the case for some time
				for(_ <- 1 to (maxNRows + 1)) {
					fc.write(buff)
					buff.rewind()
				}

				fc.position(0)

				rowFactory().foreach{case (dt, row) =>
					val js = ByteBuffer.wrap(row.toJson.compactPrint.getBytes)
					fc.write(js, slotByteOffset(dt))
				}
			}catch{
				case err: Throwable =>
					fc.close()
					Files.deleteIfExists(cachePath)
					throw err
			}finally{
				if(fc.isOpen) fc.close()
			}
		} catch {
			case _: FileAlreadyExistsException =>
				//expected error, no problem, cache already initialized, do nothing
		}

	}
}
