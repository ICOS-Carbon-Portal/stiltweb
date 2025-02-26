import React, { Component } from 'react';
import { connect } from 'react-redux';
import * as Toaster from 'icos-cp-toaster';
import {copyprops} from 'icos-cp-utils';
import MapView from '../components/MapView.jsx';
import DashboardView from '../components/DashboardView.jsx';
import {stationSelected, jobDefUpdated, datesUpdated, useExistingStationData, startJob, cancelJob} from '../actions';
import {MAP_VIEW, DASHBOARD_VIEW} from '../actions';

export const cardHeaderInfo = "card-header alert-info fs-5 text-dark";

class App extends Component {
	constructor(props) {
		super(props);
		this.state = {};
		this.displayLoginInfo = true;
	}

	componentWillReceiveProps(nextProps){
		const toasterData = nextProps.toasterData;
		if(toasterData) this.setState({toasterData});

		if (this.displayLoginInfo && nextProps.currUser && !nextProps.currUser.email){
			this.displayLoginInfo = false;
			this.toastInfo("You have to be logged in to start a new job");
		}
	}

	toastInfo(mess){
		this.setState({toasterData: new Toaster.ToasterData(Toaster.TOAST_INFO, mess)});
	}

	toastWarning(mess){
		this.setState({toasterData: new Toaster.ToasterData(Toaster.TOAST_WARNING, mess)});
	}

	toastError(mess){
		this.setState({toasterData: new Toaster.ToasterData(Toaster.TOAST_ERROR, mess)});
	}

	render() {
		const props = this.props;
		const subtitle = props.currentView === MAP_VIEW ? "Job starter" : "Dashboard";

		return <div>
			<Toaster.AnimatedToasters
				autoCloseDelay={5000}
				fadeInTime={100}
				fadeOutTime={400}
				toasterData={this.state.toasterData || null}
				maxWidth={400}
			/>

			<div className="row page-header justify-content-between align-items-center mb-3">
				<h1 className="col-auto">
					STILT calculation service <span className="fs-3 text-secondary">{subtitle}</span>
				</h1>
				<div className="col-auto">
					<a className="btn btn-primary text-white mb-1" href="/viewer/" target="_blank">
						<i className="fas fa-search"/> STILT viewer
					</a>
					<a className="btn btn-info text-white mb-1" href="https://www.icos-cp.eu/about-stilt-calculator" target="_blank">
						<i className="fas fa-question-circle" /> Help
					</a>
				</div>
			</div>

			{
				props.currentView === MAP_VIEW
					? <MapView
						toastInfo={this.toastInfo.bind(this)}
						toastWarning={this.toastWarning.bind(this)}
						toastError={this.toastError.bind(this)}
						{...props}/>
					: <DashboardView {...copyprops(props, ['dashboardState', 'showMap', 'currUser', 'cancelJob'])} />
			}

		</div>;
	}
}

function dispatchToProps(dispatch){
	return {
		selectStation: station => dispatch(stationSelected(station)),
		updateJobDef: update => dispatch(jobDefUpdated(update)),
		updateDates: dates => dispatch(datesUpdated(dates)),
		useExistingStationData: update => dispatch(useExistingStationData(update)),
		startJob: () => dispatch(startJob),
		showDashboard: () => dispatch({type: DASHBOARD_VIEW}),
		showMap: () => dispatch({type: MAP_VIEW}),
		cancelJob: jobId => dispatch(cancelJob(jobId))
	};
}

export default connect(state => state, dispatchToProps)(App)
