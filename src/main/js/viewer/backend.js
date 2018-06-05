import 'whatwg-fetch';
import {checkStatus, sparql, getJson, getBinaryTable, getBinRaster, tableFormatForSpecies} from 'icos-cp-backend';
import {wdcggTimeSeriesQuery} from './sparqlQueries';
import {copyprops} from 'icos-cp-utils';
import {feature} from 'topojson';
import config from './config';

export function getInitialData(){
	return Promise.all([
		tableFormatForSpecies(config.wdcggSpec, config),
		getStationInfo(),
		getCountriesGeoJson()
	]).then(([wdcggFormat, stations, countriesTopo]) => {return {wdcggFormat, stations, countriesTopo};});
}

function getCountriesGeoJson(){
	return getJson('https://static.icos-cp.eu/js/topojson/readme-world.json')
		.then(topo => feature(topo, topo.objects.countries));
}

function getStationInfo(){
	return getJson('stationinfo').then(stInfos => {

		const wdcggNames = stInfos.map(stInfo => stInfo.wdcggId).filter(i => i);

		const query = wdcggTimeSeriesQuery(wdcggNames);

		return sparql(query, config.sparqlEndpoint).then(sparqlResult => {

			const dobjInfo = sparqlResult.results.bindings.map(binding => {
				const year = binding.ackStartTime && binding.ackEndTime
					? new Date(new Date(binding.ackStartTime.value).valueOf() / 2 + new Date(binding.ackEndTime.value).valueOf() / 2).getUTCFullYear()
					: undefined;
				return {
					wdcggId: binding.wdcggId.value.trim(),
					year,
					nRows: parseInt(binding.nRows.value),
					dobj: binding.dobj.value
				};
			});

			return stInfos.map(stInfo => {
				const name = stInfo.name || stInfo.wdcggId || stInfo.id;
				const years = stInfo.years.map(year => {
					return {year, dataObject: dobjByYear(dobjInfo, stInfo.wdcggId, year)};
				});

				return Object.assign(copyprops(stInfo, ['id', 'lat', 'lon', 'alt']), {name, years})
			});
		});
	});
}

function dobjByYear(dobjInfo, wdcggId, year){
	const available = dobjInfo
		.filter(dobj => dobj.year == year && dobj.wdcggId == wdcggId)
		.map(({dobj, nRows}) => {return {id: dobj, nRows};})
		.sort((do1, do2) => do2.nRows - do1.nRows); //by number of points, descending
	return available[0];
}

export function getRaster(stationId, filename){
	const id = stationId + filename;
	return getBinRaster(id, 'footprint', ['stationId', stationId], ['footprint', filename]);
}

export function getStationData(stationId, scope, wdcggFormat){
console.log(scope);
	//stationId: String, scope: {fromDate: LocalDate(ISO), toDate: LocalDate(ISO), dataObjectInfo: {id: String, nRows: Int}}, wdcggFormat: TableFormat
	const {fromDate, toDate, dataObject} = scope;
	const footprintsListPromise = getFootprintsList(stationId, fromDate, toDate);
	const observationsPromise = getWdcggBinaryTable(dataObject, wdcggFormat);
	const modelResultsPromise = getStiltResults({
		stationId,
		fromDate,
		toDate,
		columns: config.stiltResultColumns.map(series => series.label)
	});

	return Promise.all([observationsPromise, modelResultsPromise, footprintsListPromise])
		.then(([obsBinTable, modelResults, footprints]) => {return {obsBinTable, modelResults, footprints};});
}

function getWdcggBinaryTable(dataObjectInfo, wdcggFormat){
	if(!dataObjectInfo) return Promise.resolve(null);

	const axisIndices = ['TIMESTAMP', 'PARAMETER'].map(idx => wdcggFormat.getColumnIndex(idx));
	const tblRequest = wdcggFormat.getRequest(dataObjectInfo.id, dataObjectInfo.nRows, axisIndices);

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
