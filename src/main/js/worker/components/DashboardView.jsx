import React, { Component } from 'react';
import JobInfoView, {InfoPanelWithList} from './JobInfoView.jsx';

export default class DashboardView extends Component {
	constructor(props) {
		super(props);
	}

	render() {
		const props = this.props;
		const ds = props.dashboardState;

		return <WideRow>

			<JobInfoList
				title="Job queue"
				jobs={ds.queue}
				currUser={props.currUser}
				toggleYesNoView={props.toggleYesNoView}
				cancelJob={props.cancelJob}
				yesNoViewVisible={props.yesNoViewVisible}
			/>
			<JobInfoList
				title="Running computations"
				jobs={ds.running}
				currUser={props.currUser}
				toggleYesNoView={props.toggleYesNoView}
				cancelJob={props.cancelJob}
				yesNoViewVisible={props.yesNoViewVisible}
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

const JobInfoList = props => props.jobs.length
	? <InfoPanelWithList title={props.title}>{
				props.jobs.map(jinfo => {
					return <JobInfoView
						currUser={props.currUser}
						toggleYesNoView={props.toggleYesNoView}
						yesNoViewVisible={props.yesNoViewVisible}
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
