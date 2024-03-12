import { createStore, applyMiddleware } from 'redux'
import {thunk} from 'redux-thunk'
import reducer from './reducer'
import {fetchInitData} from './actions'
import Axes from "./models/Axes";
import config from './config';

const defaultGas = "co2"

const defaultVisibility = {}
for(let gas in config.byTracer){
	defaultVisibility[gas + ".stilt"] = true
	defaultVisibility[gas + ".observed"] = true
}

export const StationFilters = [{
	label: "ICOS",
	predicate: station => station.isIcos
}, {
	label: "Obspack",
	predicate: (station, gas) => station.years && station.years.some(year => year.dataObject && year.dataObject[gas])
}, {
	label: "All",
	predicate: () => true
}]

const initState = {
	axes: new Axes(defaultGas),
	showSpinner: false,
	icosFormat: null,
	stationFilter: StationFilters[0], //ICOS by default
	allStations: [],
	selectedGas: defaultGas,
	selectedStation: null,
	footprints: null,
	footprint: null,
	resultPacks: [],
	desiredFootprint: null,
	playingMovie: false,
	options: {
		modelComponentsVisibility: defaultVisibility
	},
	toasterData: null,
	error: null
};

/*
function logger({ getState }) {
	return (next) => (action) => {
		console.log('will dispatch', action)

		// Call the next dispatch method in the middleware chain.
		let returnValue = next(action)

		console.log('state after dispatch', getState())

		// This will likely be the action itself, unless
		// a middleware further in chain changed it.
		return returnValue
	}
}
*/

export default function(){
	const store = createStore(reducer, initState, applyMiddleware(thunk));//, logger));
	store.dispatch(fetchInitData);
	return store;
}
