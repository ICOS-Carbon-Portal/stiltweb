import React, { Component } from 'react';
import StationsMap from '../../common/components/LMap.jsx';
import Select from '../../common/components/Select.jsx';
import TextInput from '../components/TextInput.jsx';
import StationInfo from '../models/StationInfo';
import DatesValidation from '../models/DatesValidation';
import InfiniteCalendar from 'react-infinite-calendar';
import 'react-infinite-calendar/styles.css';

const geoBoundary = {
	latMin: 33,
	lonMin: -15,
	latMax: 72.9166666666667,
	lonMax: 34.875
};

export default class MapView extends Component {
	constructor(props) {
		super(props);
		this.state = {
			startCalVisible: false,
			stopCalVisible: false
		};
	}

	getJobdefUpdater(prop){
		const props = this.props;
		return function(update){
			props.updateJobdef(Object.assign({propertyName: prop}, update));
		};
	}

	onLoadDataBtnClick(){
		this.props.useExistingStationData();
	}

	toggleCalendar(cal){
		this.setState({[cal]: !this.state[cal]});
	}

	onStartDateSelected(date){
		const dates = new DatesValidation(date, this.props.workerData.formData.stop);
		setTimeout(() => this.toggleCalendar('startCalVisible'), 1);
		this.props.updateDates(dates);
	}

	onStopDateSelected(date){
		const dates = new DatesValidation(this.props.workerData.formData.start, date);
		setTimeout(() => this.toggleCalendar('stopCalVisible'), 1);
		this.props.updateDates(dates);
	}

	render() {
		const props = this.props;
		const formData = props.workerData.formData;
		const errors = props.workerData.errors;
		const isExisting = props.workerData.selectedStation.isExisting;
		const selectedStation = props.workerData.isFormAndExistingStationDifferent
			? new StationInfo(formData.lat, formData.lon)
			: props.workerData.selectedStation;

		const labelStyle = {display: 'block', clear: 'both'};
		const buttonStyle = {display: 'block', clear: 'both', marginTop: 40};
		const verticalMargin = {marginBottom: 20};
		const ds = this.props.dashboardState;
		const calStyle = {position:'absolute', left: -5, display:'inline', zIndex: 9, boxShadow: '7px 7px 5px #888'};
		const startCalStyle = this.state.startCalVisible ? calStyle : {display:'none'};
		const stopCalStyle = this.state.stopCalVisible ? calStyle : {display:'none'};

		// console.log({props, formData, form: props.workerData._workerFormData, selSt: props.workerData._selectedStation,
		// 	hasErrors: props.workerData.hasErrors, errors, isJobDefComplete: props.workerData.isJobDefComplete, jobDef: props.workerData.jobDef
		// });

		return <div className="row">

			<div className="col-md-8">
				<h4>Existing STILT footprints</h4>

				<div className="panel panel-default">
					<div className="panel-body">

						<div style={{marginBottom: 10}}>
							<Select
								selectValue={props.selectStation}
								infoTxt="Select station here or on the map"
								availableValues={props.workerData.stations}
								value={props.workerData.selectedStation}
								presenter={station => station ? `${station.siteId} (${station.name})` : station}
								sort={true}
							/>
						</div>

						<div style={{width: '100%', height: 600}}>
							<StationsMap
								workerMode={true}
								stations={props.workerData.stations}
								selectedStation={selectedStation}
								action={props.selectStation}
								toastWarning={props.toastWarning}
								toastError={props.toastError}
								geoBoundary={geoBoundary}
							/>
						</div>
					</div>
				</div>
			</div>

			<div className="col-md-2" style={{minWidth: 310}}>
				<h4>Create new STILT footprint</h4>

				<div style={startCalStyle}>
					<InfiniteCalendar
						locale={{
							weekStartsOn: 1
						}}
						displayOptions={{
							shouldHeaderAnimate: false
						}}
						width={320}
						height={420}
						onSelect={this.onStartDateSelected.bind(this)}
						min={new Date('2000-01-01')}
						max={new Date()}
						maxDate={new Date()}
					/>
				</div>

				<div style={stopCalStyle}>
					<InfiniteCalendar
						locale={{
							weekStartsOn: 1
						}}
						displayOptions={{
							shouldHeaderAnimate: false
						}}
						width={320}
						height={420}
						onSelect={this.onStopDateSelected.bind(this)}
						min={new Date('2000-01-01')}
						max={new Date()}
						maxDate={new Date()}
					/>
				</div>

				<div className="panel panel-default">
					<div className="panel-body">

						<label style={labelStyle}>Latitude (decimal degree)</label>
						<TextInput style={verticalMargin} value={formData.lat} action={this.getJobdefUpdater('lat')} converter={toLat} disabled={isExisting}/>

						<label style={labelStyle}>Longitude (decimal degree)</label>
						<TextInput style={verticalMargin} value={formData.lon} action={this.getJobdefUpdater('lon')} converter={toLon} disabled={isExisting}/>

						<label style={labelStyle}>Altitude above ground (meters)</label>
						<TextInput style={verticalMargin} value={formData.alt} action={this.getJobdefUpdater('alt')} converter={toInt} disabled={isExisting}/>

						<label style={labelStyle}>Site id (usually a 3 letter code)</label>
						<div className="input-group" style={verticalMargin}>
							<TextInput value={formData.siteId} action={this.getJobdefUpdater('siteId')} converter={s => s.toUpperCase()} maxLength="5"/>
							<span className="input-group-btn">
								<button className="btn btn-primary cp-pointer"
										onClick={this.onLoadDataBtnClick.bind(this)}
										disabled={!isExisting}>Load data</button>
							</span>
						</div>

						<label style={labelStyle}>Start date (YYYY-MM-DD)</label>
						<TextInput
							style={verticalMargin}
							maxLength="10"
							value={formData.start}
							error={errors.start}
							onClick={this.toggleCalendar.bind(this, 'startCalVisible')}
						/>

						<label style={labelStyle}>End date (YYYY-MM-DD)</label>
						<TextInput
							style={verticalMargin}
							maxLength="10"
							value={formData.stop}
							error={errors.stop}
							onClick={this.toggleCalendar.bind(this, 'stopCalVisible')}
						/>

						<button style={buttonStyle}
								className="btn btn-primary cp-pointer"
								disabled={!props.workerData.isJobDefComplete || !props.currUser.email}
								onClick={props.startJob}>Dispatch STILT job</button>

					</div>
				</div>
			</div>

			<div className="col-md-2" style={{minWidth: 310}}>
				<h4>Dispatched STILT jobs</h4>

				<div className="panel panel-default">
					<div className="panel-body">
						{ ds.queue.length || ds.done.length || ds.running.length
							? <div>
								<Job title="Job queue" jobs={ds.queue}/>
								<Job title="Running computations" jobs={ds.running} />
								<Job title="Finished computations" jobs={ds.done} />
								<button style={buttonStyle} className="btn btn-primary cp-pointer" onClick={props.showDashboard}>Show details</button>
							</div>
							: <div>No jobs have been submitted</div>
						}
					</div>
				</div>

			</div>
		</div>;
	}
}

