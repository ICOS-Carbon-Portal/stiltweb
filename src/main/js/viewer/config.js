import Dygraph from 'dygraphs';
import {parseUrlQuery} from './utils';

const dateSeries = {
	label: 'isodate',
	comment: 'datetime, UTC',
	options: {axis: 'x'}
};

const stiltCo2ResultColumns = [dateSeries, {
	label: 'co2.stilt',
	comment: 'STILT-modelled CO2 mole fraction',
	options: {axis: 'y1', color: 'rgb(0,0,255)', strokeWidth: 2}
}, {
	label: 'co2.background',
	comment: 'global background CO2 mole fraction',
	options: {axis: 'y1', color: 'rgb(157,195,230)'}
}, {
	label: 'co2.bio',
	comment: 'CO2 from biospheric processes',
	options: {axis: 'y2', color: 'rgb(0,255,0)', strokeWidth: 2}
}, {
	label: 'co2.bio.gee',
	comment: 'CO2 uptake by photosynthesis',
	options: {axis: 'y2', color: 'rgb(0,144,81)'}
}, {
	label: 'co2.bio.resp',
	comment: 'CO2 release by respiration',
	options: {axis: 'y2', color: 'rgb(146,144,0)'}
}, {
	label: 'co2.fuel',
	comment: 'anthropogenic CO2 from fuel combustion',
	options: {axis: 'y2', color: 'rgb(255,0,0)', strokeWidth: 2}
}, {
	label: 'co2.fuel.oil',
	comment: 'CO2 from oil combustion',
	options: {axis: 'y2', color: 'rgb(197,90,17)'}
}, {
	label: 'co2.fuel.coal',
	comment: 'CO2 from coal combustion',
	options: {axis: 'y2', color: 'rgb(255,147,0)'},
}, {
	label: 'co2.fuel.gas',
	comment: 'CO2 from gas combustion',
	options: {axis: 'y2', color: 'rgb(255,64,255)'}
}, {
	label: 'co2.fuel.bio',
	comment: 'CO2 from biofuel combustion',
	options: {axis: 'y2', color: 'rgb(216,131,255)'}
}, {
	label: 'co2.fuel.waste',
	comment: 'CO2 from solid waste',
	options: {axis: 'y2', color: 'rgb(216,131,127)'}
}, {
	label: 'co2.energy',
	comment: 'CO2 from energy production',
	options: {axis: 'y2', color: 'rgb(197,90,17)', strokePattern: Dygraph.DASHED_LINE}
}, {
	label: 'co2.transport',
	comment: 'CO2 from transport',
	options: {axis: 'y2', color: 'rgb(255,147,0)', strokePattern: Dygraph.DASHED_LINE},
}, {
	label: 'co2.industry',
	comment: 'CO2 from industry',
	options: {axis: 'y2', color: 'rgb(255,64,255)', strokePattern: Dygraph.DASHED_LINE}
}, {
	label: 'co2.residential',
	comment: 'CO2 from residential emissions',
	options: {axis: 'y2', color: 'rgb(128,64,128)', strokePattern: Dygraph.DASHED_LINE}
}, {
	label: 'co2.cement',
	comment: 'CO2 from cement production',
	options: {axis: 'y2', color: 'rgb(128,128,128)', strokePattern: Dygraph.DASHED_LINE}
}, {
	label: 'co2.other_categories',
	comment: 'CO2 from other categories',
	options: {axis: 'y2', color: 'rgb(216,131,255)', strokePattern: Dygraph.DASHED_LINE}
}];

const ch4ResultColumns = [dateSeries, {
	label: 'ch4.stilt',
	comment: 'STILT-modelled CH4 mole fraction',
	options: {axis: 'y1', color: 'rgb(0,0,255)', strokeWidth: 2}
}, {
	label: 'ch4.background',
	comment: 'global background CH4 mole fraction',
	options: {axis: 'y1', color: 'rgb(157,195,230)'}
}, {
	label: 'ch4.anthropogenic',
	comment: 'anthropogenic CH4',
	options: {axis: 'y2', color: 'rgb(255,0,0)', strokeWidth: 2}
}, {
	label: 'ch4.agriculture',
	comment: 'CH4 from agriculture',
	options: {axis: 'y2', color: 'rgb(255,147,0)'},
}, {
	label: 'ch4.waste',
	comment: 'CH4 from waste',
	options: {axis: 'y2', color: 'rgb(216,131,127)'}
}, {
	label: 'ch4.energy',
	comment: 'CH4 from energy production',
	options: {axis: 'y2', color: 'rgb(197,90,17)', strokePattern: Dygraph.DASHED_LINE}
}, {
	label: 'ch4.other_categories',
	comment: 'CH4 from other categories',
	options: {axis: 'y2', color: 'rgb(216,131,255)', strokePattern: Dygraph.DASHED_LINE}
}, {
	label: 'ch4.natural',
	comment: 'CH4 from natural processes',
	options: {axis: 'y2', color: 'rgb(0,255,0)', strokeWidth: 2}
}, {
	label: 'ch4.wetlands',
	comment: 'CH4 from wetlands',
	options: {axis: 'y2', color: 'rgb(0,144,81)'}
}, {
	label: 'ch4.soil_uptake',
	comment: 'CH4 uptake by soil',
	options: {axis: 'y2', color: 'rgb(128,128,128)', strokePattern: Dygraph.DASHED_LINE}
}, {
	label: 'ch4.wildfire',
	comment: 'CH4 from wildfires',
	options: {axis: 'y2', color: 'rgb(255,64,255)', strokePattern: Dygraph.DASHED_LINE}
}, {
	label: 'ch4.other_natural',
	comment: 'CH4 from other natural causes',
	options: {axis: 'y2', color: 'rgb(146,144,0)'}
}];

