import {ERROR, FETCHED_INIT_INFO, GOT_DASHBOARD_STATE, STATION_SELECTED, JOBDEF_UPDATED, DATES_UPDATED, USE_EXISTING_STATION, STARTED_JOB} from './actions';
import {MAP_VIEW, DASHBOARD_VIEW, FETCHED_MONTHS} from './actions';

import AvailableMonths from './models/AvailableMonths';
import {copyprops, deepUpdate} from 'icos-cp-utils';
import * as Toaster from 'icos-cp-toaster';


export default function(state, action){

	switch(action.type){

		case ERROR:
			return update({toasterData: new Toaster.ToasterData(Toaster.TOAST_ERROR, action.error.message.split('\n')[0])});

		case FETCHED_INIT_INFO:
			return update({workerData: state.workerData.withStations(action.stations), currUser: action.currUser});

		case FETCHED_MONTHS:
			return update({availableMonths: new AvailableMonths(action.availableMonths)});

		case STATION_SELECTED:
			return update({workerData: state.workerData.withSelectedStation(action.selectedStation, true)});

		case JOBDEF_UPDATED:
			const workerData = state.workerData.withUpdatedFormData(action.update);

			if (workerData.isFormAndExistingStationDifferent) {
				const msg = 'You have entered the site code for an existing station. Press "Load data" to use its parameters.';
				return update({workerData}, {toasterData: new Toaster.ToasterData(Toaster.TOAST_INFO, msg)});
			} else if (workerData.isJobDefComplete && !state.currUser.email){
				const msg = 'You have to log in before submitting a new STILT job.';
				return update({workerData}, {toasterData: new Toaster.ToasterData(Toaster.TOAST_INFO, msg)});
			} else {
				return update({workerData});
			}

		case DATES_UPDATED:
			return update({workerData: state.workerData.withDates(action.dates)});

		case USE_EXISTING_STATION:
			return update({workerData: state.workerData.withExistingStationData()});

		case STARTED_JOB:
			const newStation = copyprops(state.workerData.jobDef, ['lat', 'lon', 'alt', 'siteId']);
			return update({workerData: state.workerData.resetAndAddNewStation(newStation)});

		case GOT_DASHBOARD_STATE:
			return updateWith(['dashboardState']);

		case DASHBOARD_VIEW:
			return update({currentView: DASHBOARD_VIEW});

		case MAP_VIEW:
			return update({currentView: MAP_VIEW});

		default:
			return state;
	}

	function keep(props, updatesObj){
		return Object.assign(copyprops(state, props), updatesObj); 
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

}


function jobdefIsComplete(job){
	return !!job
		&& job.lat !== undefined
		&& job.lon !== undefined
		&& job.alt !== undefined
		&& !!job.siteId
		&& !!job.start
		&& !!job.stop
		&& Date.parse(job.stop) >= Date.parse(job.start);
}

