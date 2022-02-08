import React, { Component } from 'react';
import { Provider } from 'react-redux';
import getStore from '../store';
import App from './App.jsx';

import '../../common/customStyles.css';

const store = getStore();

export default class Root extends Component {
	render() {
		return (
			<Provider store={store}>
				<App />
			</Provider>
		);
	}
}

