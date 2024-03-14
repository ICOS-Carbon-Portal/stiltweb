
export default {
	workerOutputDir: '/worker/output/',
	geoBoundary: {
		latMin: 33,
		lonMin: -15,
		latMax: 72.9166666666667,
		lonMax: 34.875
	},
	scopedViewLink(job){
		return `/viewer/?stationId=${job.siteId}&fromDate=${job.start}&toDate=${job.stop}`;
	}
}
