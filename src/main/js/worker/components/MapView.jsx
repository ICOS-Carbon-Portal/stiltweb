import React, { Component } from 'react';
import StationsMap from '../../common/components/LMap.jsx';
import Select from '../../common/components/Select.jsx';
import TextInput from '../components/TextInput.jsx';

export default class MapView extends Component {
	constructor(props) {
		super(props);
	}

	getJobdefUpdater(prop){
		const props = this.props;
		return function(val){
			const update = {};
			update[prop] = val;
			props.updateJobdef(update);
		};
	}

	render() {
		const props = this.props;
		const jobdef = props.jobdef || {lat: '', lon: '', alt: '', siteId: '', start: '', stop: ''};
		const stationForTheMap = isNumber(jobdef.lat) && isNumber(jobdef.lon) ? jobdef : undefined;

		const labelStyle = {display: 'block', clear: 'both'};
		const textInputStyle = {marginBottom: 20};

		return <div className="row">

			<div className="col-md-8">
				<h4>Existing STILT footprints</h4>

				<div className="panel panel-default">
					<div className="panel-body">

						<div style={{marginBottom: 10}}>
							<Select
								selectValue={props.selectStation}
								infoTxt="Select station here or on the map"
								availableValues={props.stations}
								value={props.selectedStation}
								presenter={station => station ? `${station.name} (${station.id})` : station}
								sort={true}
							/>
						</div>

						<div style={{width: '100%', height: 600}}>
							<StationsMap
								workerMode={true}
								stations={props.stations}
								selectedStation={stationForTheMap}
								action={props.selectStation}
								toastWarning={props.toastWarning}
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
						<TextInput style={textInputStyle} value={jobdef.lat} action={this.getJobdefUpdater('lat')} converter={toGeo} disabled={jobdef.alreadyExists}/>

						<label style={labelStyle}>Longitude (decimal degree)</label>
						<TextInput style={textInputStyle} value={jobdef.lon} action={this.getJobdefUpdater('lon')} converter={toGeo} disabled={jobdef.alreadyExists}/>

						<label style={labelStyle}>Altitude (meters)</label>
						<TextInput style={textInputStyle} value={jobdef.alt} action={this.getJobdefUpdater('alt')} converter={toInt}/>

						<label style={labelStyle}>3 letter code</label>
						<TextInput style={textInputStyle} value={jobdef.siteId} action={this.getJobdefUpdater('siteId')} converter={s => s.toUpperCase()}/>

						<label style={labelStyle}>Start date</label>
						<TextInput style={textInputStyle} value={jobdef.start} action={this.getJobdefUpdater('start')} converter={toDate}/>

						<label style={labelStyle}>End date</label>
						<TextInput style={textInputStyle} value={jobdef.stop} action={this.getJobdefUpdater('stop')} converter={toDate}/>

						<button style={textInputStyle} className="btn btn-primary" disabled={!props.jobdefComplete} onClick={props.startJob}>Create STILT footprint</button>

						<button className="btn btn-primary" onClick={props.showDashboard}>Show dashboard</button>

					</div>
				</div>
			</div>
		</div>;
	}
}

function toGeo(str){
	const res = parseFloat(parseFloat(str).toFixed(2));
	if(!isNumber(res)) throw new Error("This is not a number")
	else if(res.toString() != str) throw new Error("The number is not in a canonical format")
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

