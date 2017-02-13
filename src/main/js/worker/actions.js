import {getInitialData, makeDashboardWebsocketConnection} from './backend';

export const FETCHED_INITDATA = 'FETCHED_INITDATA';
export const GOT_DASHBOARD_STATE = 'GOT_DASHBOARD_STATE';
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
console.log(eventData);
		dispatch({
			type: GOT_DASHBOARD_STATE,
			dashboardState: eventData
		});
	}
)

