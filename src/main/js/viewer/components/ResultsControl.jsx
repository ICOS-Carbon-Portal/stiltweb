import React, { Component } from 'react';
import _ from 'lodash';
import { packageResults } from '../backend';

export default class ResultsControl extends Component{
	constructor(props){
		super(props);

		this.state = {
			packaging: false
		}
	}

	onPackageClick(){
		if(this.state.packaging) return
		const self = this
		const {props} = this
		const {resultBatch} = props
		this.setState({packaging: true, resultBatch})
		packageResults(resultBatch)
			.then(
				resPacks => {
					const curResultBatch = self.props.resultBatch
					if(_.isEqual(curResultBatch, resultBatch)) props.updateResultPacks(resPacks)
				},
				props.failWithError
			)
			.finally(() => self.setState({packaging: false}))
	}

	render(){
		const {props, state} = this
		const {resultBatch} = props
		if(!resultBatch.stationId || !resultBatch.fromDate || !resultBatch.toDate) return null

		const packExists = props.resultPacks && props.resultPacks.length > 0
		function forBatch(resBatch){
			return `for station ${resBatch.stationId} from ${resBatch.fromDate} to ${resBatch.toDate}`
		}
		const packTitle = state.packaging
			? `Result packaging is ongoing ${forBatch(state.resultBatch)}. Please allow up to 5 minutes for this process.`
			: packExists
				? "Result data package already exists. Consider using it instead of re-packaging."
				: `Prepare a STILT-results package ${forBatch(resultBatch)}`

		return (<span>
			{props.resultPacks && props.resultPacks.length > 0 &&
				<a
					className="btn btn-primary text-white mb-1"
					href={`downloadresults/${props.resultPacks[0]}`}
					title={`Download the latest-packaged result ${forBatch(resultBatch)}`}
				>
					<i className="fas fa-download" />
					{" Download results"}
				</a>
			}
			<a
				className={`btn btn-${packExists || state.packaging ? "warning" : "secondary"} text-white mb-1`}
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
