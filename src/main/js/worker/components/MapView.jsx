import React, { Component } from 'react';
import StationsMap from '../../common/components/LMap.jsx';
import Select from '../../common/components/Select.jsx';
import TextInput from '../components/TextInput.jsx';
import DatePickerWrapper from './DatePickerWrapper.jsx';
import {cardHeaderInfo} from '../containers/App.jsx';
import { copyprops } from 'icos-cp-utils';
import config from '../config.js'

const marginBottom = 30;

export default class MapView extends Component {
	constructor(props) {
		super(props);
	}

	getJobDefUpdater(prop){
		const props = this.props
		return function(value){
			props.updateJobDef({propertyName: prop, value})
		}
	}

	onLoadDataBtnClick(){
		this.props.useExistingStationData();
	}

	render() {
		const props = this.props;
		const {disableLatLonAlt} = props

		const {start, stop, minDate, maxDate, disabledDates, disabledMonths} = getDatesFromProps(props);
		const selectedStation = props.stations.find(s =>
			s.siteId == props.siteId &&
			s.lat == props.lat &&
			s.lon == props.lon &&
			s.alt == props.alt
		)

		const showJobSubmProblems = props.jobSubmissionObstacles.length > 0 && (
			!isNaN(props.lat) || !isNaN(props.lon) || !isNaN(props.alt) || props.siteId || props.start || props.stop
		)

		const labelStyle = {display: 'block', clear: 'both'};
		const verticalMargin = {marginBottom: 20};
		const ds = props.dashboardState;
		const ccUpdater = this.getJobDefUpdater('countryCode')

		return <div className="row">

			<div className="col-md-7 col-sm-12" style={{marginBottom}}>
				<h4>Existing STILT stations</h4>

				<div className="card card-secondary">
					<div className="card-body">

						<div style={{marginBottom: 10}}>
							<Select
								selectValue={props.selectStation}
								infoTxt="Select station here or on the map"
								availableValues={props.stations}
								value={selectedStation}
								presenter={station => station ? `${station.siteId} (${station.siteName})` : station}
								sort={true}
							/>
						</div>

						<div ref={(div) => {this.mapDiv = div;}} style={{width: '100%', height: 1000}}>
							<StationsMap
								workerMode={true}
								stations={props.stations}
								selectedStation={copyprops(props, ['lat', 'lon', 'alt', 'siteId'])}
								action={props.selectStation}
								toastWarning={props.toastWarning}
								toastError={props.toastError}
							/>
						</div>
					</div>
				</div>
			</div>

			<div className="col">
				<div className="row">
					<div className="col" style={{minWidth: 310, marginBottom}}>
						<h4>Start new STILT run</h4>

						<div className="card card-secondary">
							<div className="card-body">

								<label style={labelStyle}>Site id (letter code + altitude)</label>
								<div className="input-group" style={verticalMargin}>
									<TextInput value={props.siteId || ''} action={this.getJobDefUpdater('siteId')} converter={s => s.toUpperCase()} maxLength="8"/>
									<button className="btn btn-primary cp-pointer"
										onClick={this.onLoadDataBtnClick.bind(this)}
										disabled={!disableLatLonAlt}>Load data</button>
								</div>

								<label style={labelStyle}>Site name</label>
								<TextInput style={verticalMargin} value={props.siteName || ''} action={this.getJobDefUpdater('siteName')} maxLength="100"/>

								<label style={labelStyle}>ICOS station id</label>
								<TextInput style={verticalMargin} value={props.icosId || ''} action={this.getJobDefUpdater('icosId')} maxLength="6"/>

								<label htmlFor="ccSelect" style={labelStyle}>Country code</label>
								<select style={verticalMargin} className="form-control" id="ccSelect" value={props.countryCode || ''} onChange={e => ccUpdater(e.target.value)}>
									<option value="">&mdash; Select &mdash;</option>
									{config.europeanCountries.map(c => (
										<option key={c.code} value={c.code}>{c.code} &mdash; {c.name}</option>
									))}
								</select>

								<label style={labelStyle}>Latitude (decimal degree)</label>
								<TextInput style={verticalMargin} value={props.lat} action={this.getJobDefUpdater('lat')}
										converter={validateLatLngVal(-90, 90)} disabled={disableLatLonAlt}/>

								<label style={labelStyle}>Longitude (decimal degree)</label>
								<TextInput style={verticalMargin} value={props.lon} action={this.getJobDefUpdater('lon')}
										converter={validateLatLngVal(-180, 180)} disabled={disableLatLonAlt}/>

								<label style={labelStyle}>Altitude above ground (meters)</label>
								<TextInput style={verticalMargin} value={props.alt} action={this.getJobDefUpdater('alt')} converter={toInt} disabled={disableLatLonAlt}/>

								<label style={labelStyle}>Start date (YYYY-MM-DD)</label>
								<DatePickerWrapper
									name="start"
									minDate={minDate}
									maxDate={maxDate}
									style={verticalMargin}
									value={start}
									siblingValue={stop}
									updateDates={props.updateDates}
									disabledDates={disabledDates}
									disabledMonths={disabledMonths}
									toastWarning={props.toastWarning}
									toastError={props.toastError}
								/>

								<label style={labelStyle}>End date (YYYY-MM-DD)</label>
								<DatePickerWrapper
									name="stop"
									minDate={minDate}
									maxDate={maxDate}
									style={verticalMargin}
									value={stop}
									siblingValue={start}
									updateDates={props.updateDates}
									disabledDates={disabledDates}
									disabledMonths={disabledMonths}
									toastWarning={props.toastWarning}
									toastError={props.toastError}
								/>

								{showJobSubmProblems &&
									<div className="alert alert-warning" role="alert">
										{props.jobSubmissionObstacles.join('; ')}
									</div>
								}

								<button className="btn btn-primary cp-pointer"
									disabled={props.jobSubmissionObstacles.length > 0}
									onClick={props.startJob}>Submit STILT job</button>

							</div>
						</div>
					</div>

					<div className="col" style={{minWidth: 310, marginBottom}}>
						<h4>Submitted STILT jobs</h4>

						<div className="card card-secondary">
							<div className="card-body">
								<button style={{display: 'block', clear: 'both', marginBottom: 20}} className="btn btn-primary cp-pointer" onClick={props.showDashboard}>Show details</button>
								{ ds.queue && (ds.queue.length || ds.done.length || ds.running.length)
									? <div>
										<JobList title="Job queue" user={props.currUser} jobs={ds.queue}/>
										<JobList title="Running computations" user={props.currUser} jobs={ds.running} />
										<JobList title="Finished computations" user={props.currUser} jobs={ds.done} />
									</div>
									: <div>No jobs have been submitted</div>
								}
							</div>
						</div>

					</div>
				</div>
			</div>
		</div>;
	}
}

