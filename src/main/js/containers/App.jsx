import React, { Component } from 'react'
import ReactDOM from 'react-dom'
import { connect } from 'react-redux'
import { fetchData, chooseSite } from '../actions'
import SiteAndVariableSelector from '../components/SiteAndVariableSelector.jsx'

class App extends Component {
	constructor(props){
		super(props);
	}

	render() {
		return <div className="container-fluid">
			<SiteAndVariableSelector chooseSite={this.props.chooseSite} updateVariable={this.props.updateVariable}/>
		</div>;
	}
}

function dispatchToProps(dispatch){
	const columns = ['zi'];
	return {
//		fetch: () => dispatch(fetchData(columns)),
		chooseSite: function(site){
			dispatch(chooseSite(site));
		},
		updateVariable: function(update){
			console.log(update);
		}
	};
}

export default connect(state => state, dispatchToProps)(App);

