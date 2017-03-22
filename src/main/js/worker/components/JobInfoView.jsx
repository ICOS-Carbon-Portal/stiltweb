import React, { Component } from 'react';
import YesNoView from './YesNoView.jsx';

export default class JobInfoView extends Component {
	constructor(props) {
		super(props);
		this.state = {
			expanded: false
		};
		this.mouseClick = undefined;
	}

	handleClick(){
		this.setState({expanded: !this.state.expanded});
	}

	handleCancelClick(ev){
		ev.stopPropagation();
		this.mouseClick = ev.nativeEvent;
		if (this.props.toggleYesNoView) this.props.toggleYesNoView();
	}

	render() {
		const props = this.props;
		const jinfo = this.props.jobInfo;
	
		const par = jinfo.run ? jinfo.run.parallelism : undefined;
		const job = jinfo.run ? jinfo.run.job : jinfo;
		const status = jinfo.status || undefined;
		const jobId = jinfo.status ? jinfo.status.id : jinfo.id;
		const allowCancel = props.cancelJob && props.toggleYesNoView;

		// console.log({props});

		return <li>
			{allowCancel
				? <YesNoView
					visible={props.yesNoViewVisible}
					mouseClick={this.mouseClick}
					title={'Cancel job'}
					question={'Are you sure you want to cancel this job?'}
					actionYes={{fn: props.cancelJob, args: [jobId]}}
					actionNo={{fn: props.toggleYesNoView}}
				/>
				: null
			}

			<div className="panel panel-default">
				<div className="panel-heading" onClick={this.handleClick.bind(this)} style={{cursor: "pointer"}}>
					{allowCancel
						? <button className="btn btn-primary" onClick={this.handleCancelClick.bind(this)}>
							<span className="glyphicon glyphicon-remove-sign" style={{marginRight: 10, top: 3, fontSize:'130%'}}></span>
							Cancel job
						</button>
						: null
					}
					<StatusLabel status={status} />
					<HeaderInfo job={job} />
				</div>
				{
				this.state.expanded
					? status
						? <RunningAndFinished status={status} job={job} jinfo={jinfo} par={par} />
						: <Queue job={job} />
					: null
				}
			</div>
		</li>;
	}
}

const StatusLabel = props => {
	const status = props.status;
	console.log({status})
	return <span style={{fontSize: '120%', position: 'relative', top: -2, marginRight: 20}}>
		{status && Number.isInteger(status.exitValue)
			? status.exitValue === 0
				? <span className="label label-success">Calculation finished</span>
				: <span className="label label-danger">Calculation failed</span>
			: null
		}
	</span>;
};

const HeaderInfo = props => {
	return <span>
		<span><b>Site id: <i>{props.job.siteId}</i></b></span>
		<span>{` from ${props.job.start} to ${props.job.stop} submitted by ${props.job.userId}`}</span>
	</span>;
};

const Queue = props => {
	const job = props.job;

	return(
		<div className="panel-body">
			<InfoPanelWithList title="Job parameters">
				<li><b>Lat: </b>{job.lat}</li>
				<li><b>Lon: </b>{job.lon}</li>
				<li><b>Alt: </b>{job.alt}</li>
				<li><b>Start: </b>{job.start}</li>
				<li><b>Stop: </b>{job.stop}</li>
			</InfoPanelWithList>
		</div>
	);
};

const RunningAndFinished = props => {
	const job = props.job;
	const status = props.status;
	const jinfo = props.jinfo;

	return(
		<div className="panel-body">
			<InfoPanelWithList title="Job parameters">
				<li style={{marginBottom: 10}}>{status.exitValue === 0
					? <span>
						Calculation finished, view results <a target="_blank" href={"/viewer/" + status.id + "/"}>here</a>
					</span>
					: null
				}</li>
				<li><b>Lat: </b>{job.lat}</li>
				<li><b>Lon: </b>{job.lon}</li>
				<li><b>Alt: </b>{job.alt}</li>
				<li><b>Start: </b>{job.start}</li>
				<li><b>Stop: </b>{job.stop}</li>
				<li><b>Number of cores: </b>{props.par}</li>
				<li><b>Execution node: </b>{jinfo.executionNode}</li>
			</InfoPanelWithList>
			<OutputStrings title="Standard output" stylecontext="success" strings={status.output}/>
			<OutputStrings title="Errors" stylecontext="danger" strings={status.errors}/>
			<OutputStrings title="STILT logs (merged, last 200 lines only)" stylecontext="info" strings={status.logs}/>
		</div>
	);
};

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
};

export const InfoPanelWithList = props => <div className="panel panel-info">
	<div className="panel-heading">
		<h3 className="panel-title">{props.title}</h3>
	</div>
	<div className="panel-body">
		<ul className="list-unstyled">{props.children}</ul>
	</div>
</div>;

