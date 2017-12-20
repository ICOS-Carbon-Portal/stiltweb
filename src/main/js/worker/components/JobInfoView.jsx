import React, { Component } from 'react';
import YesNoView from './YesNoView.jsx';

export default class JobInfoView extends Component {
	constructor(props) {
		super(props);
		this.state = {showCancelJobDialog: false};
	}

	toggleCancelJobDialog(){
		this.setState({showCancelJobDialog: !this.state.showCancelJobDialog});
	}

	confirmJobCancel(){
		this.props.cancelJob(this.props.jobInfo.job.id);
		this.toggleCancelJobDialog();
	}

	render() {
		const props = this.props;
		const jinfo = props.jobInfo;
		const job = jinfo.job;
		const allowCancel = !!props.cancelJob &&
			(props.currUser.email === job.userId || props.currUser.isAdmin);

		return <div className="panel panel-default">
			<div className="panel-heading">
				{allowCancel
					? <button className="btn btn-primary" onClick={this.toggleCancelJobDialog.bind(this)}>
						<span className="glyphicon glyphicon-remove-sign" style={{marginRight: 10, top: 3, fontSize:'130%'}} />
						{this.state.showCancelJobDialog ? "Keep running" : "Cancel job"}
					</button>
					: null
				}
				<span>
					<span> <b>Site id: <i>{job.siteId}</i></b> (<b>lat:</b> {job.lat}, <b>lon:</b> {job.lon}), </span>
					<span><b>alt:</b> {job.alt}, <b>start:</b> {job.start}, <b>stop:</b> {job.stop}, </span>
					<span><b>done:</b> {jinfo.nSlotsFinished} of {jinfo.nSlots}</span>
					<span> - submitted by {job.userId}</span>
				</span>
			</div>
			{allowCancel
				? <div className="panel-body">
					<YesNoView
						visible={this.state.showCancelJobDialog}
						title={'Cancel job'}
						question={'Are you sure you want to cancel this job?'}
						actionYes={this.confirmJobCancel.bind(this)}
						actionNo={this.toggleCancelJobDialog.bind(this)}
					/>
				</div>
				: null
			}
		</div>;
	}
}

