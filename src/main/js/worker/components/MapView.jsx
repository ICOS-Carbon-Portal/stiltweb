import React, { Component } from 'react';
import StationsMap from '../../common/components/LMap.jsx';
import Select from '../../common/components/Select.jsx';
import TextInput from '../components/TextInput.jsx';
import StationInfo from '../models/StationInfo';
import DatesValidation from '../models/DatesValidation';
import {DatePickerInput} from 'rc-datepicker';


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
			startInvalid: false,
			stopInvalid: false
		};

		this.startInput = undefined;
		this.stopInput = undefined;
		this.onKeyUpHandler = this.onKeyUp.bind(this);
	}

	getJobDefUpdater(prop){
		const props = this.props;
		return function(update){
			props.updateJobDef(Object.assign({propertyName: prop}, update));
		};
	}

	onLoadDataBtnClick(){
		this.props.useExistingStationData();
	}

	onStartDateSelected(dateObj, dateString){
		if (dateString === 'Invalid date') {
			this.setState({startInvalid: true});
			return;
		} else if (this.state.startInvalid) {
			this.setState({startInvalid: false});
		}

		const {stop, minDate, maxDate, disabledDates} = getDatesFromProps(this.props);
		const dates = new DatesValidation(dateString, stop, minDate, maxDate, disabledDates);
		if (dates.startError) this.props.toastError(dates.startError);
		this.props.updateDates(dates);
	}

	onStopDateSelected(dateObj, dateString){
		if (dateString === 'Invalid date') {
			this.setState({stopInvalid: true});
			return;
		} else if (this.state.stopInvalid) {
			this.setState({stopInvalid: false});
		}

		const {start, minDate, maxDate, disabledDates} = getDatesFromProps(this.props);
		const dates = new DatesValidation(start, dateString, minDate, maxDate, disabledDates);
		if (dates.stopError) this.props.toastError(dates.stopError);
		this.props.updateDates(dates);
	}

	componentDidMount(){
		// Add handler for emptying date picker input
		const datePickerInputStart = this.datePickerInputStart;
		const datePickerInputStop = this.datePickerInputStop;

		this.startInput = datePickerInputStart.getDatePickerInput().firstChild.firstChild;
		this.stopInput = datePickerInputStop.getDatePickerInput().firstChild.firstChild;

		this.startInput.setAttribute('sender', 'start');
		this.stopInput.setAttribute('sender', 'stop');

		this.startInput.addEventListener('keyup', this.onKeyUpHandler);
		this.stopInput.addEventListener('keyup', this.onKeyUpHandler);
	}

	onKeyUp(evt){
		if (evt.target.value === '') {
			const sender = evt.target.getAttribute('sender');

			if (sender === 'start'){
				this.onStartDateSelected();
			} else if (sender === 'stop'){
				this.onStopDateSelected();
			}
		}
	}

	componentWillUnmount(){
		this.startInput.removeEventListener("keyup", this.onKeyUpHandler);
		this.stopInput.removeEventListener("keyup", this.onKeyUpHandler);
		// document.body.removeEventListener('click', this.bound_onClick);
	}

	onDateSet(sender, dateObj, dateString){
		if (dateString === 'Invalid date') {
			this.setState({[sender + 'Invalid']: true});
			return;
		} else if (this.state[sender + 'Invalid']) {
			this.setState({[sender + 'Invalid']: false});
		}

		// Set time to midnight
		const selectedDate = dateString === undefined ? undefined : new Date(dateString);
		const {category, filterTemporal, setFilterTemporal} = this.props;
		let newFilter = undefined;

		if (sender === 'start'){
			newFilter = category === 'dataTime'
				? filterTemporal.withDataTimeFrom(selectedDate)
				: filterTemporal.withSubmissionFrom(selectedDate);
		} else if (sender === 'stop'){
			newFilter = category === 'dataTime'
				? filterTemporal.withDataTimeTo(selectedDate)
				: filterTemporal.withSubmissionTo(selectedDate);
		} else {
			throw new Error('Unknown sender category: ' + sender);
		}

		if (newFilter && setFilterTemporal) setFilterTemporal(newFilter);
	}

	render() {
		const {startInvalid, stopInvalid} = this.state;
		const props = this.props;
		const formData = props.workerData.formData;
		const errors = props.workerData.errors;
		const errorStart = !!errors.start;
		const errorStop = !!errors.stop;
		const {start, stop, minDate, maxDate} = getDatesFromProps(props);
		const isExisting = props.workerData.selectedStation.isExisting;
		const selectedStation = props.workerData.isFormAndExistingStationDifferent
			? new StationInfo(formData.lat, formData.lon)
			: props.workerData.selectedStation;

		const labelStyle = {display: 'block', clear: 'both'};
		const buttonStyle = {display: 'block', clear: 'both', marginTop: 40};
		const verticalMargin = {marginBottom: 20};
		const ds = props.dashboardState;
		console.log({lat: formData.lat, lon: formData.lon, selectedStation});
		// console.log({props, formData, minDate, maxDate, start, stop, disabledDates: props.availableMonths, hasErrors: props.workerData.hasErrors, errors});

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

						<div ref={(div) => {this.mapDiv = div;}} style={{width: '100%', height: 600}}>
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

				<div className="panel panel-default">
					<div className="panel-body">

						<label style={labelStyle}>Latitude (decimal degree)</label>
						<TextInput style={verticalMargin} value={formData.lat} action={this.getJobDefUpdater('lat')}
								   converter={validateLatLngVal(geoBoundary.latMin, geoBoundary.latMax)} disabled={isExisting}/>

						<label style={labelStyle}>Longitude (decimal degree)</label>
						<TextInput style={verticalMargin} value={formData.lon} action={this.getJobDefUpdater('lon')}
								   converter={validateLatLngVal(geoBoundary.lonMin, geoBoundary.lonMax)} disabled={isExisting}/>

						<label style={labelStyle}>Altitude above ground (meters)</label>
						<TextInput style={verticalMargin} value={formData.alt} action={this.getJobDefUpdater('alt')} converter={toInt} disabled={isExisting}/>

						<label style={labelStyle}>Site id (usually a 3 letter code)</label>
						<div className="input-group" style={verticalMargin}>
							<TextInput value={formData.siteId} action={this.getJobDefUpdater('siteId')} converter={s => s.toUpperCase()} maxLength="5"/>
							<span className="input-group-btn">
								<button className="btn btn-primary cp-pointer"
										onClick={this.onLoadDataBtnClick.bind(this)}
										disabled={!isExisting}>Load data</button>
							</span>
						</div>

						<label style={labelStyle}>Start date (YYYY-MM-DD)</label>
						<DatePickerInput
							ref={dpi => this.datePickerInputStart = dpi}
							minDate={minDate}
							maxDate={maxDate}
							defaultValue={start ? undefined : minDate}
							style={verticalMargin}
							showOnInputClick={false}
							value={start}
							className={errorStart || startInvalid ? 'cp-dpi-error' : ''}
							onChange={this.onStartDateSelected.bind(this)}
							onClear={this.onStartDateSelected.bind(this)}
							displayFormat="YYYY-MM-DD"
							returnFormat="YYYY-MM-DD"
						/>

						<label style={labelStyle}>End date (YYYY-MM-DD)</label>
						<DatePickerInput
							ref={dpi => this.datePickerInputStop = dpi}
							minDate={minDate}
							maxDate={maxDate}
							defaultValue={stop ? undefined : minDate}
							showOnInputClick={false}
							value={stop}
							className={errorStop || stopInvalid ? 'cp-dpi-error' : ''}
							onChange={this.onStopDateSelected.bind(this)}
							onClear={this.onStopDateSelected.bind(this)}
							displayFormat="YYYY-MM-DD"
							returnFormat="YYYY-MM-DD"
						/>

						<button style={buttonStyle}
								className="btn btn-primary cp-pointer"
								disabled={!props.workerData.isJobDefComplete || !props.currUser.email}
								onClick={props.startJob}>Submit STILT job</button>

					</div>
				</div>
			</div>

			<div className="col-md-2" style={{minWidth: 310}}>
				<h4>Submitted STILT jobs</h4>

				<div className="panel panel-default">
					<div className="panel-body">
						<button style={{display: 'block', clear: 'both', marginBottom: 20}} className="btn btn-primary cp-pointer" onClick={props.showDashboard}>Show details</button>
						{ ds.queue && (ds.queue.length || ds.done.length || ds.running.length)
							? <div>
								<JobList title="Job queue" isQueue={true} user={props.currUser} jobs={ds.queue}/>
								<JobList title="Running computations" user={props.currUser} jobs={ds.running} />
								<JobList title="Finished computations" user={props.currUser} jobs={ds.done} />
							</div>
							: <div>No jobs have been submitted</div>
						}
					</div>
				</div>

			</div>
		</div>;
	}
}

