import React, { Component } from 'react'
import { connect } from 'react-redux'
import { fetchData } from '../actions'

class App extends Component {
	constructor(props){
		super(props);
	}

	render() {
		return <div>
			<span>{this.props.dummyProperty}</span> <br />
			<button onClick={this.props.fetch}>Fetch data!</button>
		</div>;
	}
}

function dispatchToProps(dispatch){
	const columns = ['zi'];
	return {
		fetch: function(){
			dispatch(fetchData(columns));
		}
	};
}

export default connect(state => state, dispatchToProps)(App);

