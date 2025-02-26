import React from 'react';

import config from '../config';

import Select from '../../common/components/Select.jsx';
import StationsMap from '../../common/components/LMap.jsx';
import { getPrimaryComponents } from '../models/Axes.js';
import StationAndYearSelector from './StationAndYearSelector.jsx';


export default props =>
		<div className="row">
			<div className="col-md-6 pe-0">
			
				<h2 className="visually-hidden">Station selection map</h2>
				<StationSelectingMap {...props} />
			</div>
			<div className="col-md-6 ps-0">
				<h2 className="visually-hidden">Configuration settings</h2>
				<ul className="list-group rounded-0 mb-0">
					{config.viewerScope
						? <ViewerScopeDisplay {...config.viewerScope} />
						: <StationAndYearSelector {...props} />
					}
					<li className="list-group-item">
						<GasSelector selectGas={props.selectGas} gas="CO2" selectedGas={props.selectedGas} />
						<GasSelector selectGas={props.selectGas} gas="CH4" selectedGas={props.selectedGas} />
					</li>
					<li className="list-group-item">
						<AxisControlPrimary title="Primary Y-axis" components={getPrimaryComponents(props.selectedScope, props.selectedGas)} {...props} />
					</li>
					<li className="list-group-item">
						<AxisControlSecondary title="Secondary Y-axis" components={config.byTracer[props.selectedGas].secondaryComponents} {...props} />
					</li>
					<li className="list-group-item"><MovieControl {...props} /></li>
				</ul>
			</div>
		</div>

const GasSelector = ({selectGas, gas, selectedGas}) => {
	const gasVar = gas.toLowerCase();
	return <div className="form-check form-check-inline">
		<input
			className="form-check-input"
			type="radio"
			id={gasVar + "radio"}
			value={gasVar}
			onChange={() => selectGas(gasVar)}
			checked={selectedGas === gasVar}
		/>
		<label className="form-check-label fw-bold" htmlFor={gasVar + "radio"}>{gas}</label>
	</div>
}

const StationSelectingMap = ({stations, selectedStation, selectStation}) => {
	return <div style={{height: 565}}>
		<StationsMap
			stations={stations}
			selectedStation={selectedStation}
			action={selectStation}
		/>
	</div>
};

const ViewerScopeDisplay = props => <li className="list-group-item">
	<div className="row">
		<div className="col-md-4">
			<strong>Pre-selected site:</strong> {props.stationId}
		</div>
		<div className="col-md-4">
			<strong>Start date:</strong> {props.fromDate}
		</div>
		<div className="col-md-4">
			<strong>End date:</strong> {props.toDate}
		</div>
	</div>
</li>


const AxisControlPrimary = props => {
	return <div>
		<h3 className="h6 text-black fw-bold">{props.title}:</h3>
		<FanOutComponents fanOuts={props.components} {...props} />
	</div>;
};

const FanOutComponents = props => {
	return (
		<div>
		{props.fanOuts.map(
			(comp,i) => <StiltComponentSelector key={i} {...comp} {...props} />
		)}
		</div>
	);
};

const AxisControlSecondary = props => {
	return (
		<div>
			<h3 className="h6 text-black fw-bold">{props.title}:</h3>
			{Object.keys(props.components).map((label, i) =>
				<div key={'group' + i}>
					<div className="text-decoration-underline">{label}</div>
					<FanOutComponents fanOuts={props.components[label]} {...props} />
				</div>
			)}
		</div>
	);
};


const StiltComponentSelector = ({label, comment, disabled, updateVisibility, options}) => {
	const visibilityChangeHandler = event => {
		if(updateVisibility){
			updateVisibility(label, event.target.checked);
		}
	};

	const visibility = options.modelComponentsVisibility || {};
	const style = {marginRight: 3, position: 'relative', top: 2};
	if (disabled)
		Object.assign(style, {cursor: 'not-allowed'})

	return <div key={label} title={disabled ? 'Not available' : comment} style={{marginLeft: 7, display: 'inline-block'}}>
		<input type="checkbox"
			checked={disabled ? false : !!visibility[label]}
			onChange={visibilityChangeHandler}
			style={style}
			disabled={disabled}
			id={label.replaceAll(".", "-")}
		/>
		<label htmlFor={label.replaceAll(".", "-")}>
			{label}
		</label>
	</div>
};


const MovieControl = props => {

	const toNext = () => props.incrementFootprint(1);
	const toPrevious = () => props.incrementFootprint(-1);

	const navDisabled = props.playingMovie || !props.footprint;

	const playClass = "fas fa-" + (props.playingMovie ? 'pause' : 'play');
	const playTitle = props.playingMovie ? 'Pause playback' : 'Play';

	return <div className="row">
		<div className="d-flex col my-1 justify-content-between align-items-center">
			<div className="me-auto fw-bold pe-1">
				Playback
			</div>
			<div className="ms-auto btn-group" style={{minWidth: 120}}>
				<button title="To previous footprint" type="button" className="btn btn-outline-secondary" onClick={toPrevious} disabled={navDisabled}>
					<i className="fas fa-chevron-left" />
				</button>
				<button title={playTitle} type="button" className="btn btn-outline-secondary" onClick={props.pushPlay} disabled={!props.footprint}>
					<i className={playClass} />
				</button>
				<button  title="To next footprint" type="button" className="btn btn-outline-secondary" onClick={toNext} disabled={navDisabled}>
				<i className="fas fa-chevron-right" />
				</button>
			</div>
		</div>
		<div className="d-flex col my-1 justify-content-between align-items-center">
			<div className="me-auto fw-bold pe-1">
				Playback speed
			</div>
			<Select
				selectValue={props.setDelay}
				infoTxt="Select playback speed"
				availableValues={delayValues}
				value={props.movieDelay}
				presenter={delayPresenter}
				options={{disabled: !props.footprint}}
				className="ms-auto w-auto"
				style={{minWidth: "100px"}}
			/>
		</div>
	</div>;
};

const delayValues = [0, 50, 100, 200, 500, 1000, 3000];
function delayPresenter(delay){
	switch (delay){
		case 0 : return 'Fastest (processor-limited)';
		case 50 : return 'Very fast (up to 20 fps)';
		case 100 : return 'Fast (up to 10 fps)';
		case 200 : return 'Medium (up to 5 fps)';
		case 500 : return 'Medium (up to 2 fps)';
		case 1000 : return 'Slow (up to 1 fps)';
		case 3000 : return 'Very slow (0.33 fps)';
		default : return (1000 / delay) + ' fps';
	}
}

