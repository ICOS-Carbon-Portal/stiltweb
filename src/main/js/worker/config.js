

export default {
	workerOutputDir: '/worker/output/',
	scopedViewLink(job){
		return `/viewer/?stationId=${job.siteId}&fromDate=${job.start}&toDate=${job.stop}`;
	}
};
