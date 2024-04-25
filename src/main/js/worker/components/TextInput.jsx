import React, { Component } from 'react';
import {copyprops} from 'icos-cp-utils';

export default class TextInput extends Component {
	constructor(props) {
		super(props)
		this.state = {
			val: this.props.value,
			error: null
		}
	}

	componentWillReceiveProps(nextProps){
		if (this.state.val != nextProps.value) {
			this.setState({val: nextProps.value, error: null})
		}
	}

	updateText(event){
		const val = event.target.value
		try {
			const converted = this.props.converter
				? this.props.converter(val)
				: val

			this.setState({val: converted, error: null})
			if(this.props.action) this.props.action(converted)
		} catch(err) {
			this.setState({val, error: err.message})
		}
	}

	render(){
		const props = copyprops(this.props, ['disabled', 'maxLength', 'onClick']);

		props.style = Object.assign(
			(this.state.error ? {backgroundColor: "pink"} : {}),
			this.props.style,
		)
		const val = this.state.val
		props.value = (typeof val === 'number' && isNaN(val)) ? '' : this.state.val

		return <input {...props} className="form-control" type="text"
			title={this.state.error} onChange={this.updateText.bind(this)}/>
	}
}