const stiltCo2ResultColumnGrouping = {
	'Biospheric CO2': ['co2.bio'],
	'Biospheric CO2 split into Photosynthetic uptake and respiration': ['co2.bio.gee', 'co2.bio.resp'],
	'Anthropogenic CO2': ['co2.fuel', 'co2.cement'],
	'Anthropogenic CO2 from fuel use': ['co2.fuel.coal', 'co2.fuel.oil', 'co2.fuel.gas', 'co2.fuel.bio', 'co2.fuel.waste'],
	'Anthropogenic CO2 split into Source categories': ['co2.energy', 'co2.transport', 'co2.industry', 'co2.residential', 'co2.other_categories']
};

const ch4ResultGrouping = {
	'Anthropogenic CH4': ['ch4.anthropogenic'],
	'Anthropogenic CH4 split into source categories': ['ch4.agriculture', 'ch4.waste', 'ch4.energy', 'ch4.other_categories'],
	'Natural CH4': ['ch4.natural'],
	'Natural CH4 split into processes': ['ch4.wetlands', 'ch4.soil_uptake', 'ch4.wildfire', 'ch4.other_natural']
}

function secondaryComponents(grouping, allColumns){
	const comps = {}
	for(let key in grouping){
		let colLabels = grouping[key]
		let columns = colLabels.map(label => allColumns.find(col => col.label === label))
		comps[key] = columns
	}
	return comps
}

const icosCo2Columns = [dateSeries, {
	label: 'co2.observed',
	comment: 'observed atmospheric CO2 mole fraction available from ICOS data portal',
	options: {axis: 'y1', color: 'rgb(0, 0, 0)', strokeWidth: 2}
}];

const icosCh4Columns = [dateSeries, {
	label: 'ch4.observed',
	comment: 'observed atmospheric CH4 mole fraction available from ICOS data portal',
	options: {axis: 'y1', color: 'rgb(0, 0, 0)', strokeWidth: 2}
}];

const urlQuery = parseUrlQuery();

const tracerConf = {
	co2: {
		observationDataSpec: 'http://meta.icos-cp.eu/resources/cpmeta/ObspackTimeSerieResult',
		stiltResultColumns: stiltCo2ResultColumns,
		stiltResultColumnGrouping: stiltCo2ResultColumnGrouping,
		icosColumns: icosCo2Columns,
		secondaryComponents: secondaryComponents(stiltCo2ResultColumnGrouping, stiltCo2ResultColumns)
	},
	ch4: {
		observationDataSpec: 'http://meta.icos-cp.eu/resources/cpmeta/ObspackCH4TimeSeriesResult',
		stiltResultColumns: ch4ResultColumns,
		stiltResultColumnGrouping: ch4ResultGrouping,
		icosColumns: icosCh4Columns,
		secondaryComponents: secondaryComponents(ch4ResultGrouping, ch4ResultColumns)
	}
}

export default {
	sparqlEndpoint: 'https://meta.icos-cp.eu/sparql',
	cpmetaOntoUri: 'http://meta.icos-cp.eu/ontologies/cpmeta/',
	cpmetaResUri: 'http://meta.icos-cp.eu/resources/cpmeta/',
	byTracer: tracerConf,
	//icosCo2Spec: 'http://meta.icos-cp.eu/resources/cpmeta/atcCo2L2DataObject',
	observationVarName: 'value',
	//stiltResultColumns,
	//stiltResultColumnGrouping,
	//icosColumns,
	// primaryComponents(selectedScope){
	// 	const obsColumns = !selectedScope || selectedScope.dataObject
	// 		? icosColumns.slice(1)
	// 		: [Object.assign({}, icosColumns[1], {disabled: true})];
	// 	return obsColumns.concat(stiltResultColumns.slice(1,3));
	// },
	//secondaryComponents,
	defaultDelay: 100, //ms
	viewerScope: ["stationId", "fromDate", "toDate"].every(qpar => urlQuery.hasOwnProperty(qpar)) ? urlQuery : null
}

