import { createStore, applyMiddleware } from 'redux';
import thunkMiddleware from 'redux-thunk';
import reducer from './reducer';
import {fetchInitialInfo, establishWsCommunication, fetchAvailableMonths, MAP_VIEW} from './actions';
import WorkerData from './models/WorkerData';

const initState = {
	workerData: new WorkerData(),
	currentView: MAP_VIEW,
	dashboardState: {running: undefined, done: undefined, queue: undefined},
	yesNoViewVisible: false
};


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
	const store = createStore(reducer, initState, applyMiddleware(thunkMiddleware));//, logger));
	store.dispatch(fetchInitialInfo);
	store.dispatch(fetchAvailableMonths);
	store.dispatch(establishWsCommunication);
	return store;
}