const JobList = props => props.jobs.length
	? <div className="panel panel-info">
		<div className="panel-heading">
			<h3 className="panel-title">{props.title}</h3>
		</div>
		<div className="panel-body">{
			props.jobs.map(job => {
				return props.isQueue
					? <JobLabel user={props.user} job={job} key={job.id} />
					: <JobLabel user={props.user} job={job.job} key={job.job.id} />;
			})
		}
		</div>
	</div>
	: null;

const JobLabel = props => {
	const job = props.job;
	const lbl = {
		txt: "Site '" + job.siteId + "'",
		cls: "label label-default",
		title: `Site Id: ${job.siteId}
Latitude: ${job.lat}
Longitude: ${job.lon}
Altitude: ${job.alt}
From: ${job.start}
To: ${job.stop}`
	};

	const myJob = props.user ? props.user.email === job.userId : undefined;

	return <h4>
		<span title={lbl.title} className={"cp-help " + lbl.cls} style={{verticalAlign: 'middle'}}>
			{myJob
				? <span className="glyphicon glyphicon-star" style={{ top: 2, marginRight: 5}}></span>
				: null
			}
			{lbl.txt}
		</span>
	</h4>;
};

function getDatesFromProps(props){
	return {
		start: props.workerData.formData ? props.workerData.formData.start : undefined,
		stop: props.workerData.formData ? props.workerData.formData.stop : undefined,
		minDate: props.availableMonths ? props.availableMonths.min : undefined,
		maxDate: props.availableMonths ? props.availableMonths.max : undefined,
		disabledDates: props.availableMonths ? props.availableMonths.disabledDates : undefined,
	}
}

function validateLatLngVal(min, max){
	return str => {
		if (str.length === 0) return str;

		// Force '.' as decimal character and remove duplicate decimal character
		const cleanedStr = str.replace(',', '.').split('.').slice(0, 2).join('.');

		const res = parseFloat(parseFloat(cleanedStr).toFixed(2));
		if (!isNumber(res)) throw new Error("This is not a number");
		else if (res < min || res > max) throw new Error("The position lies outside of boundary");
		else if (cleanedStr.match(/\.$/) || cleanedStr.match(/\.\d0$/) || cleanedStr.match(/\.0+$/)) return cleanedStr;
		else if (res.toString() !== cleanedStr) throw new Error("The number is not in a canonical format");
		else return res;
	}
}

function toInt(str){
	if (str.length === 0) return str;

	const res = parseInt(str);
	if(!isNumber(res)) throw new Error("This is not a number");
	else if(res.toString() !== str || res <= -1) throw new Error("The number is not a non-negative integer");
	else return res;
}

function isNumber(n){
	return !isNaN(parseFloat(n)) && isFinite(n);
}

