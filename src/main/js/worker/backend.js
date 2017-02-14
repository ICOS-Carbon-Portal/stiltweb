import 'whatwg-fetch';
import {sparql, getJson} from 'icos-cp-backend';
import {stationInfoQuery} from './sparqlQueries';
import {groupBy, copyprops} from 'icos-cp-utils';
import config from './config';

export function makeDashboardWebsocketConnection(onUpdate){
	const l = window.location;
	const url = ((l.protocol === "https:") ? "wss://" : "ws://") + l.host + l.pathname + 'wsdashboardinfo';
	const ws = new WebSocket(url);
	ws.addEventListener('message', event => {
		if(event.data) onUpdate(JSON.parse(event.data))
	});
	ws.addEventListener('close', event => {
		console.log("Websocket connection closed, will try reestablishing in 60 seconds");
		setTimeout(() => makeDashboardWebsocketConnection(onUpdate), 60000);
	});
}

export function getInitialData(){
	return Promise.all([
		getStationInfo()
	]).then(([stations]) => {return {stations};});
}

function getStationInfo(){
	return Promise.all([
		sparql(stationInfoQuery, config.sparqlEndpoint),
		getJson('stationyears')
	]).then(([sparqlResult, stationYears]) => {

		const flatInfo = sparqlResult.results.bindings.map(binding => {
			const year = binding.ackStartTime && binding.ackEndTime
				? new Date(new Date(binding.ackStartTime.value).valueOf() / 2 + new Date(binding.ackEndTime.value).valueOf() / 2).getUTCFullYear()
				: undefined;
			return {
				id: binding.stiltId.value,
				name: binding.stationName.value.trim(),
				lat: parseFloat(binding.lat.value),
				lon: parseFloat(binding.lon.value),
				//TODO: add altitude to metadata
				alt: 55,
				year,
				nRows: binding.nRows ? parseInt(binding.nRows.value) : undefined,
				dobj: binding.dobj ? binding.dobj.value : undefined
			};
		}).filter(({id, year}) => stationYears[id] && stationYears[id].length > 0);

		const byId = groupBy(flatInfo, info => info.id);

		return Object.keys(byId).map(id => {
			const dataObjs = byId[id];

			const byYear = groupBy(
				dataObjs.filter(dobj => dobj.year),
				dobj => dobj.year
			);

			const years = stationYears[id].sort().map(year => {

				const dobjs = (byYear[year] || [])
					.map(({dobj, nRows}) => {return {id: dobj, nRows};})
					.sort((do1, do2) => do2.nRows - do1.nRows); //by number of points, descending

				return {year, dataObject: dobjs[0]};//picking the observation with largest number of points
			});

			return Object.assign(copyprops(dataObjs[0], ['id', 'name', 'lat', 'lon', 'alt']), {years});
		});
	});
}
