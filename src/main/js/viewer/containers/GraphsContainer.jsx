import React from 'react';
import { connect } from 'react-redux';
import {copyprops} from 'icos-cp-utils';
import {setDateRange, jumpToFootprint} from '../actions.js';
import {formatDate} from '../models/formatting';
import Dygraphs from '../components/Dygraphs.jsx';
import config from '../config.js';

const GraphsContainer = props => props.timeSeriesData
	? <Dygraphs data={props.timeSeriesData} {...props}/>
	: <div />;

function stateToProps(state){
	const gasConf = config.byTracer[state.selectedGas]
	return Object.assign(
		{
			dateFormatter: formatDate,
			visibility: state.options.modelComponentsVisibility,
			graphOptions: {
				ylabel: gasConf.ylabel,
				y2label: gasConf.y2label,
				xlabel: 'timestamp (UTC)',
				axes: {
					y2: {
						axisLabelFormatter: number => Number(number).toFixed(2)
					}
				}
			}
		},
		copyprops(state, ['timeSeriesData', 'dateRange', 'selectedStation', 'axes', 'footprint', 'footprints'])
	);
}

function dispatchToProps(dispatch){
	return {
		updateXRange: range => dispatch(setDateRange(range)),
		jumpToFootprint: index => dispatch(jumpToFootprint(index)),
	};
}

export default connect(stateToProps, dispatchToProps)(GraphsContainer);

