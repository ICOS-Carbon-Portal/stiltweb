import 'whatwg-fetch';
import _ from 'lodash';
import {checkStatus, sparql, getJson, getBinaryTable, getBinRaster, tableFormatForSpecies} from 'icos-cp-backend';
import {icosAtmoReleaseQuery} from './sparqlQueries';
import {copyprops} from 'icos-cp-utils';
import {feature} from 'topojson';
import config from './config';

export function getInitialData(){
	return Promise.all([ // binary cpb formats for co2 and ch4 are the same
		tableFormatForSpecies(config.byTracer.co2.observationDataSpec, config, true),
		getStationInfo(),
		getCountriesGeoJson()
	]).then(([icosFormat, stations, countriesTopo]) => {return {icosFormat, stations, countriesTopo};});
}

function getCountriesGeoJson(){
	return getJson('https://static.icos-cp.eu/js/topojson/readme-world.json')
		.then(topo => feature(topo, topo.objects.countries));
}

function getStationInfo(){
	const specs = Object.values(config.byTracer)
		.map(tracerConf => tracerConf.observationDataSpec);

	return Promise.all([
		getJson('stationinfo'),
		sparql(icosAtmoReleaseQuery(specs), config.sparqlEndpoint, true)
	])
	.then(([stInfos, sparqlResult]) => {
		const tsLookup = {}
		for(binding in sparqlResult.results.bindings) {
			const stationId = binding.stationId.value.trim();
			const dobjInfo = {
				start: new Date(binding.acqStartTime.value),
				stop: new Date(binding.acqEndTime.value),
				nRows: parseInt(binding.nRows.value),
				samplingHeight: parseFloat(binding.samplingHeight.value),
				spec: binding.spec.value,
				id: binding.dobj.value
			};
			if(!tsLookup.hasOwnProperty(stationId)) tsLookup[stationId] = [];
			tsLookup[stationId].push(dobjInfo);
		}

		function dobjByStation(stInfo, year){
			const cands = tsLookup[stInfo.icosId]
			const altDiff = dInfo => Math.abs(dInfo.samplingHeight - stInfo.alt)
			const byTracer = {}
			for(gas in config.byTracer){
				if(cands) {
					let spec = config.byTracer[gas].observationDataSpec
					let available = cands
						.filter(dobj => spec === dobj.spec && dobj.start.getUTCFullYear() <= year && dobj.stop.getUTCFullYear() >= year)
						.sort((do1, do2) => altDiff(do1) - altDiff(do2)); //by proximity of sampling height and stilt altitude
					byTracer[gas] = available[0]
				}
			}
			return byTracer
		}

		return stInfos.map(stInfo => {
			const name = stInfo.name || stInfo.icosId || stInfo.id;
			const isICOS = stInfo.icosId !== undefined;
			const years = stInfo.years.map(year =>
				({year, dataObject: dobjByStation(stInfo, year)})
			);

			return Object.assign(copyprops(stInfo, ['id', 'lat', 'lon', 'alt']), {name, years, isICOS})
		});
	});
}

export function getRaster(stationId, filename){
	const id = stationId + filename;
	return getBinRaster(id, 'footprint', ['stationId', stationId], ['footprint', filename]);
}

export function getStationData(stationId, scope, icosFormat){
	/**
	 * stationId: String
	 * scope: {
	 *    fromDate: LocalDate(ISO),
	 *    toDate: LocalDate(ISO),
	 *    dataObject: {
	 *       co2: undefined | {id: String, nRows: Int, start: Data(UTC), stop: Date(UTC)}}
	 *       ch4: undefined | {id: String, nRows: Int, start: Data(UTC), stop: Date(UTC)}}
	 *    }
	 * icosFormat: TableFormat
	 */
	const resultBatch = Object.assign({stationId}, _.pick(scope, ['fromDate', 'toDate']))
	const footprintsListPromise = getFootprintsList(resultBatch);
	const observationsPromise = getIcosBinaryTable(scope.dataObject, icosFormat);
	const modelResultsPromise = getStiltResults(
		Object.assign({}, resultBatch, {
			columns: config.stiltResultColumns.map(series => series.label)
		})
	)

	const packsPromise = getResultBatchJson('listresultpackages', resultBatch)

	return Promise.all([observationsPromise, modelResultsPromise, footprintsListPromise, packsPromise])
		.then(
			([obsBinTable, modelResults, footprints, packs]) => ({obsBinTable, modelResults, footprints, packs}),
			err => console.error(err)
		);
}

export function packageResults(resultBatch){
	return getResultBatchJson('joinfootprints', resultBatch)
}

function getIcosBinaryTable(dataObject, icosFormat){
	if(!dataObject) return Promise.resolve(null);
	const axisIndices = ['TIMESTAMP', config.observationVarName].map(idx => icosFormat.getColumnIndex(idx));
	const tblRequest = icosFormat.getRequest(dataObject.id, dataObject.nRows, axisIndices);
	return getBinaryTable(tblRequest);
}

function getStiltResults(resultsRequest){
	return fetch('stiltresult', {
		method: 'POST',
		headers: {"Content-Type": "application/json"},
		body: JSON.stringify(resultsRequest)
	})
	.then(checkStatus)
	.then(response => response.json());
}

function getFootprintsList(resultBatch){
	return getResultBatchJson('listfootprints', resultBatch).then(fpArray => fpArray.sort());
}

function getResultBatchJson(apiUriSegment, resultBatch){
	const {stationId, fromDate, toDate} = resultBatch
	return getJson(apiUriSegment, ['stationId', stationId], ['fromDate', fromDate], ['toDate', toDate]);
}
