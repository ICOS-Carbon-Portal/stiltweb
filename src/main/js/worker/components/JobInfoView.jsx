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
		const allowCancel = !!props.cancelJob && !!props.toggleYesNoView
			&& (props.currUser.email === job.userId || props.currUser.isAdmin);

		console.log({props, job, status, jobId, allowCancel});

		return <div>
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
							<span className="glyphicon glyphicon-remove-sign" style={{marginRight: 10, top: 3, fontSize:'130%'}} />
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
		</div>;
	}
}

const StatusLabel = props => {
	const status = props.status;

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
	const job = props.job;
	const params = <span>
		<b>lat:</b> {job.lat}, <b>lon:</b> {job.lon}, <b>alt:</b> {job.alt}, <b>start:</b> {job.start}, <b>stop:</b> {job.stop}
	</span>;

	return <span>
		<span><b>Site id: <i>{job.siteId}</i></b></span>
		<span> - Submitted {job.logbook.enqueued} by {job.userId} ({params})</span>
	</span>;
};

const Queue = props => {
	const job = props.job;

	return(
		<div className="panel-body">
			<InfoPanelWithList title="Job definition">
				<div><b>Lat: </b>{job.lat}</div>
				<div><b>Lon: </b>{job.lon}</div>
				<div><b>Alt: </b>{job.alt}</div>
				<div><b>Start: </b>{job.start}</div>
				<div><b>Stop: </b>{job.stop}</div>
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
			<InfoPanelWithList title="Summary of job">
				{Number.isInteger(status.exitValue)
					? status.exitValue === 0
						? <div style={{marginBottom: 10}}>
							<div>Calculation started {job.logbook.start} and finished {job.logbook.stop}</div>
							<div><b>Runtime:</b> {getRuntime(job.logbook.start, job.logbook.stop)}</div>
							<div>
								View results <a target="_blank" href={"/viewer/" + status.id + "/"}>here</a>
							</div>
						</div>
						: <div style={{marginBottom: 10}}>
							<div>Calculation started {job.logbook.start} and failed {job.logbook.stop}</div>
							<div><b>Runtime:</b> {getRuntime(job.logbook.start, job.logbook.start)}</div>
						</div>
					: <div style={{marginBottom: 10}}>
						Calculation started {job.logbook.start}
					</div>
				}
				<div><b>Lat: </b>{job.lat}</div>
				<div><b>Lon: </b>{job.lon}</div>
				<div><b>Alt: </b>{job.alt}</div>
				<div><b>Start: </b>{job.start}</div>
				<div><b>Stop: </b>{job.stop}</div>
				<div><b>Number of cores: </b>{props.par}</div>
				<div><b>Execution node: </b>{jinfo.executionNode}</div>
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
			<div className="panel-body" style={{overflow: 'auto', maxHeight: 300}}>
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
		<div>{props.children}</div>
	</div>
</div>;

function getRuntime(start, stop){
	// start = new Date("2017-01-01 23:00");
	// stop = new Date("2017-01-03 00:50:20");

	if (!(start && stop)) return <span>Not defined</span>;

	start.setMinutes(start.getMinutes() - start.getTimezoneOffset());
	stop.setMinutes(stop.getMinutes() - stop.getTimezoneOffset());

	const msPerDay = 24 * 60 * 60 * 1000;
	const msRuntime = stop.getTime() - start.getTime();
	const runtime = new Date(msRuntime);

	const days = Math.floor(msRuntime / msPerDay);
	const hours = runtime.getUTCHours();
	const minutes = runtime.getUTCMinutes();
	const seconds = runtime.getUTCSeconds();

	return <span>
		{days > 1 ? days + " day, " : days + " day, "}
		{hours > 1 ? hours + " hours, " : hours + " hour, "}
		{minutes > 1 ? minutes + " minutes and " : minutes + " minute and "}
		{seconds > 1 ? seconds + " seconds" : seconds + " second"}
	</span>;
}
