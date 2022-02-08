import React, { Component } from 'react';
import YesNoView from './YesNoView.jsx';
import config from '../config';

export default class JobInfoView extends Component {
	constructor(props) {
		super(props);
		this.state = {showCancelJobDialog: false, showErrors: false};
	}

	toggleCancelJobDialog(evnt){
		this.setState({showCancelJobDialog: !this.state.showCancelJobDialog});
	}

	toggleShowErrors(){
		this.setState({showErrors: !this.state.showErrors});
	}

	confirmJobCancel(){
		this.props.cancelJob(this.props.jobInfo.job.id);
		this.toggleCancelJobDialog();
	}

	render() {
		const props = this.props;
		const {showCancelJobDialog} = this.state;
		const jinfo = props.jobInfo;
		const job = jinfo.job;
		const allowCancel = !!props.cancelJob &&
			(props.currUser.email === job.userId || props.currUser.isAdmin);
		const showLink = (jinfo.nSlotsFinished === jinfo.nSlots);
		const hasFailures = !!jinfo.failures.length;
		const failuresMustBeShown = hasFailures && this.state.showErrors;

		return <div className={"mb-3 card text-dark bg-" + (hasFailures? "warning" : "light")}>
			<div className="card-header" onClick={this.toggleShowErrors.bind(this)} style={hasFailures ? {cursor: 'pointer'} : {}}>
				{allowCancel
					? <button className="btn btn-primary" style={{marginRight: 10}} onClick={this.toggleCancelJobDialog.bind(this)}>
						<Icon name="times-circle"/>
						{showCancelJobDialog ? "Keep running" : "Cancel job"}
					</button>
					: null
				}
				{showLink
					? <a href={config.scopedViewLink(job)} target="_blank" onClick={e => e.stopPropagation()}>
						<button className="btn btn-primary" style={{marginRight: 10}}><Icon name="info-circle"/>View results</button>
					</a>
					: null
				}
				<span>
					<span> <b>Site id: <i>{job.siteId}</i></b> (<b>lat:</b> {job.lat}, <b>lon:</b> {job.lon}), </span>
					<span><b>alt:</b> {job.alt}, <b>start:</b> {job.start}, <b>stop:</b> {job.stop}, </span>
					<span><b>done:</b> {jinfo.nSlotsFinished} of {jinfo.nSlots}</span>
					<span> - submitted by {job.userId}</span>
				</span>
			</div>
			{showCancelJobDialog || failuresMustBeShown
				? <div className="card-body">
					<YesNoView
						visible={showCancelJobDialog}
						title={'Cancel job'}
						question={'Are you sure you want to cancel this job?'}
						actionYes={this.confirmJobCancel.bind(this)}
						actionNo={this.toggleCancelJobDialog.bind(this)}
					/>
					<FailureList failures={jinfo.failures} visible={failuresMustBeShown}/>
				</div>
				: null
			}
		</div>;
	}
}
const Icon = ({name}) => <i className={`fas fa-${name}`} style={{marginRight: 10, top: 3, fontSize:'130%'}} />;

const FailureList = props => props.visible
	? <div className="card text-dark bg-light">
		<div className="card-header">Slot calculation failures</div>
		<div className="card-body">
			<table className="table">
				<thead>
					<tr><th>Slots</th><th>Error messages</th><th>Logs</th></tr>
				</thead>
				<tbody>{props.failures.map(({slot, errorMessages, logsFilename}) =>
					<tr key={logsFilename}>
						<td>{timeStr(slot.time)}</td>
						<td>{
							errorMessages.map((msg, i) => <div key={'i'+i}>{msg}</div>)
						}</td>
						<td><a href={config.workerOutputDir + logsFilename} target="_blanck">logs</a></td>
					</tr>
				)}</tbody>
			</table>
		</div>
	</div>
	: null;

const timeStr = ({year, month, day, hour}) => {
	return `${year}-${pad(month)}-${pad(day)} ${pad(hour)}`;
}

function pad(num){
	return (num > 9 ? '' : '0') + num;
}
