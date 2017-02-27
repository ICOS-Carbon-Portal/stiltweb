import React, { Component } from 'react';
import {copyprops} from 'icos-cp-utils';

export default class TextInput extends Component {
	constructor(props) {
		super(props);
		this.state = {
			val: this.props.value,
			error: null
		};
	}

	componentWillReceiveProps(nextProps){
		if (this.state.val !== nextProps.value) this.setState({val: nextProps.value});
	}

	onTextChange(event){
		const val = event.target.value;

		try {
			const converted = this.props.converter(val);

			act(this, converted, null);
			this.setState({val: converted, error: null});
		} catch(err){
			this.setState({val, error: err.message});
			act(this, val, "" + err.message);
		}
	}

	onTextBlur(event){
		const orgVal = event.target.value;
		const parsedVal = parseFloat(parseFloat(orgVal).toFixed(2));

		// Update state only for numbers
		if(isNaN(Date.parse(orgVal)) && isNumber(parsedVal) && parsedVal.toString() != orgVal) {
			act(this, parsedVal, null);
			this.setState({val: parsedVal, error: null});
		}
	}

	render(){
		const props = copyprops(this.props, ['disabled', 'maxLength']);
		const style = Object.assign(
			{},
			this.props.style,
			(this.state.error ? {backgroundColor: "pink"} : {})
		);

		return <input ref="input" className="form-control" type="text" {...props}
			value={this.state.val || ''} title={this.state.error} style={style}
			onChange={this.onTextChange.bind(this)} onBlur={this.onTextBlur.bind(this)}
		/>;
	}
}

function act(self, value, error){
	self.props.action({value, error});
}

function isNumber(n){
	return !isNaN(parseFloat(n)) && isFinite(n);
}
