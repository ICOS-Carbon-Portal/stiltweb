import React, { Component } from 'react'
import { Provider } from 'react-redux'
import Store from '../store'
import App from './App.jsx'

export default class Root extends Component {

	render() {
		return (
			<Provider store={Store}>
				<App />
			</Provider>
		);
	}
}

