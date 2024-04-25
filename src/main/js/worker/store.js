import { createStore, applyMiddleware } from 'redux';
import {thunk} from 'redux-thunk';
import reducer, {withFeedbackToUser} from './reducer';
import {fetchInitialInfo, establishWsCommunication, fetchAvailableMonths, MAP_VIEW} from './actions';

export const initStation = {
	lat: NaN,
	lon: NaN,
	alt: NaN,
	siteId: null,
	siteName: null,
	icosId: null,
	countryCode: null
}

export const initJob = Object.assign({start: null, stop: null}, initStation)

const initState = Object.assign(
	{
		stations: [],
		currentView: MAP_VIEW,
		currUser: undefined,
		jobSubmissionObstacles: [],
		disableLatLonAlt: false,
		dashboardState: {running: [], done: [], queue: [], infra: []}
	},
	initJob
)

function logger({ getState }) {
	return (next) => (action) => {
//		console.log('will dispatch', action)

		// Call the next dispatch method in the middleware chain.
		let returnValue = next(action);

		console.log('state after dispatch', getState());

		// This will likely be the action itself, unless
		// a middleware further in chain changed it.
		return returnValue;
	}
}


export default function(){
	const store = createStore(reducer, withFeedbackToUser(initState), applyMiddleware(thunk))//, logger))
	store.dispatch(fetchInitialInfo);
	store.dispatch(fetchAvailableMonths);
	store.dispatch(establishWsCommunication);
	return store;
}

