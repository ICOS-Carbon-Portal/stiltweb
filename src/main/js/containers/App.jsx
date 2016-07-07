import React, { Component } from 'react'
import ReactDOM from 'react-dom'
import { connect } from 'react-redux'
import { fetchData, startStilt } from '../actions'

class App extends Component {
	constructor(props){
		super(props);
		this.startStilt = this.startStilt.bind(this);
	}

	render() {
		return <div className="container-fluid" id="stations">
			<div className="row">
				<div className="col-md-8">
					<span><label>Computation status: </label>{this.props.computationStatus}</span><br />

					<label htmlFor="site">Select dataset to use as input to STILT</label><br />

					<select ref="site" id="site">{this.props.sites.map(
						(site, i) => <option value={site} key={i}>{site}</option>
					)}</select>

					<div>
						<button onClick={this.startStilt}>Start</button>
					</div>
				</div>
			</div>
		</div>;
	}

	startStilt() {
		const chosenSite = ReactDOM.findDOMNode(this.refs.site).value;
		if(chosenSite){
			this.props.start(chosenSite);
		}
	}
}

function dispatchToProps(dispatch){
	const columns = ['zi'];
	return {
		fetch: () => dispatch(fetchData(columns)),
		start: site => dispatch(startStilt(site))
	};
}

export default connect(state => state, dispatchToProps)(App);

