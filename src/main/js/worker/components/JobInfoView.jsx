import React, { Component } from 'react';

export default class JobInfoView extends Component {
	constructor(props) {
		super(props);
		this.state = {expanded: false};
	}

	handleClick(){
		this.setState({expanded: !this.state.expanded});
	}

	render() {
		const jinfo = this.props.jobInfo;
	
		const par = jinfo.run.parallelism;
		const job = jinfo.run.job;
		const status = jinfo.status
	
		return <li>
			<div className="panel panel-default">
				<div className="panel-heading" onClick={this.handleClick.bind(this)} style={{cursor: "pointer"}}>
					{`${status.id}: ${job.siteId} from ${job.start} to ${job.stop} on ${par} cores by ${jinfo.executionNode}`}
				</div>
				{
				this.state.expanded
					? <div className="panel-body">
							<OutputStrings title="Standard output" stylecontext="success" strings={status.output}/>
							<OutputStrings title="Errors" stylecontext="danger" strings={status.errors}/>
							<OutputStrings title="STILT logs (merged, last 200 lines only)" stylecontext="info" strings={status.logs}/>
						</div>
					: null
				}
			</div>
		</li>;
	}
}

const OutputStrings = props => {
	const strings = props.strings.map(s => s.trim()).filter(s => s.length);
	return strings.length
		? <div className={`panel panel-${props.stylecontext}`}>
			<div className="panel-heading">
				<h3 className="panel-title">{props.title}</h3>
			</div>
			<div className="panel-body">
			{
				strings.map((s, i) => <p key={"s_" + i}>{s}</p>)
			}
			</div>
		</div>
		: null;
}

