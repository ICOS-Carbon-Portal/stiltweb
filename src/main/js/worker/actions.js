import {getStationInfo, makeDashboardWebsocketConnection, enqueueJob} from './backend';

export const FETCHED_STATIONS = 'FETCHED_STATIONS';
export const GOT_DASHBOARD_STATE = 'GOT_DASHBOARD_STATE';
export const STATION_SELECTED = 'STATION_SELECTED';
export const JOBDEF_UPDATED = 'JOBDEF_UPDATED';
export const USE_EXISTING = 'USE_EXISTING';
export const STARTED_JOB = 'STARTED_JOB';
export const ERROR = 'ERROR';

export const MAP_VIEW = 'MAP_VIEW';
export const DASHBOARD_VIEW = 'DASHBOARD_VIEW';



function failWithError(error){
	console.log(error);
	return {
		type: ERROR,
		error
	};
}

export const fetchStationInfo = dispatch => {
	getStationInfo().then(
		stations => dispatch({type: FETCHED_STATIONS, stations}),
		err => dispatch(failWithError(err))
	);
}

export const establishWsCommunication = dispatch => makeDashboardWebsocketConnection(eventData => {
	dispatch({
		type: GOT_DASHBOARD_STATE,
		dashboardState: eventData
	});
})

export function stationSelected(selectedStation){
	return {
		type: STATION_SELECTED,
		selectedStation
	};
}

export function jobdefUpdated(update){
	return {
		type: JOBDEF_UPDATED,
		update
	};
}

export function useExistingStationData(){
	return {
		type: USE_EXISTING
	};
}

export const startJob = (dispatch, getState) => {
	const job = getState().jobdef;
	enqueueJob(job).then(
		() => dispatch({type: STARTED_JOB}),
		err => dispatch(failWithError(err))
	);
}

