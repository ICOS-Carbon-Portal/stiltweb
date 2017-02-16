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
			<JobInfoList title="Finished computations" jobs={ds.done}/>

			<WideRow>
				<button className="btn btn-primary" onClick={this.props.showMap}>To the job starter</button>
			</WideRow>

		</WideRow>;
	}
}

const JobInfoList = props => props.jobs.length
	? <div className="panel panel-info">
		<div className="panel-heading">
			<h3 className="panel-title">{props.title}</h3>
		</div>
		<div className="panel-body">
			<ul className="list-unstyled">{
				props.jobs.map((jinfo, i) => <JobInfoView jobInfo={jinfo} key={jinfo.status.id + '_' + i}/>)
			}</ul>
		</div>
	</div>
	: null;

const WideRow = props => <div className="row">
	<div className="col-md-12">
		{props.children}
	</div>
</div>;

