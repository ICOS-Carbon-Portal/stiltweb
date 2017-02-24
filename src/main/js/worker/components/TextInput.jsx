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
		const self = this;
		const val = event.target.value;

		function act(value, error){
			self.props.action({value, error});
		}

		try {
			const converted = this.props.converter(val);
			act(converted, null);
			this.setState({val: converted, error: null});
		} catch(err){
			this.setState({val, error: err.message});
			act(val, "" + err.message);
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
			onChange={this.onTextChange.bind(this)}
		/>;
	}
}
