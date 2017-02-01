import React, { Component } from 'react';
import ReactDOM from 'react-dom';

export default class TextInput extends Component {
	constructor(props) {
		super(props);
		this.state = {
			val: this.props.value ? this.props.value : ''
		};
	}

	componentWillReceiveProps(nextProps){
		this.setState({val: nextProps.value ? nextProps.value : ''});
	}

	onTextChange(){
		const input = ReactDOM.findDOMNode(this.refs.input);
		this.setState({val: input.value});
	}

	render(){
		const props = this.props;
		const state = this.state;

		return <input ref="input" type="text" style={props.style}
					  value={state.val}
					  onChange={this.onTextChange.bind(this)}
		/>
	}
}