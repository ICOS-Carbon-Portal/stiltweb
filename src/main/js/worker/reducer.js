import {ERROR, FETCHED_INIT_INFO, GOT_DASHBOARD_STATE, STATION_SELECTED, JOBDEF_UPDATED, STARTED_JOB} from './actions';
import {MAP_VIEW, DASHBOARD_VIEW} from './actions';

import {copyprops, deepUpdate} from 'icos-cp-utils';
import * as Toaster from 'icos-cp-toaster';


export default function(state, action){

	switch(action.type){

		case ERROR:
			return Object.assign({}, state, {
				toasterData: new Toaster.ToasterData(Toaster.TOAST_ERROR, action.error.message.split('\n')[0])
			});

		case FETCHED_INIT_INFO:
			return updateWith(['stations', 'userId']);

		case STATION_SELECTED:
			const station = Object.assign({id: undefined, alt: 100}, action.selectedStation);
			var jobdef = Object.assign(
				{},
				state.jobdef,
				copyprops(station, ['lat', 'lon', 'alt']),
				{alreadyExists: !!station.id, siteId: station.id}
			);
			return update({selectedStation: station}, {jobdef}, {jobdefComplete: jobdefIsComplete(jobdef)});

		case JOBDEF_UPDATED:
			var jobdef = Object.assign({}, state.jobdef, action.update);
			const existing = state.stations.find(station => station.id == jobdef.siteId);
			if(existing)
				Object.assign(jobdef, copyprops(existing, ['lat', 'lon', 'alt']), {alreadyExists: true});
			else
				Object.assign(jobdef, {alreadyExists: false});
			return update({jobdef}, {jobdefComplete: jobdefIsComplete(jobdef)});

		case STARTED_JOB:
			const newStation = copyprops(state.jobdef, ['lat', 'lon', 'alt']);
			newStation.id = state.jobdef.siteId;
			const newStations = state.stations.concat([newStation]);
			return update({
				jobdef: undefined,
				jobdefComplete: false,
				stations: newStations,
				currentView: DASHBOARD_VIEW
			});

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
	return job && job.lat !== undefined && job.lon !== undefined && job.alt !== undefined && job.siteId && job.start && job.stop && Date.parse(job.stop) >= Date.parse(job.start);
}

