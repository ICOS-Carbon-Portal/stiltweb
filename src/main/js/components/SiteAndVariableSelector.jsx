import React, { Component } from 'react'
import ReactDOM from 'react-dom'
import config from '../config'

export default class SiteAndVariableSelector extends Component {
	constructor(props){
		super(props);
	}

	render() {
		return <div className="row">
			<div className="col-md-8">

				<label htmlFor="site">Site</label> 

				<select ref="site" id="site" onChange={this.chooseSite.bind(this)}>{config.sites.map(
					site => <option value={site} key={site}>{site}</option>
				)}</select><br />

				{config.variables.map(
					v => <label key={v} className="checkbox-inline">
						<input type="checkbox" className="footprint" onClick={this.getVariableUpdater(v)}/>{v}<br/>
					</label>
				)}
			</div>
		</div>;
	}

	chooseSite(){
		const site = ReactDOM.findDOMNode(this.refs.site).value;
		this.props.chooseSite(site);
	}

	getVariableUpdater(varName){
		return event => {
			this.props.updateVariable({[varName]: event.target.checked});
		};
	}
}
