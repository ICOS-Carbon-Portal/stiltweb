import React, { Component } from 'react';

export default class YesNoView extends Component {
	constructor(props) {
		super(props);
	}

	render(){
		const props = this.props;

		const style = props.visible
			? {width: '21rem'}
			: {display: 'none'};

		return (
			<div className="card" style={style}>
				<h5 className="card-header">
					{props.title}
				</h5>
				<div className="card-body">
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

