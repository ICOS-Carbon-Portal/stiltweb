import React from 'react';
import { connect } from 'react-redux';
import {copyprops} from 'icos-cp-utils';
import {visibilityUpdate, setSelectedYear, setSelectedStation, incrementFootprint, pushPlayButton, setDelay, setSelectedGas, setStationFilter} from '../actions';
import ControlPanel from '../components/ControlPanel.jsx';
import config from '../config';


function stateToProps(state){
	const stationPred = s => state.stationFilter.predicate(s, state.selectedGas)
	const selectedStation = (state.selectedStation && stationPred(state.selectedStation)) ? state.selectedStation : null
	return Object.assign(
		copyprops(state, ['stationFilter', 'selectedGas', 'selectedScope', 'footprint', 'options', 'playingMovie']),
		{
			movieDelay: state.footprintsFetcher ? state.footprintsFetcher.delay : config.defaultDelay,
			stations: state.allStations.filter(stationPred),
			selectedStation
		}
	);
}

function dispatchToProps(dispatch){
	return {
		updateVisibility: (name, visible) => dispatch(visibilityUpdate(name, visible)),
		setStationFilter: stationFilter => dispatch(setStationFilter(stationFilter)),
		selectStation: station => {
			if(!config.viewerScope) dispatch(setSelectedStation(station));
		},
		selectYear: year => dispatch(setSelectedYear(year)),
		selectGas: gas => dispatch(setSelectedGas(gas)),
		incrementFootprint: increment => dispatch(incrementFootprint(increment)),
		pushPlay: () => dispatch(pushPlayButton),
		setDelay: delay => dispatch(setDelay(delay))
	};
}

export default connect(stateToProps, dispatchToProps)(props => <ControlPanel {...props} />);
