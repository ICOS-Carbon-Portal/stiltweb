import {getStationInfo, getWhoIam, makeDashboardWebsocketConnection, enqueueJob} from './backend';

export const FETCHED_INIT_INFO = 'FETCHED_INIT_INFO';
export const GOT_DASHBOARD_STATE = 'GOT_DASHBOARD_STATE';
export const STATION_SELECTED = 'STATION_SELECTED';
export const JOBDEF_UPDATED = 'JOBDEF_UPDATED';
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

export const fetchInitialInfo = dispatch => {
	Promise.all([getStationInfo(), getWhoIam()])
		.then(([stations, userId]) => {return {stations, userId};})
		.then(
			initInfo => dispatch(Object.assign({type: FETCHED_INIT_INFO}, initInfo)),
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

export const startJob = (dispatch, getState) => {
	const state = getState();
	const job = Object.assign({}, state.jobdef, {userId: state.userId});
	enqueueJob(job).then(
		() => dispatch({type: STARTED_JOB}),
		err => dispatch(failWithError(err))
	);
}

