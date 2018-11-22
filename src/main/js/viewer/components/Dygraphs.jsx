import React, { Component } from 'react';
import Dygraph from 'dygraphs';
import './Dygraphs.css';
import {deepMerge} from 'icos-cp-utils';
import DygraphYAxes from '../models/DygraphYAxes';


export default class Dygraphs extends Component {
	constructor(props) {
		super(props);

		this.dataId = undefined;
		this.graph = undefined;
		this.axes = props.axes;
		this.lastAxesTs = undefined;
		this.dygraphYAxes = new DygraphYAxes();
	}

	componentDidMount(){
		const props = this.props;

		this.dataId = props.data.id;
		const station = props.selectedStation;
		const title = `${station.name} (${station.alt} m, lat: ${station.lat}, lng: ${station.lon})`;

		this.graph = new Dygraph(
			this.graphDiv,
			props.data.getData(),
			deepMerge({
				title,
				drawCallback: this.drawCallbackHandler.bind(this),
				zoomCallback: this.dygraphYAxes.zoomCallback.bind(this.dygraphYAxes),
				interactionModel: Object.assign({}, Dygraph.defaultInteractionModel, {
					dblclick: this.dygraphYAxes.dblclick.bind(this.dygraphYAxes)
				}),
				dateWindow: props.dateRange,
				strokeWidth: 1,
				colorValue: 0.9,
				labels: this.makeLabels(props),
				legend: 'always',
				labelsDiv: this.labelsDiv,
				labelsSeparateLines: false,
				connectSeparatedPoints: true,
				labelsKMB: true,
				labelsUTC: true,
				digitsAfterDecimal: 4,
				axes: {
					x: {
						drawGrid: false,
						valueFormatter: this.formatDate.bind(this),
						axisLabelWidth: 68
					},
					y: {
						axisLabelWidth: 65,
						ticker: this.dygraphYAxes.tickerY1.bind(this.dygraphYAxes)
					},
					y2: {
						axisLabelWidth: 70,
						ticker: this.dygraphYAxes.tickerY2.bind(this.dygraphYAxes)
					}
				},
				series: makeSeriesOpt(props.data.series),
				visibility: this.getVisibility(props),
			}, props.graphOptions)
		);
	}

	drawCallbackHandler(graph){
		if (this.props.updateXRange) this.props.updateXRange(graph.xAxisRange());
	}

	formatDate(ms){
		const formatter = this.props.dateFormatter;
		//Firefox hack: add empty bold string
		return '<b></b>' + formatter ? formatter(ms) : new Date(ms).toUTCString();
	}

	makeLabels(props){
		return props.data.series.map(s => s.label);
	}

	getVisibility(props){
		return computeVisibility(this.makeLabels(props).slice(1), props.visibility);
	}

	componentWillReceiveProps(nextProps){
		this.axes = nextProps.axes;
		const nextVisibility = this.getVisibility(nextProps);
		const update = {};

		if (this.axes.ts !== this.lastAxesTs) {
			this.lastAxesTs = this.axes.ts;

			const data = nextProps.data.getData();
			const labels = this.makeLabels(nextProps).slice(1);
			const axelNumbers = labels.map(lbl => nextProps.axes.isLabelOnPrimary(lbl) ? 1 : 2);

			this.dygraphYAxes.initFromExternal(this.graph, data, nextVisibility, axelNumbers);
		}

		const nextRange = nextProps.dateRange;

		if(nextRange){
			const currRange = this.graph.xAxisRange();

			if(!currRange || nextRange[0] !== currRange[0] || nextRange[1] !== currRange[1]){
				Object.assign(update, {dateWindow: nextRange});
			}
		}

		const nextData = nextProps.data;
		if(nextData && nextData.id !== this.dataId){
			this.dataId = nextData.id;
			Object.assign(update, {
				file: nextProps.data.getData(),
				labels: this.makeLabels(nextProps),
				series: makeSeriesOpt(nextProps.data.series)
			});
		}

		if(!areEqualArrays(nextVisibility, this.getVisibility(this.props))){
			Object.assign(update, {visibility: nextVisibility});
		}

		const optionsWillUpdate = (Object.keys(update).length > 0);

		if(annotationsHaveBeenUpdated(this.props.annotations, nextProps.annotations)){
			this.graph.setAnnotations(nextProps.annotations, optionsWillUpdate); //avoiding double redrawing
		}

		if (optionsWillUpdate) this.graph.updateOptions(update);
	}

	render(){
		return (
			<div>
				<div ref={div => this.graphDiv = div} style={{width: '100%'}} />
				<div ref={div => this.labelsDiv = div} style={{width: '100%', fontSize: '1.1em', marginTop: 5}} />
			</div>
		);
	}
}

function computeVisibility(labels, visibilityObj){
	let visibility = visibilityObj || {};
	return labels.map(label => !!visibility[label]);
}

function areEqualArrays(a1, a2){
	if(!a1 || !a2 || a1.length !== a2.length) return false;
	return a1.every((a, i) => a === a2[i]);
}

function makeSeriesOpt(dyDataSeries){
	let opt = {};
	dyDataSeries.forEach((s, i) => {
		opt[s.label] = s.options;
	});
	return opt;
}

function annotationsHaveBeenUpdated(oldAnno, newAnno){
	if(!!oldAnno !== !!newAnno) return true;
	if(!newAnno) return false;
	if(oldAnno.length !== newAnno.length) return true;
	if(newAnno.length === 0) return false;

	return !oldAnno.every((oa, i) => oa.series === newAnno[i].series && oa.x === newAnno[i].x);
}
