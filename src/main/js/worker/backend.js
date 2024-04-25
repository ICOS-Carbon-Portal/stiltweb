import {getJson, checkStatus} from 'icos-cp-backend';
import {copyprops} from 'icos-cp-utils';
import { initStation } from './store';

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
		credentials: 'include',
		body: JSON.stringify(job)
	}).then(checkStatus);
}

export function deleteJob(jobId){
	return fetch('deletejob/' + jobId, {
		method: 'POST',
		credentials: 'include'
	}).then(checkStatus);
}

export function getStationInfo(){
	return getJson('/viewer/stationinfo')
		.then(sInfos => sInfos.map(
			sInfo => {
				sInfo.siteId = sInfo.id
				sInfo.siteName = sInfo.name || sInfo.icosId || sInfo.id
				delete sInfo.id
				delete sInfo.name
				return Object.assign({}, initStation, sInfo)
			})
		);
}

export function getAvailableMonths(){
	return getJson('/viewer/availablemonths')
		.then(availableMonths => availableMonths);
}

export function getWhoIam(){
	return fetch('/whoami', {credentials: 'include'})
		.then(checkStatus)
		.then(resp => resp.json());
}

