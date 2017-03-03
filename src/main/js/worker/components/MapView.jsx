import React, { Component } from 'react';
import StationsMap from '../../common/components/LMap.jsx';
import Select from '../../common/components/Select.jsx';
import TextInput from '../components/TextInput.jsx';
import StationInfo from '../models/StationInfo';

const geoBoundary = {
	latMin: 33,
	lonMin: -15,
	latMax: 72.9166666666667,
	lonMax: 34.875
};

export default class MapView extends Component {
	constructor(props) {
		super(props);
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

	render() {
		const props = this.props;
		const formData = props.workerData.formData;
		const isExisting = props.workerData.selectedStation.isExisting;
		const selectedStation = props.workerData.isFormAndExistingStationDifferent
			? new StationInfo(formData.lat, formData.lon)
			: props.workerData.selectedStation;

		const labelStyle = {display: 'block', clear: 'both'};
		const verticalMargin = {marginBottom: 20};

		// console.log({props, formData, form: props.workerData._workerFormData, selSt: props.workerData._selectedStation,
		// 	hasErrors: props.workerData.hasErrors, errors: props.workerData._errors, isJobDefComplete: props.workerData.isJobDefComplete, jobDef: props.workerData.jobDef
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

			<div className="col-md-2">
				<h4>Create new STILT footprint</h4>

				<div className="panel panel-default">
					<div className="panel-body">

						<label style={labelStyle}>Latitude (decimal degree)</label>
						<TextInput style={verticalMargin} value={formData.lat} action={this.getJobdefUpdater('lat')} converter={toLat} disabled={isExisting}/>

						<label style={labelStyle}>Longitude (decimal degree)</label>
						<TextInput style={verticalMargin} value={formData.lon} action={this.getJobdefUpdater('lon')} converter={toLon} disabled={isExisting}/>

						<label style={labelStyle}>Altitude (meters)</label>
						<TextInput style={verticalMargin} value={formData.alt} action={this.getJobdefUpdater('alt')} converter={toInt} disabled={isExisting}/>

						<label style={labelStyle}>3 letter code</label>
						<div className="input-group" style={verticalMargin}>
							<TextInput value={formData.siteId} action={this.getJobdefUpdater('siteId')} converter={s => s.toUpperCase()} maxLength="5"/>
							<span className="input-group-btn">
								<button className="btn btn-primary"
										onClick={this.onLoadDataBtnClick.bind(this)}
										disabled={!isExisting}>Load data</button>
							</span>
						</div>

						<label style={labelStyle}>Start date (YYYY-MM-DD)</label>
						<TextInput style={verticalMargin} value={formData.start} action={this.getJobdefUpdater('start')} converter={toDate} maxLength="10"/>

						<label style={labelStyle}>End date (YYYY-MM-DD)</label>
						<TextInput value={formData.stop} action={this.getJobdefUpdater('stop')} converter={toDate} maxLength="10"/>

						<button style={{display: 'block', clear: 'both', marginTop: 40, marginBottom: 20}}
								className="btn btn-primary"
								disabled={!props.workerData.isJobDefComplete || !props.userId}
								onClick={props.startJob}>Dispatch STILT job</button>

						<button className="btn btn-primary" onClick={props.showDashboard}>Show dashboard</button>

					</div>
				</div>
			</div>
		</div>;
	}
}

function toLat(str){
	const res = parseFloat(parseFloat(str).toFixed(2));

	if (!isNumber(res)) throw new Error("This is not a number");
	else if (res < geoBoundary.latMin || res > geoBoundary.latMax) throw new Error("The position lies outside of boundary");
	else if (str.match(/\.$/) || str.match(/\.0+$/)) return str;
	else if(res.toString() != str) throw new Error("The number is not in a canonical format");
	else return res;
}

function toLon(str){
	const res = parseFloat(parseFloat(str).toFixed(2));

	if (!isNumber(res)) throw new Error("This is not a number");
	else if (res < geoBoundary.lonMin || res > geoBoundary.lonMax) throw new Error("The position lies outside of boundary");
	else if (str.match(/\.$/) || str.match(/\.0+$/)) return str;
	else if(res.toString() != str) throw new Error("The number is not in a canonical format");
	else return res;
}

function toInt(str){
	const res = parseInt(str);
	if(!isNumber(res)) throw new Error("This is not a number")
	else if(res.toString() != str || res <= -1) throw new Error("The number is not a non-negative integer")
	else return res;
}

function toDate(str){
	let date = str.substring(0, 10);

	if(date.length <= 9 || isNaN(Date.parse(date)) || new Date(Date.parse(date)).toISOString().substring(0, 10) != date)
		throw new Error("This is not a correct ISO date, try YYYY-MM-DD");

	return date;
}

function isNumber(n){
	return !isNaN(parseFloat(n)) && isFinite(n);
}

