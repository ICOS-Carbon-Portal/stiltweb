import React, { Component } from 'react';
import JobInfoView from './JobInfoView.jsx';

export default class DashboardView extends Component {
	constructor(props) {
		super(props);
	}

	render() {
		const ds = this.props.dashboardState;

		return <WideRow>

			<JobInfoList title="Running computations" jobs={ds.running}/>
			<JobQueue jobs={ds.queue}/>
			<JobInfoList title="Finished computations" jobs={ds.done}/>

			<WideRow>
				<button className="btn btn-primary" onClick={this.props.showMap}>To the job starter</button>
			</WideRow>

		</WideRow>;
	}
}

const JobInfoList = props => props.jobs.length
	? <InfoPanelWithList title={props.title}>{
				props.jobs.map((jinfo, i) => <JobInfoView jobInfo={jinfo} key={jinfo.status.id + '_' + i}/>)
		}</InfoPanelWithList>
	: null;

const WideRow = props => <div className="row">
	<div className="col-md-12">
		{props.children}
	</div>
</div>;

const InfoPanelWithList = props => <div className="panel panel-info">
	<div className="panel-heading">
		<h3 className="panel-title">{props.title}</h3>
	</div>
	<div className="panel-body">
		<ul className="list-unstyled">{props.children}</ul>
	</div>
</div>;

const JobQueue = props => props.jobs.length
	? <InfoPanelWithList title="Job queue">{
				props.jobs.map((job, i) => <JobView job={job} key={job.siteId + '_' + i}/>)
		}</InfoPanelWithList>
	: null;

const JobView = props => {
	const job = props.job;
	return <li>
		<span className="label label-default">{`${job.siteId} (lat/lon/alt: ${job.lat} / ${job.lon} / ${job.alt}) from ${job.start} to ${job.stop}`}</span>
	</li>;
};
