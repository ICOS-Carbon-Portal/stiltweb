import React, { Component } from 'react';
import { packageResults } from '../backend';

export default class ResultsControl extends Component{
	constructor(props){
		super(props);

		this.state = {
			packaging: false
		};

	}

	onPackageClick(){
		if(this.state.packaging) return
		const {props} = this
		this.setState({packaging: true})
		const {stationId, fromDate, toDate} = props
		packageResults(stationId, fromDate, toDate)
			.then(props.updateResultPacks, props.failWithError)
			.finally(() => this.setState({packaging: false}))
	}

	render(){
		const {props, state} = this
		const {stationId, fromDate, toDate} = props
		if(!stationId || !fromDate || !toDate) return null

		const packExists = props.resultPacks && props.resultPacks.length > 0
		const packTitle = state.packaging
			? "Result packaging is ongoing. Please allow up to 5 minutes for this process."
			: packExists
				? "Result data package already exists. Consider using it instead of re-packaging."
				: `Prepare a STILT-results package for station ${stationId} from ${fromDate} to ${toDate}`

		return (<span>
			{props.resultPacks && props.resultPacks.length > 0 &&
				<a
					className="btn btn-primary text-white"
					href={`downloadresults/${props.resultPacks[0]}`}
					title="Download the latest-packaged result for this station and temporal scope"
				>
					<i className="fas fa-download" />
					{" Download results"}
				</a>
			}
			<a
				className={`btn btn-${packExists ? "warning" : "secondary"} text-white`}
				onClick={this.onPackageClick.bind(this)}
				style={state.packaging ? {cursor: 'default'} : {}}
				title={packTitle}
			>
				<i className="fas fa-archive" />
				{state.packaging ? " Packaging..." : ` ${packExists ? "Re-p" : "P" }ackage results`}
			</a>

		</span>);
	}
}
