import 'whatwg-fetch';
import {checkStatus, sparql, getJson, getBinaryTable, getBinRaster, tableFormatForSpecies} from 'icos-cp-backend';
import {icosAtmoReleaseQuery} from './sparqlQueries';
import {copyprops} from 'icos-cp-utils';
import {feature} from 'topojson';
import config from './config';

export function getInitialData(){
	return Promise.all([
		tableFormatForSpecies(config.icosCo2Spec, config),
		getStationInfo(),
		getCountriesGeoJson()
	]).then(([icosFormat, stations, countriesTopo]) => {return {icosFormat, stations, countriesTopo};});
}

function getCountriesGeoJson(){
	return getJson('https://static.icos-cp.eu/js/topojson/readme-world.json')
		.then(topo => feature(topo, topo.objects.countries));
}

function getStationInfo(){

	return Promise.all([
		getJson('stationinfo'),
		sparql(icosAtmoReleaseQuery(config.icosCo2Spec), config.sparqlEndpoint)
	])
	.then(([stInfos, sparqlResult]) => {

		const tsLookup = sparqlResult.results.bindings.reduce((acc, binding) => {
			const stationId = binding.stationId.value.trim();
			const dobjInfo = {
				start: new Date(binding.acqStartTime.value),
				stop: new Date(binding.acqEndTime.value),
				nRows: parseInt(binding.nRows.value),
				samplingHeight: parseFloat(binding.samplingHeight.value),
				id: binding.dobj.value
			};
			if(!acc.hasOwnProperty(stationId)) acc[stationId] = [];
			acc[stationId].push(dobjInfo);
			return acc;
		}, {});

		function dobjByStation(stInfo, year){
			const cands = tsLookup[stInfo.icosId];
			if(!cands) return undefined;
			function altDiff(dInfo){
				return Math.abs(dInfo.samplingHeight - stInfo.alt);
			}
			const available = cands
				.filter(dobj => dobj.start.getUTCFullYear() <= year && dobj.stop.getUTCFullYear() >= year)
				.sort((do1, do2) => altDiff(do1) - altDiff(do2)); //by proximity of sampling height and stilt altitude
			return available[0];
		}

		return stInfos.map(stInfo => {
			const name = stInfo.name || stInfo.icosId || stInfo.id;
			const years = stInfo.years.map(year =>
				({year, dataObject: dobjByStation(stInfo, year)})
			);

			return Object.assign(copyprops(stInfo, ['id', 'lat', 'lon', 'alt']), {name, years})
		});
	});
}

export function getRaster(stationId, filename){
	const id = stationId + filename;
	return getBinRaster(id, 'footprint', ['stationId', stationId], ['footprint', filename]);
}

export function getStationData(stationId, scope, icosFormat){
	//stationId: String
	//scope: {fromDate: LocalDate(ISO), toDate: LocalDate(ISO), dataObject: {id: String, nRows: Int, start: Data(UTC), stop: Date(UTC)}}
	//icosFormat: TableFormat
	const {fromDate, toDate} = scope;
	const footprintsListPromise = getFootprintsList(stationId, fromDate, toDate);
	const observationsPromise = getIcosBinaryTable(scope, icosFormat);
	const modelResultsPromise = getStiltResults({
		stationId,
		fromDate,
		toDate,
		columns: config.stiltResultColumns.map(series => series.label)
	});

	return Promise.all([observationsPromise, modelResultsPromise, footprintsListPromise])
		.then(([obsBinTable, modelResults, footprints]) => {return {obsBinTable, modelResults, footprints};});
}

function getIcosBinaryTable(scope, icosFormat){
	const {fromDate, toDate, dataObject} = scope;
	if(!dataObject) return Promise.resolve(null);
	const {nRows} = dataObject;

	const axisIndices = ['TIMESTAMP', config.observationVarName].map(idx => icosFormat.getColumnIndex(idx));
	const tblRequest = icosFormat.getRequest(dataObject.id, nRows, axisIndices);

	const scopeFrom = new Date(fromDate + 'T00:00:00.000Z').valueOf();
	const scopeTo = new Date(toDate + 'T23:59:59.999Z').valueOf();
	const dobjFrom = dataObject.start.valueOf();
	const dobjTo = dataObject.stop.valueOf();

	if(scopeFrom >= dobjTo || scopeTo <= dobjFrom) return Promise.resolve(null);

	if(scopeFrom <= dobjFrom && scopeTo >= dobjTo) return getBinaryTable(tblRequest);

	const dobjDur = dobjTo - dobjFrom;
	const offset = scopeFrom <= dobjFrom
		? 0
		: Math.floor(nRows * ((scopeFrom - dobjFrom) / dobjDur));
	const endPos = scopeTo >= dobjTo
		? nRows
		: Math.ceil(nRows * ((scopeTo - dobjFrom) / dobjDur));
	tblRequest.slice = {offset, length: endPos - offset};
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

function getFootprintsList(stationId, fromDate, toDate){
	return getJson('listfootprints', ['stationId', stationId], ['fromDate', fromDate], ['toDate', toDate]).then(fpArray => fpArray.sort());
}
