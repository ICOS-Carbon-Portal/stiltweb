import { createStore, applyMiddleware } from 'redux'
import thunkMiddleware from 'redux-thunk'
import reducer from './reducer'

const initState = {
	site: null,
	variables: {},
	computationStatus: "Computation not started yet"
};

const logger = store => next => action => {
	console.log('dispatching', action);
	// Call the next dispatch method in the middleware chain.
	let returnValue = next(action);
	console.log('state after dispatch', store.getState());
	return returnValue;
}

const store = createStore(reducer, initState, applyMiddleware(thunkMiddleware, logger));

export default store;
