import React, { Component } from 'react';
import {copyprops} from 'icos-cp-utils';

export default class TextInput extends Component {
	constructor(props) {
		super(props);
		this.state = {
			val: this.props.value,
			error: null
		};
		this.sendVal = undefined;
	}

	componentWillReceiveProps(nextProps){
		if (this.state.val != nextProps.value) {
			const val = nextProps.value
				? nextProps.value + ""
				: "";
			this.updateText(val);
		}
	}

	updateText(val){
		try {
			const converted = this.props.converter(val);

			this.setState({val: converted, error: null});
			act(this, converted, null);
		} catch(err) {
			this.setState({val, error: err.message});
			act(this, val, "" + err.message);
		}
	}

	onTextChange(event){
		this.updateText(event.target.value);
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
	if (value != self.sendVal) self.props.action({value, error});

	self.sendVal = value;
}

function isNumber(n){
	return !isNaN(parseFloat(n)) && isFinite(n);
}
