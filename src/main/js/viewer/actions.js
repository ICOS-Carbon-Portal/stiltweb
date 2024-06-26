import {getInitialData, getStationData} from './backend';
import {throttle} from 'icos-cp-utils';
import config from './config';

export const FETCHED_INITDATA = 'FETCHED_INITDATA';
export const FETCHED_STATIONDATA = 'FETCHED_STATIONDATA';
export const FETCHED_RESULT_PACKS_LIST = 'FETCHED_RESULT_PACKS_LIST';
export const FETCHED_RASTER = 'FETCHED_RASTER';
export const SET_STATION_FILTER = 'SET_STATION_FILTER'
export const SET_SELECTED_STATION = 'SET_SELECTED_STATION';
export const SET_SELECTED_SCOPE = 'SET_SELECTED_SCOPE';
export const SET_SELECTED_GAS = 'SET_SELECTED_GAS';
export const SET_DATE_RANGE = 'SET_DATE_RANGE';
export const SET_VISIBILITY = 'SET_VISIBILITY';
export const INCREMENT_FOOTPRINT = 'INCREMENT_FOOTPRINT';
export const SET_FOOTPRINT = 'SET_FOOTPRINT';
export const PUSH_PLAY = 'PUSH_PLAY';
export const SET_DELAY = 'SET_DELAY';
export const ERROR = 'ERROR';
export const SHOW_SPINNER = 'SHOW_SPINNER';
export const HIDE_SPINNER = 'HIDE_SPINNER';


export const fetchInitData = dispatch => {
	dispatch({type: SHOW_SPINNER});

	getInitialData().then(
		initData => {
			dispatch(Object.assign({type: FETCHED_INITDATA}, initData));
			if (config.viewerScope){
				const {stationId, fromDate, toDate} = config.viewerScope;
				const fromYear = fromDate.substring(0, 4)
				const scope = {fromDate, toDate}
				if (fromYear == toDate.substring(0, 4)) scope.year = parseInt(fromYear)
				dispatch(setStationFilter({predicate: station => station.siteId === stationId}))
				dispatch(setSelectedScope(scope));
				dispatch(setSelectedStationById(stationId));
			}
			dispatch({type: HIDE_SPINNER});
		},
		err => dispatch(failWithError(err))
	);
};

export function visibilityUpdate(name, visibility){
	return {
		type: SET_VISIBILITY,
		update: {[name]: visibility}
	};
}

export function failWithError(error){
	console.error(error);
	return {
		type: ERROR,
		error
	};
}

export function fetchedResultsPackList(packs){
	return {
		type: FETCHED_RESULT_PACKS_LIST,
		packs
	}
}

function gotStationData(stationData, stationId, fromDate, toDate){
	return Object.assign({}, stationData, {
		type: FETCHED_STATIONDATA,
		stationId,
		fromDate,
		toDate
	});
}

export const fetchStationData = (dispatch, getState) => {
	const state = getState();
	const scope = state.selectedScope;
	if (!scope || !state.selectedStation || !state.icosFormat) return;

	const stationId = state.selectedStation.id;

	dispatch({type: SHOW_SPINNER});
	getStationData(stationId, scope, state.icosFormat, state.selectedGas).then(
		stationData => {
			dispatch(gotStationData(stationData, stationId, scope.fromDate, scope.toDate));
			dispatch({type: HIDE_SPINNER});
			dispatch(fetchFootprint)
		},
		err => dispatch(failWithError(err))
	);
};

export function setStationFilter(stationFilter){
	return {
		type: SET_STATION_FILTER,
		stationFilter
	}
}


export const setSelectedStation = selectedStation => dispatch => {
	dispatch({
		type: SET_SELECTED_STATION,
		selectedStation
	});
	dispatch(fetchStationData); //date scope might have been selected automatically
};

export const setSelectedGas = selectedGas => (dispatch, getState) => {
	dispatch({
		type: SET_SELECTED_GAS,
		selectedGas
	});
	const station = getState().selectedStation
	if(station) dispatch(setSelectedStation(station))
}

const setSelectedStationById = stationId => (dispatch, getState) => {
	const station = getState().allStations.find(s => s.id === stationId);
	if(station) dispatch(setSelectedStation(station));
};

const setSelectedScope = selectedScope => dispatch => {
	dispatch({
		type: SET_SELECTED_SCOPE,
		selectedScope
	});
	dispatch(fetchStationData);
};

export const setSelectedYear = year => (dispatch, getState) => {
	const gas = getState().selectedGas
	const scope = Object.assign({}, year, {dataObject: year.dataObject[gas]})
	dispatch(setSelectedScope(scope))
}

export const setDateRange = dateRange => (dispatch, getState) => {
	const currRange = getState().dateRange;

	if(currRange && currRange[0] === dateRange[0] && currRange[1] === dateRange[1] || dateRange[0] >= dateRange[1]) return;

	dispatch({
		type: SET_DATE_RANGE,
		dateRange
	});

	fetchFootprintThrottled(dispatch);
}

const fetchFootprint = (dispatch, getState) => {
	const state = getState();
	const desired = state.desiredFootprint;
	if(!desired) return;

	if(state.footprint && desired.date === state.footprint.date) {
//		console.log('footprint already there, will increment if needed');
		dispatch(incrementIfNeeded);
	} else state.footprintsFetcher.fetch(desired).then(
		raster => {
			//console.log('fetched ', desired);
			dispatch({
				type: FETCHED_RASTER,
				footprint: desired,
				raster
			});
		},
		err => dispatch(failWithError(err))
	);
};

const fetchFootprintThrottled = throttle(dispatch => dispatch(fetchFootprint), 300);

export const jumpToFootprint = index => dispatch => {
	dispatch({
		type: SET_FOOTPRINT,
		index
	});

	dispatch(fetchFootprint);
};

export const incrementFootprint = increment => dispatch => {
	dispatch({
		type: INCREMENT_FOOTPRINT,
		increment
	});

	dispatch(fetchFootprint);
};

export const incrementIfNeeded = (dispatch, getState) => {
//	var ts = Date.now();
//	console.log('will increment in 5 ms after', ts);

	setTimeout(() => {
		if(getState().playingMovie) {
//			console.log('incrementing after ', ts);
			dispatch(incrementFootprint(1));
		} else {
//			console.log('not incrementing after ', ts);
		}
	}, 5); //a tiny delay in hope to improve interface's responsiveness
};

export const pushPlayButton = (dispatch, getState) => {
	dispatch({type: PUSH_PLAY});
	dispatch(incrementIfNeeded);
};

export const setDelay = delay => {
	return {
		type: SET_DELAY,
		delay
	};
};
