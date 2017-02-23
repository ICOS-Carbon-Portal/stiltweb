import { createStore, applyMiddleware } from 'redux';
import thunkMiddleware from 'redux-thunk';
import reducer from './reducer';
import {fetchInitialInfo, establishWsCommunication, MAP_VIEW, DASHBOARD_VIEW} from './actions';

const initState = {
	stations: [],
	currentView: MAP_VIEW,
	dashboardState: {running: [], done: [], queue: []}
};


function logger({ getState }) {
	return (next) => (action) => {
//		console.log('will dispatch', action)

		// Call the next dispatch method in the middleware chain.
		let returnValue = next(action)

		console.log('state after dispatch', getState())

		// This will likely be the action itself, unless
		// a middleware further in chain changed it.
		return returnValue;
	}
}


export default function(){
	const store = createStore(reducer, initState, applyMiddleware(thunkMiddleware, logger));
	store.dispatch(fetchInitialInfo);
	store.dispatch(establishWsCommunication);
	return store;
}

