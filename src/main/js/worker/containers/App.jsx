import React, { Component } from 'react';
import { connect } from 'react-redux';
import * as Toaster from 'icos-cp-toaster';
import MapView from '../components/MapView.jsx';
import {stationSelected, jobdefUpdated, startJob} from '../actions';

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

	render() {
		return <div>
			<Toaster.AnimatedToasters
				autoCloseDelay={5000}
				fadeInTime={100}
				fadeOutTime={400}
				toasterData={this.state.toasterData}
				maxWidth={400}
			/>

			<div className="page-header">
				<h1>STILT footprint worker</h1>
			</div>

			<div className="row" style={{marginTop: 10}}>
				<MapView toastWarning={this.toastWarning.bind(this)} {...this.props}/>
			</div>

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
		startJob: () => dispatch(startJob)
	};
}

export default connect(stateToProps, dispatchToProps)(App)

