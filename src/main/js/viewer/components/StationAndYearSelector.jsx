import React, { Fragment } from 'react'
import Dropdown from "./Dropdown.jsx";
import Select from '../../common/components/Select.jsx';
import { StationFilters } from '../store.js';

export default function(props){
	const {
		selectYear, setStationFilter, selectStation, selectedGas,
		selectedScope, stationFilter, selectedStation, stations
	} = props
	const yearInfos = selectedStation ? selectedStation.years : []
	const yearLookup = year => yearInfos.find(info => info.year === year)
	const yearsDisabled = !yearInfos.length;

	return <li className="list-group-item">
		<div className="row justify-content-center" style={{marginBottom: '10px'}}>
			<div className="input-group col-md-12 justify-content-center">
				<div className="input-group-text" id="statFiltLabel">Station filter</div>
				{
					StationFilters.map(filter => {
						const id = "stationFilter_" + filter.label
						return <Fragment key={filter.label}>
							<input type="radio"
								className="btn-check"
								name={id} id={id}
								autoComplete="off"
								checked={filter.label === stationFilter.label}
								onChange={() => setStationFilter(filter)} />
							<label className="btn btn-outline-info" htmlFor={id}>{filter.label}</label>
						</Fragment>
					})
				}
			</div>
		</div>
		<div className="row">
			<div className="col-md-7">
				<Dropdown
					buttonLbl="Select station here or on the map"
					presenter={station => station ? `${station.id} (${station.name}, ${station.alt} m)` : station}
					itemClickAction={selectStation}
					availableValues={stations}
					selectedValue={selectedStation}
					sort={true}
				/>
			</div>
			<div className="col-md-5">
				<Select
					selectValue={year => selectYear(yearLookup(year))}
					infoTxt={yearsDisabled ? "Select station first" : "Select year"}
					availableValues={yearInfos.map(info => info.year)}
					value={selectedScope && selectedScope.year}
					presenter={year => yearInfoToLabel(yearLookup(year), selectedGas)}
					options={{disabled: yearsDisabled}}
				/>
			</div>
		</div>
	</li>
}


function yearInfoToLabel(info, gas){
	if(!info) return info;
	if(!info.dataObject || !info.dataObject[gas]) return info.year
	return info.year + ` (+ObsPack ${info.dataObject[gas].samplingHeight} m)`;
}