const JobList = props => props.jobs.length
	? <div className="card mb-4">
		<div className={cardHeaderInfo}>
			{props.title}
		</div>
		<div className="card-body">{
			props.jobs.map(job => <JobLabel user={props.user} job={job.job} key={job.job.id} />)
		}
		</div>
	</div>
	: null;

const JobLabel = props => {
	const job = props.job;
	const lbl = {
		txt: `Site '${job.siteId}' (${job.start} - ${job.stop})`,
		cls: "bg-dark bg-opacity-50 fw-bold text-white",
		title: `Site Id: ${job.siteId}
Latitude: ${job.lat}
Longitude: ${job.lon}
Altitude: ${job.alt}
From: ${job.start}
To: ${job.stop}`
	};

	const myJob = props.user ? props.user.email === job.userId : undefined;
	const alertCls = "cp-help alert alert-dark p-1 d-inline-block";

	if (myJob === undefined){
		return (
			<div>
				<span title={lbl.title} className={alertCls}>{lbl.txt}</span>
			</div>
		);
	}

	return (
		<div>
			<span title={lbl.title} className={alertCls}>
				<i className="fas fa-star" style={{ fontSize: 10, top: -2, position: 'relative', marginRight: 5}} />{lbl.txt}
			</span>
		</div>
	);
};

function getDatesFromProps(props){
	return {
		start: props.start,
		stop: props.stop,
		minDate: props.availableMonths ? props.availableMonths.min : undefined,
		maxDate: props.availableMonths ? props.availableMonths.max : undefined,
		disabledDates: props.availableMonths ? props.availableMonths.disabledDates : undefined,
		disabledMonths: props.availableMonths ? props.availableMonths.disabledMonths : undefined,
	}
}

function validateLatLngVal(min, max){
	return str => {
		if (str === undefined || str === null || str.length === 0) return NaN;

		// Force '.' as decimal character and remove duplicate decimal character
		const cleanedStr = str.replace(',', '.').split('.').slice(0, 2).join('.');

		const res = parseFloat(parseFloat(cleanedStr).toFixed(2));
		if (!isNumber(res)) throw new Error("This is not a number");
		else if (res < min || res > max) throw new Error("The position lies outside of allowed range");
		else if (cleanedStr.match(/\.$/) || cleanedStr.match(/\.\d0$/) || cleanedStr.match(/\.0+$/)) return cleanedStr;
		else if (res.toString() !== cleanedStr) throw new Error("The number is not in a canonical format");
		else return res;
	}
}

function toInt(str){
	if (str === undefined || str === null || str.length === 0) return NaN;

	const res = parseInt(str);
	if(!isNumber(res)) throw new Error("This is not a number");
	else if(res.toString() !== str || res <= -1) throw new Error("The number is not a non-negative integer");
	else return res;
}

function isNumber(n){
	return !isNaN(parseFloat(n)) && isFinite(n);
}

