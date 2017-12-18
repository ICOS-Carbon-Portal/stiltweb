package se.lu.nateko.cp.stiltcluster

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory


object ConfigLoader {

	def workerNode(): Config = combineAsFallbackChain(local(), clusterBase())
	def frontNode(): Config = combineAsFallbackChain(local(), clusterFront(), clusterBase())

	def localWithDefault() = combineAsFallbackChain(local())

	private def clusterBase(): Config = ConfigFactory.parseResources("clusterbase.conf")
	private def clusterFront(): Config = ConfigFactory.parseResources("clusterfront.conf")

	private def local(): Config = ConfigFactory.parseFile(new java.io.File("local.conf"))

	private def combineAsFallbackChain(confs: Config*): Config = confs
		.foldRight(ConfigFactory.load()){
			(conf1, conf2) => conf1.withFallback(conf2)
		}.resolve()
}
