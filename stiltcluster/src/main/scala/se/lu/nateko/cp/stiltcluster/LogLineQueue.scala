package se.lu.nateko.cp.stiltcluster

import scala.collection.mutable.Queue
import scala.collection.immutable.Seq
import scala.annotation.tailrec

// TODO This class is only used by ProcessRunner, which in turn is not being used
// Should probably be cleaned up
class LogLineQueue(sizeBound: Int):

	private val queue = Queue.empty[String]
	private var remainder = Array.empty[Byte]

	def append(bytes: Array[Byte]): Unit =
		remainder ++= bytes
		enqueueCompleteLines()

	def flush(): Unit =
		enqueue(remainder)
		remainder = Array.empty[Byte]

	def lines: Seq[String] = queue.toVector

	@tailrec private def enqueueCompleteLines(): Unit =
		val nlPos = remainder.indexOf(10)
		if(nlPos >= 0){
			enqueue(remainder.take(nlPos + 1))
			remainder = remainder.drop(nlPos + 1)
			enqueueCompleteLines()
		}

	private def enqueue(bytes: Array[Byte]): Unit =
		queue += new String(bytes, "UTF-8").stripLineEnd
		if(queue.size > sizeBound) queue.dequeue()

end LogLineQueue
