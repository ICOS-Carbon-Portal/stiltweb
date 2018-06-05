import React, { Component } from 'react';
import { connect } from 'react-redux';
import {AnimatedToasters} from 'icos-cp-toaster';
import FootprintContainer from './FootprintContainer.jsx';
import ControlPanelContainer from './ControlPanelContainer.jsx';
import GraphsContainer from './GraphsContainer.jsx';
import config from '../config';

const marginTop = 10;

class App extends Component {
	constructor(props) {
		super(props);
	}

	render() {
		const title = config.viewerScope
			? "STILT single-site scoped viewer"
			: "STILT results viewer";
		return (
			<div>
				<AnimatedToasters toasterData={this.props.toasterData} autoCloseDelay={5000} />

				<div className="page-header">
					<h1>{title}</h1>
				</div>
				<div className="row" style={{marginTop}}>

					<div className="col-md-4">
						<FootprintContainer />
					</div>

					<div className="col-md-8">
						<ControlPanelContainer />
					</div>

				</div>

				<div className="row" style={{marginTop}}>
					<div className="col-md-12">
						<GraphsContainer />
					</div>
				</div>

			</div>
		);
	}
}

function stateToProps(state){
	return Object.assign({}, state);
}

export default connect(stateToProps)(App)