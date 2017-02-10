import React, { Component } from 'react';
import { connect } from 'react-redux';
import {AnimatedToasters} from 'icos-cp-toaster';
import StationsMap from '../../common/components/LMap.jsx';
import Select from '../../common/components/Select.jsx';
import TextInput from '../components/TextInput.jsx';

const marginTop = 10;
const defaultAlt = 100;

class App extends Component {
	constructor(props) {
		super(props);
		this.state = {
			selectedStation: null,
		};
	}

	setSelectedStation(selectedStation){
		this.setState({selectedStation});
	}

	updateLat(newVal){
		this.updateCoord(true, newVal);
	}

	updateLon(newVal){
		this.updateCoord(false, newVal);
	}

	updateCoord(isLat, newVal){
		const selectedStation = this.state.selectedStation;

		this.setState({selectedStation: {
			lat: isLat ? newVal : selectedStation.lat,
			lon: isLat ? selectedStation.lon : newVal
		}});
	}

	render() {
		const state = this.state;
		const props = this.props;

		const lat = state.selectedStation ? state.selectedStation.lat : '';
		const lon = state.selectedStation ? state.selectedStation.lon : '';
		const alt = state.selectedStation && state.selectedStation.alt ? state.selectedStation.alt : defaultAlt;
		const code = state.selectedStation ? state.selectedStation.id : '';

		const labelStyle = {display: 'block', clear: 'both'};
		const textInputStyle = {marginBottom: 20};

		// console.log({state, props, lat, lon, alt, code});

		return (
			<div>
				<AnimatedToasters
					autoCloseDelay={5000}
					fadeInTime={100}
					fadeOutTime={400}
					toasterData={props.toasterData}
					maxWidth={400}
				/>

				<div className="page-header">
					<h1>STILT footprint worker</h1>
				</div>

				<div className="row" style={{marginTop}}>

					<div className="col-md-8">
						<h4>Existing STILT footprints</h4>

						<div className="panel panel-default">
							<div className="panel-body">

								<div style={{marginBottom: 10}}>
									<Select
										selectValue={this.setSelectedStation.bind(this)}
										infoTxt="Select station here or on the map"
										availableValues={props.stations}
										value={state.selectedStation}
										presenter={station => station ? `${station.name} (${station.id})` : station}
										sort={true}
									/>
								</div>

								<div style={{width: '100%', height: 600}}>
									<StationsMap
										workerMode={true}
										stations={props.stations}
										selectedStation={state.selectedStation}
										action={this.setSelectedStation.bind(this)}
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
								<TextInput style={textInputStyle} value={lat} action={this.updateLat.bind(this)} />

								<label style={labelStyle}>Longitude (decimal degree)</label>
								<TextInput style={textInputStyle} value={lon} action={this.updateLon.bind(this)} />

								<label style={labelStyle}>Altitude (meters)</label>
								<TextInput style={textInputStyle} value={alt} />

								<label style={labelStyle}>3 letter code</label>
								<TextInput style={textInputStyle} value={code} />

								<label style={labelStyle}>Start date</label>
								<TextInput style={textInputStyle} />

								<label style={labelStyle}>End date</label>
								<TextInput />

								<button style={{display: 'block', clear: 'both', marginTop: 40}} className="btn btn-primary">Create STILT footprint</button>

							</div>
						</div>
					</div>
				</div>
			</div>
		);
	}
}

function stateToProps(state){
	return Object.assign({}, state);
}

export default connect(stateToProps)(App)