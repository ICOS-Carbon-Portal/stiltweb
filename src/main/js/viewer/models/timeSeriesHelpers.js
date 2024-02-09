import config from '../config';
import DygraphData, {icosBinTableToDygraphData} from './DygraphData';

export function makeTimeSeriesGraphData(stationDataAction, id, gas){

	const {obsBinTable, modelResults, fromDate, toDate} = stationDataAction;

	const modelComponents = makeModelComponentsData(modelResults, gas);
	if(!obsBinTable) return modelComponents.withId(id);

	const obsDyData = icosBinTableToDygraphData(obsBinTable, fromDate, toDate, config.byTracer[gas]);

	return DygraphData.merge(obsDyData, modelComponents).withId(id);
}

function makeModelComponentsData(rawArray, gas){

	function rowGetter(i){
		const row = rawArray[i].slice(0);
		row[0] = new Date(row[0] * 1000);
		return row;
	}

	return new DygraphData(rowGetter, rawArray.length, config.byTracer[gas].stiltResultColumns);
}

