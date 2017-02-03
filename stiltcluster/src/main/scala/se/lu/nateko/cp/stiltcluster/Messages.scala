package se.lu.nateko.cp.stiltcluster

sealed trait StiltMessage extends java.io.Serializable

case object WorkMasterRegistration extends StiltMessage
