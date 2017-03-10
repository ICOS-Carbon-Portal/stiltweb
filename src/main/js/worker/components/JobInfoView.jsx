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
					{`${status.id}: ${job.siteId} from ${job.start} to ${job.stop} by ${job.userId}`}
				</div>
				{
				this.state.expanded
					? <div className="panel-body">
							<InfoPanelWithList title="Calculation status">
								<li>{status.exitValue === 0
									? <span>Calculation finished, view results <a target="_blank" href={"/viewer/" + status.id + "/"}>here</a></span>
									: Number.isInteger(status.exitValue)
										? <span>Calculation failed</span>
										: <span>Calculation is running</span>
								}</li>
								<li><b>Lat: </b>{job.lat}</li>
								<li><b>Lon: </b>{job.lon}</li>
								<li><b>Alt: </b>{job.alt}</li>
								<li><b>Number of cores: </b>{par}</li>
								<li><b>Execution node: </b>{jinfo.executionNode}</li>
							</InfoPanelWithList>
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


export const InfoPanelWithList = props => <div className="panel panel-info">
	<div className="panel-heading">
		<h3 className="panel-title">{props.title}</h3>
	</div>
	<div className="panel-body">
		<ul className="list-unstyled">{props.children}</ul>
	</div>
</div>;

