import React, { Component } from 'react';
import ReactDOM from 'react-dom';
import {copyprops} from 'icos-cp-utils';

export default class TextInput extends Component {
	constructor(props) {
		super(props);
		this.state = {
			val: this.props.value || ''
		};
	}

	componentWillReceiveProps(nextProps){
		if(nextProps.value !== undefined) this.setState({val: nextProps.value});
	}

	onTextChange(){
		const input = ReactDOM.findDOMNode(this.refs.input);
		var val = input.value;
		this.setState({val, error: null});

		if (this.props.action){
			if(this.props.converter){
				try{
					val = this.props.converter(val);
					this.props.action(val);
				}catch(err){
					this.setState({error: err.message});
					this.props.action(undefined);
				}
			} else this.props.action(val);
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
			value={this.state.val} title={this.state.error} style={style}
			onChange={this.onTextChange.bind(this)}
		/>;
	}
}
