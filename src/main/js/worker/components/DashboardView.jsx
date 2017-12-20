import React, { Component } from 'react';
import JobInfoView from './JobInfoView.jsx';

export default class DashboardView extends Component {
	constructor(props) {
		super(props);
	}

	render() {
		const props = this.props;
		const ds = props.dashboardState;

		return <WideRow>

			<CompInfraState infra={ds.infra} />

			<JobInfoList
				title="Job queue"
				jobs={ds.queue.map(jobToJobInfo)}
				currUser={props.currUser}
				cancelJob={props.cancelJob}
			/>
			<JobInfoList
				title="Running computations"
				jobs={ds.running}
				currUser={props.currUser}
				cancelJob={props.cancelJob}
			/>
			<JobInfoList
				title="Finished computations"
				jobs={ds.done}
			/>

			<WideRow>
				<button className="btn btn-primary" onClick={this.props.showMap}>To the job starter</button>
			</WideRow>

		</WideRow>;
	}
}

function jobToJobInfo(job){
	return {job, nSlots: undefined, nSlotsFinished: 0};
}

const JobInfoList = props => props.jobs.length
	? <InfoPanelWithList title={props.title}>{
				props.jobs.map(jinfo => {
					return <JobInfoView
						currUser={props.currUser}
						cancelJob={props.cancelJob}
						jobInfo={jinfo}
						key={jinfo.job.id}
					/>;
				})
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
		<div>{props.children}</div>
	</div>
</div>;

const CompInfraState = props => <InfoPanelWithList title="Computational resources">
	<table className="table">
		<thead>
			<tr><th>Node</th><th>Free CPUs</th><th>Total CPUs</th></tr>
		</thead>
		<tbody>{props.infra.map(nodeInfo =>
			<tr key={nodeInfo.address}><td>{nodeInfo.address}</td><td>{nodeInfo.nCpusFree}</td><td>{nodeInfo.nCpusTotal}</td></tr>
		)}</tbody>
	</table>
</InfoPanelWithList>;

