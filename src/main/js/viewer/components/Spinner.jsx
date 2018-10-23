import React, { Component } from 'react';
import './Spinner.css';

export default class Spinner extends Component{
	constructor(props){
		super(props);
	}

	render(){
		const style = this.props.style || {};

		return (
			<div id="cp-spinner" style={style}>
				<div className="bounce1" />
				<div className="bounce2" />
				<div />
				<span>Carbon</span>
				<span>Portal</span>
			</div>
		);
	}
}