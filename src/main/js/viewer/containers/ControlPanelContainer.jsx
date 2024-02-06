import React from 'react';
import { connect } from 'react-redux';
import {copyprops} from 'icos-cp-utils';
import {visibilityUpdate, setSelectedYear, setSelectedStation, incrementFootprint, pushPlayButton, setDelay, setSelectedGas} from '../actions';
import ControlPanel from '../components/ControlPanel.jsx';
import config from '../config';


function stateToProps(state){
	return Object.assign(
		copyprops(state, ['stations', 'selectedGas', 'selectedStation', 'selectedScope', 'footprint', 'options', 'playingMovie']),
		{movieDelay: state.footprintsFetcher ? state.footprintsFetcher.delay : config.defaultDelay}
	);
}

function dispatchToProps(dispatch){
	return {
		updateVisibility: (name, visible) => dispatch(visibilityUpdate(name, visible)),
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
