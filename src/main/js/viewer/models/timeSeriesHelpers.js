import config from '../config';
import DygraphData, {icosBinTableToDygraphData} from './DygraphData';

export function makeTimeSeriesGraphData(obsBinTable, rawArray, id){

	const modelComponents = makeModelComponentsData(rawArray);
	if(!obsBinTable) return modelComponents.withId(id);

	const obsDyData = icosBinTableToDygraphData(obsBinTable, config.icosColumns);

	return DygraphData.merge(obsDyData, modelComponents).withId(id);
}

function makeModelComponentsData(rawArray){

	function rowGetter(i){
		const row = rawArray[i].slice(0);
		row[0] = new Date(row[0] * 1000);
		return row;
	}

	return new DygraphData(rowGetter, rawArray.length, config.stiltResultColumns);
}

