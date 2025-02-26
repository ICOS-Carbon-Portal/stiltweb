import React, { Component } from 'react';
import { connect } from 'react-redux';
import {AnimatedToasters} from 'icos-cp-toaster';
import FootprintContainer from './FootprintContainer.jsx';
import ControlPanelContainer from './ControlPanelContainer.jsx';
import GraphsContainer from './GraphsContainer.jsx';
import Spinner from '../components/Spinner.jsx';
import config from '../config';
import { Copyright } from 'icos-cp-copyright';
import ResultsControl from '../components/ResultsControl.jsx';
import { failWithError, fetchedResultsPackList } from '../actions.js';
import {formatDate} from '../models/formatting';

const marginTop = 10;

class App extends Component {
	constructor(props) {
		super(props);
	}

	render() {
		const {props} = this
		const title = config.viewerScope
			? "STILT single-site scoped viewer"
			: "STILT results viewer";
		const showSpinner = props.showSpinner;

		const resControlProps = {
			resultPacks: props.resultPacks,
			resultBatch: {
				stationId: props.selectedStation && props.selectedStation.siteId,
				fromDate: props.selectedScope && props.selectedScope.fromDate,
				toDate: props.selectedScope && props.selectedScope.toDate
			},
			failWithError: props.failWithError,
			updateResultPacks: props.updateResultPacks
		}

		const footprintStatus = props.footprint ? formatDate(props.footprint.date) : 'not loaded';

		return (
			<div>
				<AnimatedToasters toasterData={this.props.toasterData} autoCloseDelay={5000} />

				<div className="row page-header justify-content-between align-items-center">
					<h1 className="col-auto">
						{title}
					</h1>
					<div className="col-auto">
						<ResultsControl {...resControlProps}/>
						<a className="btn btn-primary text-white mb-1" href="/worker/" target="_blank">
							<i className="fas fa-calculator"/> STILT calculation
						</a>
						<a className="btn btn-info text-white mb-1" href="https://www.icos-cp.eu/about-stilt-viewer" target="_blank">
							<i className="fas fa-question-circle" /> Help
						</a>
					</div>
				</div>
				<div className="row" style={{marginTop}}>
				
					<h2 className="visually-hidden">Footprint map</h2>
					<div className="col-md-4" style={{paddingRight:0}}>
						<ul className="list-group rounded-0">
							<li className="list-group-item" style={{padding: 0}}>
								{showSpinner ? <Spinner /> : null}
								<FootprintContainer />
							</li>
							<li className="list-group-item">
								<strong>Footprint: </strong>{footprintStatus}
							</li>
						</ul>
					</div>

					<div className="col-md-8" style={{paddingLeft:0}}>
					<h2 className="visually-hidden">Footprint map</h2>
						<ControlPanelContainer />
					</div>

				</div>

				<div className="row" style={{marginTop}}>
					<h2 className="visually-hidden">Concentration time series graph</h2>
					<div className="col-md-12">
						<GraphsContainer />
					</div>
				</div>

				<div className="row">
					<div className="col-md-12">
						<Copyright rootStyleOverride={{float:'right', marginTop:10}} />
					</div>
				</div>

			</div>
		);
	}
}

function stateToProps(state){
	return Object.assign({}, state);
}

function dispatchToProps(dispatch){
	return {
		failWithError: err => dispatch(failWithError(err)),
		updateResultPacks: packs => dispatch(fetchedResultsPackList(packs))
	}
}

export default connect(stateToProps, dispatchToProps)(App)
