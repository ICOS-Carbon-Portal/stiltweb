import 'whatwg-fetch';
import {getJson, checkStatus} from 'icos-cp-backend';
import {copyprops} from 'icos-cp-utils';

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

export function enqueueJob(job){
	return fetch('enqueuejob', {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json'
		},
		body: JSON.stringify(job)
	}).then(checkStatus);
}

export function getStationInfo(){
	return getJson('stationinfo')
		.then(sInfos => sInfos.map(
			sInfo => {
				const name = sInfo.wdcggId || sInfo.icosId || sInfo.id;
				return Object.assign(copyprops(sInfo, ['id', 'lat', 'lon', 'alt']), {name});
			})
		);
}