const Job = props => props.jobs.length
	? <div className="panel panel-info">
		<div className="panel-heading">
			<h3 className="panel-title">{props.title}</h3>
		</div>
		<div className="panel-body">{
			props.jobs.map((job, i) => {
				const key = job.run ? job.run.job.siteId + '_' + i : job.siteId + '_' + i;
				return <JobLabel type={props.type} job={job} key={key} />
			})
		}
		</div>
	</div>
	: null;

const JobLabel = props => {
	const isQueue = !props.job.run;
	const jobDef = isQueue ? props.job : props.job.run.job;
	const status = props.job.status;
	const lbl = {
		txt: "Site '" + jobDef.siteId + "'",
		cls: "label label-default",
		title: `Site Id: ${jobDef.siteId}
Latitude: ${jobDef.lat}
Longitude: ${jobDef.lon}
Altitude: ${jobDef.alt}
From: ${jobDef.start}
To: ${jobDef.stop}`
	};

	if (isQueue){
		lbl.txt += " enqueued";
	} else {
		if (isNumber(status.exitValue)){
			if (status.exitValue === 0){
				lbl.txt += " is done";
				lbl.cls += "label label-success";
			} else {
				lbl.txt += " failed";
				lbl.cls += "label label-danger";
			}
		} else {
			lbl.txt += " is running";
		}
	}

	return <h4><span title={lbl.title} className={"cp-help " + lbl.cls}>{lbl.txt}</span></h4>;
};

function toLat(str){
	if (str.length == 0) return str;

	const res = parseFloat(parseFloat(str).toFixed(2));

	if (!isNumber(res)) throw new Error("This is not a number");
	else if (res < geoBoundary.latMin || res > geoBoundary.latMax) throw new Error("The position lies outside of boundary");
	else if (str.match(/\.$/) || str.match(/\.0+$/)) return str;
	else if(res.toString() != str) throw new Error("The number is not in a canonical format");
	else return res;
}

function toLon(str){
	if (str.length == 0) return str;

	const res = parseFloat(parseFloat(str).toFixed(2));

	if (!isNumber(res)) throw new Error("This is not a number");
	else if (res < geoBoundary.lonMin || res > geoBoundary.lonMax) throw new Error("The position lies outside of boundary");
	else if (str.match(/\.$/) || str.match(/\.0+$/)) return str;
	else if(res.toString() != str) throw new Error("The number is not in a canonical format");
	else return res;
}

function toInt(str){
	if (str.length == 0) return str;

	const res = parseInt(str);
	if(!isNumber(res)) throw new Error("This is not a number")
	else if(res.toString() != str || res <= -1) throw new Error("The number is not a non-negative integer")
	else return res;
}

function isNumber(n){
	return !isNaN(parseFloat(n)) && isFinite(n);
}