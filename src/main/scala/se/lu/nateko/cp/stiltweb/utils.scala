package se.lu.nateko.cp.stiltweb.utils

import scala.concurrent.Future
import scala.util.control.NoStackTrace

def failure[T](msg: String): Future[T] =
	Future.failed(new Exception(msg) with NoStackTrace)
