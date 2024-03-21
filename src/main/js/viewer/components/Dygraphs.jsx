import React, { Component } from 'react';
import Dygraph from 'dygraphs';
import './Dygraphs.css';
import {deepMerge, debounce} from 'icos-cp-utils';
import DygraphYAxes from '../models/DygraphYAxes';
import Marker from '../models/Marker';


const canvasMarker = {
	leftPadding: 15,
	lineWidth: 1,
	markerOffset: 3
};

export default class Dygraphs extends Component {
	constructor(props) {
		super(props);

		this.footprintDates = undefined;
		this.footprint = {
			index: undefined
		};
		this.xAxisRange = [];
		this.dataId = undefined;
		this.graph = undefined;
		this.axes = props.axes;
		this.lastAxesTs = undefined;
		this.dygraphYAxes = new DygraphYAxes();
		this.canvasOverlay = null;
		this.marker = null;
	}

	addEvents(){
		this.resize = debounce(this.resizeHandler.bind(this));
		window.addEventListener("resize", this.resize);

		this.mousemoveCOHandler = this.canvasOverlayMousemove.bind(this);
		this.canvasOverlay.addEventListener("mousemove", this.mousemoveCOHandler);

		this.graphOut = this.marker.dragEnd.bind(this.marker);
		this.graphDiv.addEventListener("mouseleave", this.graphOut);
		this.graphDiv.addEventListener("mouseup", this.graphOut);
	}

	componentWillUnmount(){
		this.marker.release();
		window.removeEventListener("resize", this.resize);
		this.canvasOverlay.removeEventListener("mousemove", this.mousemoveCOHandler);
		this.graphDiv.removeEventListener("mouseleave", this.graphOut);
		this.graphDiv.removeEventListener("mouseup", this.graphOut);
		this.graph = undefined
	}

	resizeHandler(){
		this.markFootprint();
		this.marker.xRange = this.props.dateRange.map(x => this.graph.toDomXCoord(x));
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
					dblclick: this.dblclickHandler.bind(this)
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

		const {canvasOverlay, marker} = createCanvasOverlay(
			this.graphDiv,
			this.graph,
			this.markerEndCallback.bind(this)
		);
		this.canvasOverlay = canvasOverlay;
		this.marker = marker;
		this.graphDiv.firstChild.appendChild(this.canvasOverlay);

		this.addEvents();
	}

	drawCallbackHandler(graph){
		if (this.props.updateXRange) this.props.updateXRange(graph.xAxisRange());
	}

	dblclickHandler(){
		this.dygraphYAxes.dblclick();
		this.markFootprint();
	}

	markFootprint(){
		const footprintDate = new Date(this.footprint.date);
		const x = this.graph.toDomXCoord(footprintDate);
		this.canvasOverlay.style.left = x - canvasMarker.markerOffset + 1 + 'px';
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
		const data = nextProps.data.getData();
		this.footprintDates = nextProps.footprints.dates;
		this.axes = nextProps.axes;
		const nextRange = nextProps.dateRange;
		this.marker.xRange = nextProps.dateRange.map(x => this.graph.toDomXCoord(x));

		const nextVisibility = this.getVisibility(nextProps);
		const update = {};

		if (this.axes.ts !== this.lastAxesTs) {
			this.lastAxesTs = this.axes.ts;

			const labels = this.makeLabels(nextProps).slice(1);
			const axelNumbers = labels.map(lbl => nextProps.axes.isLabelOnPrimary(lbl) ? 1 : 2);

			this.dygraphYAxes.initFromExternal(this.graph, data, nextVisibility, axelNumbers);
		}

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
				file: data,
				labels: this.makeLabels(nextProps),
				series: makeSeriesOpt(nextProps.data.series)
			});
		}

		if(!areEqualArrays(nextVisibility, this.getVisibility(this.props))){
			Object.assign(update, {visibility: nextVisibility});
		}

		const optionsWillUpdate = (Object.keys(update).length > 0);

		if (nextProps.footprint && this.footprint.index !== nextProps.footprint.index) {
			this.footprint = nextProps.footprint;
			this.markFootprint();

			this.graph.mouseMove_(400);
		}

		if (optionsWillUpdate) this.graph.updateOptions(update);
	}

	canvasOverlayMousemove(e){
		const x = this.marker.mapPosition(e.clientX);
		const closestIndex = this.getClosestDateIndex(x);
		this.labelsDiv.firstChild.nodeValue = this.formatDate(this.footprintDates[closestIndex]);
	}

	getClosestDateIndex(x){
		const markerDate = this.graph.toDataXCoord(x);
		const dateIdx = this.footprintDates.findIndex(footprintDate => footprintDate > markerDate);

		if (dateIdx === -1)
			return this.footprintDates.length - 1;

		const dateBeforeMarker = this.footprintDates[dateIdx - 1];
		const dateAfterMarker = this.footprintDates[dateIdx];
		return markerDate - dateBeforeMarker < dateAfterMarker - markerDate
			? dateIdx - 1
			: dateIdx;
	}

	markerEndCallback(clientX){
		const x = this.marker.mapPosition(clientX);
		const closestIndex = this.getClosestDateIndex(x);

		if (closestIndex === this.footprint.index) {
			this.markFootprint();
		} else {
			this.props.jumpToFootprint(closestIndex);
		}
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

const createCanvasOverlay = (graphElement, graph, markerEndCallback) => {
	const canvasOverlay = document.createElement("canvas");
	canvasOverlay.id = "animationMarkerCanvas";
	canvasOverlay.width = canvasMarker.markerOffset * 2 + canvasMarker.lineWidth;
	const titleHeight = graph.getOption('titleHeight');
	const xLabelHeight = graph.getOption('xLabelHeight');
	const top = titleHeight + 5;
	canvasOverlay.height = graph.canvas_.height - top - xLabelHeight - 20;
	canvasOverlay.style = 'display:inline; position:absolute; z-index:99; cursor:ew-resize; top:' + top + 'px;';

	const ctx = canvasOverlay.getContext('2d');
	ctx.beginPath();
	ctx.moveTo(canvasMarker.markerOffset, 0);
	ctx.lineTo(canvasMarker.markerOffset, canvasOverlay.height);
	ctx.lineWidth = canvasMarker.lineWidth;
	ctx.strokeStyle = 'red';
	ctx.stroke();

	const marker = new Marker(
		graphElement,
		canvasOverlay,
		canvasMarker.leftPadding,
		canvasMarker.markerOffset,
		markerEndCallback
	);

	return {canvasOverlay, marker};
};
