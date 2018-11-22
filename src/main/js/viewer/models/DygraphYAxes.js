
const emptyRanges = _ => [undefined, undefined];
const emptyTickVals = [[], []];

const mults = [1, 2, 2.5, 5];
const preferredSteps = Array.from({length: 60}, (_, i) => i - 10).reduce((acc, factor) => {
	return acc.concat(mults.map(m => Math.pow(10, factor) * m));
}, []);

export default class DygraphYAxes{
	constructor(){
		this.defaultTickCount = 7;

		this.graph = undefined;
		this.axelStepSpan = undefined;
		this.step = Infinity;
		this.valueRanges = emptyRanges();
		this.zoomValueRanges = emptyRanges();
		this.dataValueRanges = emptyRanges();
		this.tickVals = emptyTickVals;
	}

	tickerY1(){
		return this.tickVals[0];
	}

	tickerY2(){
		return this.tickVals[1];
	}

	initFromExternal(graph, data, visibility, axelNumbers){
		if (graph) this.graph = graph;

		this.dataValueRanges = calcYAxesExtremes(data, visibility, axelNumbers);

		const zvr = this.zoomValueRanges.map(zvr => zvr === undefined ? [Infinity, -Infinity] : zvr);
		const valueRanges = this.dataValueRanges.map((dvr, idxAxel) => {
			if (zvr[idxAxel].every(val => val === null)){
				return dvr;
			} else {
				return [
					zvr[idxAxel][0] > dvr[1] ? dvr[0] : Math.max(dvr[0], zvr[idxAxel][0]),
					zvr[idxAxel][1] < dvr[0] ? dvr[1] : Math.min(dvr[1], zvr[idxAxel][1]),
				];
			}
		});

		const [y1Range, y2Range] = valueRanges;
		const span = Math.max(y1Range[1] - y1Range[0], y2Range[1] - y2Range[0]);

		this.initYAxes(y1Range, y2Range, span);
	}

	initFromInternal(valueRanges){
		const overrideValueRanges = this.zoomValueRanges.some(vr => vr)
			? this.zoomValueRanges
			: valueRanges;

		const [y1Range, y2Range] = overrideValueRanges;
		const span = Math.max(y1Range[1] - y1Range[0], y2Range[1] - y2Range[0]);

		this.initYAxes(y1Range, y2Range, span);
	}

	initYAxes(y1Range, y2Range, span){
		this.axelStepSpan = 0;
		this.step = Infinity;
		this.valueRanges = emptyRanges();

		this.tickVals[0] = this.getTickVals(0, y1Range, span);
		this.tickVals[1] = this.getTickVals(1, y2Range, span);

		this.updateOptions();
	}

	getTickVals(yAxelIdx, range, span){
		if (range === undefined) return;

		const [valMin, valMax] = range;

		const step = calcStep(span, this.defaultTickCount);
		if (step === undefined) return;

		this.step = Math.min(this.step, step);

		const tickVals = this.calcTickVals(valMin, valMax, span);
		if (tickVals === undefined) return;

		padYRange(yAxelIdx, tickVals, this.step, this.dataValueRanges);

		this.valueRanges[yAxelIdx] = [tickVals[0], tickVals[tickVals.length - 1]];
		expandTicks(this.dataValueRanges[yAxelIdx], tickVals, this.step);

		return tickVals.map(v => {
			return {v, label: v !== 0 && v < 1 && v > -1 ? v.toFixed(1) : v};
		});
	}

	calcTickVals(valMin, valMax, span){
		if (valMin === null || valMax === null) return [];

		const axelSpan = Math.max(valMax - valMin, span);
		const multiplier = Math.floor(valMin / this.step);
		const minYAxelVal = multiplier * this.step;

		this.axelStepSpan = Math.max(this.axelStepSpan, calcAxelSpan(minYAxelVal, this.step, valMin + axelSpan));
		const tickCount = (this.axelStepSpan / this.step) + 1;

		return Array.from({length: tickCount}, (_, i) => i)
			.map(t => minYAxelVal + t * this.step);
	}


	updateOptions(){
		if (!this.graph) throw new Error("Graph not initialized");

		const newOptions = Object.assign({
			axes: {
				y: {valueRange: this.valueRanges[0]},
				y2: {valueRange: this.valueRanges[1]}
			}
		}, this.zoomValueRanges.some(zvr => zvr) ? {} : {dateWindow: null});

		this.graph.updateOptions(newOptions);
	}

	dblclick(){
		this.zoomValueRanges = emptyRanges();
		this.initFromInternal(this.dataValueRanges);
	}

	zoomCallback(minDate, maxDate, yRanges){
		this.zoomValueRanges = yRanges.map((range, idxAxel) =>
			this.valueRanges[idxAxel].every(vr => vr) ? range : [null, null]
		);
		this.initFromInternal(this.zoomValueRanges);
	}
}

const padYRange = (yAxelIdx, tickVals, step, dataValueRanges) => {
	// This could not be solved with dygraph option yRangePad -> it pads on each pan
	// Check if second last tick value is greater than max value in data
	if (tickVals[tickVals.length - 2] > dataValueRanges[yAxelIdx][1]) {
		// Add tick in the beginning and remove last tick
		tickVals.unshift(tickVals[0] - step);
		tickVals.pop();
	}
};

const expandTicks = (dataValueRange, tickVals, step) => {
	// If zoomed in -> add ticks for the entire data range
	const [dataMin, dataMax] = dataValueRange;

	if (dataMin < tickVals.slice(0, 1)[0]){
		do {
			tickVals.unshift(tickVals.slice(0, 1)[0] - step);

		} while (tickVals.slice(0, 1)[0] > dataMin);
	}

	if (dataMax > tickVals.slice(-1)[0]){
		do {
			tickVals.push(tickVals.slice(-1)[0] + step);

		} while (tickVals.slice(-1)[0] < dataMax);
	}
};

const calcAxelSpan = (min, step, max) => {
	let val = min;

	while (val < max){ val += step; }

	return val - min;
};

const calcStep = (span, tickCount) => {
	const step = span / tickCount;
	return preferredSteps.find(ds => ds >= step);
};

const calcYAxesExtremes = (data, visibility, axelNumbers) => {
	const visibleCols = visibility.filter(v => v);
	const visibleAxelNumbers = visibility.reduce((acc, isVisible, i) => {
		if (isVisible) acc.push(axelNumbers[i]);
		return acc;
	}, []);

	const minMaxVals = data.reduce((acc, row) => {
		const cols = row.slice(1).filter((col, idx) => visibility[idx]);
		cols.forEach((col, i) => {
			if (col !== null) {
				if (col < acc[i][0]) acc[i][0] = col;
				if (col > acc[i][1]) acc[i][1] = col;
			}
		});
		return acc;
	}, Array.from({length: visibleCols.length}, _ => [Infinity, -Infinity]));

	const y1 = minMaxVals.filter((minMax, i) => visibleAxelNumbers[i] === 1).reduce((acc, minMax) => {
		acc[0] = Math.min(acc[0], minMax[0]);
		acc[1] = Math.max(acc[1], minMax[1]);
		return acc;
	}, [Infinity, -Infinity]);

	const y2 = minMaxVals.filter((minMax, i) => visibleAxelNumbers[i] === 2).reduce((acc, minMax) => {
		acc[0] = Math.min(acc[0], minMax[0]);
		acc[1] = Math.max(acc[1], minMax[1]);
		return acc;
	}, [Infinity, -Infinity]);

	return [
		y1.map(v => Math.abs(v) === Infinity ? null : v),
		y2.map(v => Math.abs(v) === Infinity ? null : v)
	];
};
