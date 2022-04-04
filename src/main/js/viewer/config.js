import Dygraph from 'dygraphs';
import {parseUrlQuery} from './utils';

const dateSeries = {
	label: 'isodate',
	comment: 'datetime, UTC',
	options: {axis: 'x'}
};

const stiltResultColumns = [dateSeries, {
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
	label: 'co2.cement',
	comment: 'CO2 from cement production',
	options: {axis: 'y2', color: 'rgb(128,128,128)', strokePattern: Dygraph.DASHED_LINE}
}, {
	label: 'co2.others',
	comment: 'CO2 from other categories',
	options: {axis: 'y2', color: 'rgb(216,131,255)', strokePattern: Dygraph.DASHED_LINE}
}];

const stiltResultColumnGrouping = {
	'Biosperic CO2': ['co2.bio'],
	'Biosperic CO2 split into Photosynthetic uptake and respiration': ['co2.bio.gee', 'co2.bio.resp'],
	'Anthropogenic CO2': ['co2.fuel'],
	'Anthropogenic CO2 split into Fuel types': ['co2.fuel.coal', 'co2.fuel.oil', 'co2.fuel.gas', 'co2.fuel.bio', 'co2.fuel.waste'],
	'Anthropogenic CO2 split into Source categories': ['co2.energy', 'co2.transport', 'co2.industry', 'co2.cement', 'co2.others']
};

const secondaryComponents = Object.keys(stiltResultColumnGrouping).reduce((acc, key) => {
	acc[key] = stiltResultColumnGrouping[key].map(label => stiltResultColumns.find(src => src.label === label));
	return acc;
}, {});

const icosColumns = [dateSeries, {
	label: 'co2.observed',
	comment: 'observed atmospheric CO2 mole fraction available from ICOS',
	options: {axis: 'y1', color: 'rgb(0, 0, 0)', strokeWidth: 2}
}];

const urlQuery = parseUrlQuery();

export default {
	sparqlEndpoint: 'https://meta.icos-cp.eu/sparql',
	cpmetaOntoUri: 'http://meta.icos-cp.eu/ontologies/cpmeta/',
	cpmetaResUri: 'http://meta.icos-cp.eu/resources/cpmeta/',
	icosCo2Spec: 'http://meta.icos-cp.eu/resources/cpmeta/atcCo2L2DataObject',
	observationVarName: 'co2',
	stiltResultColumns,
	stiltResultColumnGrouping,
	icosColumns,
	primaryComponents(selectedScope){
		const obsColumns = !selectedScope || selectedScope.dataObject
			? icosColumns.slice(1)
			: [Object.assign({}, icosColumns[1], {disabled: true})];
		return obsColumns.concat(stiltResultColumns.slice(1,3));
	},
	secondaryComponents,
	defaultDelay: 100, //ms
	viewerScope: ["stationId", "fromDate", "toDate"].every(qpar => urlQuery.hasOwnProperty(qpar)) ? urlQuery : null
}

