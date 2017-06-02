import {getStationInfo, getWhoIam, makeDashboardWebsocketConnection, enqueueJob, deleteJob, getAvailableMonths} from './backend';

export const FETCHED_INIT_INFO = 'FETCHED_INIT_INFO';
export const FETCHED_MONTHS = 'FETCHED_MONTHS';
export const GOT_DASHBOARD_STATE = 'GOT_DASHBOARD_STATE';
export const STATION_SELECTED = 'STATION_SELECTED';
export const JOBDEF_UPDATED = 'JOBDEF_UPDATED';
export const DATES_UPDATED = 'DATES_UPDATED';
export const USE_EXISTING_STATION = 'USE_EXISTING_STATION';
export const STARTED_JOB = 'STARTED_JOB';
export const TOGGLE_YESNO = 'TOGGLE_YESNO';
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
		.then(([stations, currUser]) => {return {stations, currUser};})
		.then(
			initInfo => dispatch(Object.assign({type: FETCHED_INIT_INFO}, initInfo)),
			err => dispatch(failWithError(err))
		);
};

export const establishWsCommunication = dispatch => makeDashboardWebsocketConnection(eventData => {
	dispatch({
		type: GOT_DASHBOARD_STATE,
		dashboardState: eventData
	});
});

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

export function datesUpdated(dates){
	return {
		type: DATES_UPDATED,
		dates
	};
}

export function useExistingStationData(){
	return {
		type: USE_EXISTING_STATION
	};
}

export function toggleYesNoView(){
	return {
		type: TOGGLE_YESNO
	};
}

export const cancelJob = jobId => dispatch => {
	deleteJob(jobId).then(
		() => dispatch({type: TOGGLE_YESNO}),
		err => dispatch(failWithError(err))
	);
};

export const startJob = (dispatch, getState) => {
	const state = getState();
	const job = Object.assign({}, state.workerData.jobDef, {userId: state.currUser.email});
	enqueueJob(job).then(
		() => dispatch({type: STARTED_JOB}),
		err => dispatch(failWithError(err))
	);
};

export const fetchAvailableMonths = dispatch => {
	getAvailableMonths().then(
		(availableMonths) => dispatch({
			type: FETCHED_MONTHS,
			availableMonths
		}),
		err => dispatch(failWithError(err))
	);
};
