import React, { Component } from 'react';
import JobInfoView from './JobInfoView.jsx';
import {cardHeaderInfo} from '../containers/App.jsx';

export default class DashboardView extends Component {
	constructor(props) {
		super(props);
	}

	render() {
		const props = this.props;
		const ds = props.dashboardState;

		return (
			<div>
				<WideRow show={true}>
					<CompInfraState infra={ds.infra} />
				</WideRow>

				<WideRow show={ds.queue.length > 0}>
					<JobInfoList
						title="Job queue"
						jobs={ds.queue}
						currUser={props.currUser}
						cancelJob={props.cancelJob}
					/>
				</WideRow>

				<WideRow show={ds.running.length > 0}>
					<JobInfoList
						title="Running computations"
						jobs={ds.running}
						currUser={props.currUser}
						cancelJob={props.cancelJob}
					/>
				</WideRow>
				
				<WideRow show={ds.done.length > 0}>
					<JobInfoList
						title="Finished computations"
						jobs={ds.done}
					/>
				</WideRow>

				<WideRow show={true}>
					<button className="btn btn-primary" onClick={this.props.showMap}>To the job starter</button>
				</WideRow>
			</div>
		);
	}
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

const WideRow = props => {
	if (!props.show)
		return null;

	return (
		<div className="row" style={{marginBottom: 20}}>
			<div className="col-md-12">
				{props.children}
			</div>
		</div>
	);
};

const InfoPanelWithList = props => <div className="card">
	<div className={cardHeaderInfo}>
		{props.title}
	</div>
	<div className="card-body">
		<div>{props.children}</div>
	</div>
</div>;

const CompInfraState = props => <InfoPanelWithList title="Computational resources">
	<table className="table" style={{marginBottom: 0}}>
		<thead>
			<tr><th>Node</th><th>Free CPUs</th><th>Total CPUs</th></tr>
		</thead>
		<tbody>{props.infra.map(nodeInfo =>
			<tr key={nodeInfo.address}>
				<td style={{borderBottomWidth: 0, borderTopWidth: 1}}>{nodeInfo.address}</td>
				<td style={{borderBottomWidth: 0, borderTopWidth: 1}}>{nodeInfo.nCpusFree}</td>
				<td style={{borderBottomWidth: 0, borderTopWidth: 1}}>{nodeInfo.nCpusTotal}</td>
			</tr>
		)}</tbody>
	</table>
</InfoPanelWithList>;

