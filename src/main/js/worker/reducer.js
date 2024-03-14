import {ERROR, FETCHED_INIT_INFO, GOT_DASHBOARD_STATE, STATION_SELECTED, JOBDEF_UPDATED, DATES_UPDATED, USE_EXISTING_STATION, STARTED_JOB} from './actions';
import {MAP_VIEW, DASHBOARD_VIEW, FETCHED_MONTHS} from './actions';

import AvailableMonths from './models/AvailableMonths';
import {copyprops, deepUpdate} from 'icos-cp-utils';
import {TOAST_ERROR, TOAST_INFO, ToasterData} from 'icos-cp-toaster';
import { initJob } from './store';


export default function(state, action){
	console.log({action})
	switch(action.type){

		case ERROR:
			return update({toasterData: new ToasterData(TOAST_ERROR, action.error.message.split('\n')[0])});

		case FETCHED_INIT_INFO:
			const stations = action.stations.map(s => {
				const newStation = copyprops(s, ['lat', 'lon', 'alt', 'name'])
				newStation.siteId = s.id
				return newStation
			})
			return update({stations, currUser: action.currUser})

		case FETCHED_MONTHS:
			return update({availableMonths: new AvailableMonths(action.availableMonths)});

		case STATION_SELECTED:
			return update(action.selectedStation)

		case JOBDEF_UPDATED:
			const jobUpdate = {}
			jobUpdate[action.update.propertyName] = action.update.value
			return withFeedbackToUser(update(jobUpdate))

		case DATES_UPDATED:
			const datesUpdate = {}
			const {start, stop} = action.dates
			if (start) datesUpdate.start = start
			if (stop) datesUpdate.stop = stop
			return update(datesUpdate)

		case USE_EXISTING_STATION:
			const existing = existingStation()
			return existing
				? update(copyprops(existing, ['lat', 'lon', 'alt', 'siteId']))
				: state

		case STARTED_JOB:
			const updates = Object.assign({}, initJob)
			if(!existingStation()){
				const newStations = state.stations.slice()
				const newStation = copyprops(state, ['lat', 'lon', 'alt', 'siteId'])
				newStations.push(newStation)
				update.stations = newStations
			}
			return update(updates)

		case GOT_DASHBOARD_STATE:
			// return update(copyprops(mockActivity(action), ['dashboardState']));
			return updateWith(['dashboardState']);

		case DASHBOARD_VIEW:
			return update({currentView: DASHBOARD_VIEW});

		case MAP_VIEW:
			return update({currentView: MAP_VIEW});

		default:
			return state;
	}

	function update(){
		const updates = Array.from(arguments);
		return Object.assign.apply(Object, [{}, state].concat(updates));
	}

	function updateWith(actionProps, path){
		return path
			? deepUpdate(state, path, copyprops(action, actionProps))
			: update(copyprops(action, actionProps));
	}

	function existingStation(){
		return state.stations.find(s => s.siteId === state.siteId)
	}

}

function withFeedbackToUser(state){
	const {lat, lon, alt, siteId, stations, start, stop} = state
	const jobSubmissionObstacles = []
	const existingStation = stations.find(s => s.siteId === siteId)
	const disableLatLonAlt = !!existingStation
	let toasterData = null
	if(existingStation){
		const es = existingStation
		if (es.lat !== lat || es.lon !== lon || es.alt != alt){
			const msg = 'You have entered an existing station\'s site code. Press "Load data" to use its parameters.'
			jobSubmissionObstacles.push(msg)
			toasterData = new ToasterData(TOAST_INFO, msg)
		}
	}
	if(!start) jobSubmissionObstacles.push("Start date missing")
	if(!stop) jobSubmissionObstacles.push("Stop date missing")
	if(state.currUser && !state.currUser.email){
		const msg = 'You must log in (USE SEPARATE BROWSER TAB) to submit a STILT job';
		jobSubmissionObstacles.push(msg)
		toasterData = new ToasterData(TOAST_INFO, msg)
	}
	return Object.assign({}, state, {
		jobSubmissionObstacles,
		disableLatLonAlt,
		toasterData
	})
}


function mockActivity(action){
	const getRndStr = () => 'rnd' + Math.floor(Math.random() * 9999999);
	const duplicateJob = (job) => {
		return Object.assign({}, job, {id: getRndStr()})
	};

	return Object.assign({}, action, {dashboardState: {
		done: [
			{
				failures: [],
				job: action.dashboardState.queue[0].job,
				nSlots: 10,
				nSlotsFinished: 10,
			},
			{
				failures: [{
					slot: {time: Date.now()},
					errorMessages: ['first error'],
					logsFilename: getRndStr()
				}],
				job: duplicateJob(action.dashboardState.queue[0].job),
				nSlots: 12,
				nSlotsFinished: 10,
			}
		],
		infra: [
			{
				address: "akka://somewhere...",
				nCpusFree: 3,
				nCpusTotal: 45,
			},
			{
				address: "akka://somewhere else...",
				nCpusFree: 0,
				nCpusTotal: 10,
			}
		],
		queue: action.dashboardState.queue,
		running: [
			{
				failures: [],
				job: duplicateJob(action.dashboardState.queue[0].job),
				nSlots: 10,
				nSlotsFinished: 5,
			},
			{
				failures: [],
				job: duplicateJob(action.dashboardState.queue[0].job),
				nSlots: 10,
				nSlotsFinished: 10,
			},
			{
				failures: [],
				job: duplicateJob(action.dashboardState.queue[0].job),
				nSlots: 10,
				nSlotsFinished: 23,
			},
			{
				failures: [],
				job: duplicateJob(action.dashboardState.queue[0].job),
				nSlots: 10,
				nSlotsFinished: 10,
			}
		],
	}});
}
