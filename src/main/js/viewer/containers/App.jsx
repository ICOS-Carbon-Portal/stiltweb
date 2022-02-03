import React, { Component } from 'react';
import { connect } from 'react-redux';
import {AnimatedToasters} from 'icos-cp-toaster';
import FootprintContainer from './FootprintContainer.jsx';
import ControlPanelContainer from './ControlPanelContainer.jsx';
import GraphsContainer from './GraphsContainer.jsx';
import Spinner from '../components/Spinner.jsx';
import config from '../config';
import { Copyright } from 'icos-cp-copyright';

const marginTop = 10;

class App extends Component {
	constructor(props) {
		super(props);
	}

	render() {
		const title = config.viewerScope
			? "STILT single-site scoped viewer"
			: "STILT results viewer";
		const showSpinner = this.props.showSpinner;

		return (
			<div>
				<AnimatedToasters toasterData={this.props.toasterData} autoCloseDelay={5000} />

				<div className="page-header">
					<h1>
						{title}
						<span style={{float:'right'}}>
							<a className="btn btn-info text-white" href="https://www.icos-cp.eu/about-stilt-viewer" target="_blank">
								<i className="fas fa-question-circle" /> Help
							</a>
						</span>
					</h1>
				</div>
				<div className="row" style={{marginTop}}>

					<div className="col-md-5" style={{paddingRight:0}}>
						{showSpinner ? <Spinner /> : null}
						<FootprintContainer />
					</div>

					<div className="col-md-7" style={{paddingLeft:0}}>
						<ControlPanelContainer />
					</div>

				</div>

				<div className="row" style={{marginTop}}>
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

export default connect(stateToProps)(App)