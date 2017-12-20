import React, { Component } from 'react';

export default class YesNoView extends Component {
	constructor(props) {
		super(props);
	}

	render(){
		const props = this.props;

		const style = props.visible
			? {}
			: {display: 'none'};

		return (
			<div className="panel panel-default" style={style}>
				<div className="panel-heading">
					<h3 className="panel-title">{props.title}</h3>
				</div>
				<div className="panel-body">
					<div style={{margin: '0px 0px 30px 0px'}}>
						{props.question}
					</div>
					<button className="btn btn-primary" onClick={props.actionYes}>Yes</button>
					<button className="btn btn-primary" style={{marginLeft: 10}} onClick={props.actionNo}>No</button>
				</div>
			</div>
		);
	}
}

