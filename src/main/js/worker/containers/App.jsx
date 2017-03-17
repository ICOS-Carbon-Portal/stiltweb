import React, { Component } from 'react';
import { connect } from 'react-redux';
import * as Toaster from 'icos-cp-toaster';
import {copyprops} from 'icos-cp-utils';
import MapView from '../components/MapView.jsx';
import DashboardView from '../components/DashboardView.jsx';
import {stationSelected, jobdefUpdated, datesUpdated, useExistingStationData, startJob} from '../actions';
import {MAP_VIEW, DASHBOARD_VIEW} from '../actions';

class App extends Component {
	constructor(props) {
		super(props);
		this.state = {};
	}

	componentWillReceiveProps(nextProps){
		const toasterData = nextProps.toasterData;
		if(toasterData) this.setState({toasterData});
	}

	toastWarning(mess){
		this.setState({toasterData: new Toaster.ToasterData(Toaster.TOAST_WARNING, mess)});
	}

	toastError(mess){
		this.setState({toasterData: new Toaster.ToasterData(Toaster.TOAST_ERROR, mess)});
	}

	render() {
		const props = this.props;
		const subtitle = props.currentView == MAP_VIEW ? "Job starter" : "Dashboard"

		return <div>
			<Toaster.AnimatedToasters
				autoCloseDelay={5000}
				fadeInTime={100}
				fadeOutTime={400}
				toasterData={this.state.toasterData}
				maxWidth={400}
			/>

			<div className="page-header">
				<div className="pull-right">{
					props.userId && props.userId.length
						? <span>{"Logged in as " + props.userId}</span>
						: <a href={"https://cpauth.icos-cp.eu/login/?targetUrl=" + window.location.toString()}>Log in</a>
				}</div>
					<h1>STILT calculation service <small>{subtitle}</small></h1>
			</div>

			{
				props.currentView == MAP_VIEW
					? <MapView
						toastWarning={this.toastWarning.bind(this)}
						toastError={this.toastError.bind(this)}
						{...props}/>
					: <DashboardView {...copyprops(props, ['dashboardState', 'showMap'])} />
			}

		</div>;
	}
}

function stateToProps(state){
	return Object.assign({}, state);
}

function dispatchToProps(dispatch){
	return {
		selectStation: station => dispatch(stationSelected(station)),
		updateJobdef: update => dispatch(jobdefUpdated(update)),
		updateDates: dates => dispatch(datesUpdated(dates)),
		useExistingStationData: update => dispatch(useExistingStationData(update)),
		startJob: () => dispatch(startJob),
		showDashboard: () => dispatch({type: DASHBOARD_VIEW}),
		showMap: () => dispatch({type: MAP_VIEW})
	};
}

export default connect(stateToProps, dispatchToProps)(App)

