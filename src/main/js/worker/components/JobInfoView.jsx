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

		// console.log({props, job, status, jobId, allowCancel});

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

	return <span>
		<span><b>Site id: <i>{job.siteId}</i></b></span>
		<span>{` - Submitted ${job.jobStart} by ${job.userId}
			(lat: ${job.lat}, lon: ${job.lon}, alt: ${job.alt}, start: ${job.start}, stop: ${job.stop})`}
		</span>
	</span>;
};

const Queue = props => {
	const job = props.job;

	return(
		<div className="panel-body">
			<InfoPanelWithList title="Job parameters">
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
			<InfoPanelWithList title="Job parameters">
				{status.exitValue === 0
					? <div style={{marginBottom: 10}}>
						<div>Calculation started {job.jobStart} and finished {job.jobStop}</div>
						<div>
							View results <a target="_blank" href={"/viewer/" + status.id + "/"}>here</a>
						</div>
					</div>
					: null
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
		<div>{props.children}</div>
	</div>
</div>;

