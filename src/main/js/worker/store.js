import { createStore, applyMiddleware } from 'redux';
import thunkMiddleware from 'redux-thunk';
import reducer from './reducer';
import {fetchStationInfo, establishWsCommunication} from './actions';

const initState = {
	stations: []
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
	store.dispatch(fetchStationInfo);
	store.dispatch(establishWsCommunication);
	return store;
}

