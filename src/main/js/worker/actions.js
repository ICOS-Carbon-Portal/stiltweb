import {getInitialData, makeDashboardWebsocketConnection, enqueueJob} from './backend';

export const FETCHED_INITDATA = 'FETCHED_INITDATA';
export const GOT_DASHBOARD_STATE = 'GOT_DASHBOARD_STATE';
export const STATION_SELECTED = 'STATION_SELECTED';
export const JOBDEF_UPDATED = 'JOBDEF_UPDATED';
export const STARTED_JOB = 'STARTED_JOB';
export const ERROR = 'ERROR';


function failWithError(error){
	console.log(error);
	return {
		type: ERROR,
		error
	};
}

export const fetchInitData = dispatch => {
	getInitialData().then(
		initData => dispatch(Object.assign({type: FETCHED_INITDATA}, initData)),
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
	const job = getState().jobdef;
	enqueueJob(job).then(
		() => dispatch({type: STARTED_JOB}),
		err => dispatch(failWithError(err))
	);
}

