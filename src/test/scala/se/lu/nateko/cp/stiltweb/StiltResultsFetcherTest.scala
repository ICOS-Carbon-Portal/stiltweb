package se.lu.nateko.cp.stiltweb

import org.scalatest.funsuite.AnyFunSuite


class StiltResultsFetcherTest extends AnyFunSuite {

	/* The following test won't work unless some specific file are created or
	   a mocked filesystem is set up.

These are the commands I used to create the files.
mkdir -p /mnt/additional_disk/WORKER/Input/Metdata/Europe2
touch /mnt/additional_disk/WORKER/Input/Metdata/Europe2/ECmetF.12100100.arl
touch /mnt/additional_disk/WORKER/Input/Metdata/Europe2/ECmetF.12010100.IN
touch /mnt/additional_disk/WORKER/Input/Metdata/Europe2/ECmetF.12110100.arl
touch /mnt/additional_disk/WORKER/Input/Metdata/Europe2/ECmetF.12030100.arl
touch /mnt/additional_disk/WORKER/Input/Metdata/Europe2/ECmetF.12040100.IN
touch /mnt/additional_disk/WORKER/Input/Metdata/Europe2/ECmetF.12090100.arl
touch /mnt/additional_disk/WORKER/Input/Metdata/Europe2/ECmetF.12110100.IN
touch /mnt/additional_disk/WORKER/Input/Metdata/Europe2/ECmetF.12050100.arl
touch /mnt/additional_disk/WORKER/Input/Metdata/Europe2/ECmetF.12070100.IN
touch /mnt/additional_disk/WORKER/Input/Metdata/Europe2/ECmetF.12020100.arl
touch /mnt/additional_disk/WORKER/Input/Metdata/Europe2/ECmetF.12060100.IN
touch /mnt/additional_disk/WORKER/Input/Metdata/Europe2/ECmetF.12010100.arl
touch /mnt/additional_disk/WORKER/Input/Metdata/Europe2/ECmetF.12020100.IN
touch /mnt/additional_disk/WORKER/Input/Metdata/Europe2/ECmetF.12090100.IN
touch /mnt/additional_disk/WORKER/Input/Metdata/Europe2/ECmetF.11120100.IN
touch /mnt/additional_disk/WORKER/Input/Metdata/Europe2/ECmetF.12050100.IN
touch /mnt/additional_disk/WORKER/Input/Metdata/Europe2/ECmetF.12060100.arl
touch /mnt/additional_disk/WORKER/Input/Metdata/Europe2/ECmetF.12070100.arl
touch /mnt/additional_disk/WORKER/Input/Metdata/Europe2/ECmetF.12080100.arl
touch /mnt/additional_disk/WORKER/Input/Metdata/Europe2/ECmetF.12080100.IN
touch /mnt/additional_disk/WORKER/Input/Metdata/Europe2/ECmetF.12100100.IN
touch /mnt/additional_disk/WORKER/Input/Metdata/Europe2/ECmetF.12030100.IN
touch /mnt/additional_disk/WORKER/Input/Metdata/Europe2/ECmetF.11120100.arl
touch /mnt/additional_disk/WORKER/Input/Metdata/Europe2/ECmetF.12040100.arl
touch /mnt/additional_disk/WORKER/Input/Metdata/Europe2/ECmetF.12120100.arl
touch /mnt/additional_disk/WORKER/Input/Metdata/Europe2/ECmetF.12120100.IN
	 */

	ignore("Test availablemonths") {
		val config = ConfigReader.default
		val srf = new StiltResultsPresenter(config)
		val cor = Vector("2011-12", "2012-01", "2012-02", "2012-03", "2012-04",
						 "2012-05", "2012-06", "2012-07", "2012-08", "2012-09",
						 "2012-10", "2012-11", "2012-12")
		assertResult(cor) { srf.availableInputMonths() }
	}
}
