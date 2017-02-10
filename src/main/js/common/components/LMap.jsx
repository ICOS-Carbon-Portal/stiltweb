import React, { Component, PropTypes } from 'react';
import ReactDOM from 'react-dom';
import * as LCommon from 'icos-cp-leaflet-common';

const warningRadius = 20000;
const coordDecimals = 2;

export default class LMap extends Component{
	constructor(props){
		super(props);
		this.app = {
			map: null,
			markers: L.markerClusterGroup({
				maxClusterRadius: function(zoom){
					return 0.1;
				},
				spiderfyDistanceMultiplier: 1.8
			}),
			circles: L.layerGroup(),
			clickMarker: L.circleMarker(),
			clickedPos: null
		}
	}

	componentDidMount() {
		const baseMaps = LCommon.getBaseMaps(21);

		const map = this.app.map = L.map(ReactDOM.findDOMNode(this.refs.map),
			{
				layers: [baseMaps.Topographic],
				worldCopyJump: false,
				maxBounds: [[-90, -180],[90, 180]],
				attributionControl: false
			}
		);

		L.control.layers(baseMaps).addTo(map);
		map.addControl(new LCommon.CoordViewer({decimals: 4}));
		map.addLayer(this.app.circles);
		map.addLayer(this.app.markers);

		if(this.props.workerMode) {
			const self = this;

			map.on('click', function (e) {
				const lat = e.latlng.lat.toFixed(coordDecimals);
				const lon = e.latlng.lng.toFixed(coordDecimals);

				mapClick(map, lat, lon, self);
			});
		}
	}

	componentWillReceiveProps(nextProps){
		const prevProps = this.props;
		const clickedPos = this.app.clickedPos;
		const map = this.app.map;

		const buildMarkers = (nextProps.stations.length > 0 && prevProps.stations.length != nextProps.stations.length) ||
			(nextProps.selectedStation != undefined && prevProps.selectedStation != nextProps.selectedStation);
		const updateCircleMarker = nextProps.selectedStation != undefined && nextProps.selectedStation.id === undefined
			&& clickedPos != null && (nextProps.selectedStation.lat != clickedPos.lat || nextProps.selectedStation.lon != clickedPos.lon);

		// console.log({prevProps, nextProps, buildMarkers, updateCircleMarker, clickedPos});

		if (buildMarkers) {
			if (nextProps.workerMode) this.buildCircles(nextProps.stations);
			this.buildMarkers(nextProps.stations, nextProps.action, nextProps.selectedStation);

			if (nextProps.selectedStation == undefined) {

				LCommon.setView(map, nextProps.stations);

			} else {

				const mapBounds = map.getBounds();
				const selectedStationPosition = L.latLng(nextProps.selectedStation.lat, nextProps.selectedStation.lon);
				const markerOptions = this.app.markers.getLayers()[0].options;
				const markerPoint = map.latLngToLayerPoint(L.latLng(selectedStationPosition));
				const markerBoundaryLL = map.layerPointToLatLng(
					L.point(markerPoint.x - markerOptions.radius, markerPoint.y)
				);
				const markerBoundaryUR = map.layerPointToLatLng(
					L.point(markerPoint.x + markerOptions.radius, markerPoint.y)
				);
				const selectedStationBounds = L.latLngBounds(markerBoundaryLL, markerBoundaryUR);

				if (!mapBounds.contains(selectedStationBounds)){
					map.panTo(selectedStationPosition);
				}
			}
		}

		if (updateCircleMarker){
			mapClick(map, nextProps.selectedStation.lat, nextProps.selectedStation.lon, this);
		}
	}

	buildCircles(geoms){
		const circles = this.app.circles;
		circles.clearLayers();
		const map = this.app.map;
		const self = this;

		geoms.forEach(geom => {
			const circle = L.circle([geom.lat, geom.lon], warningRadius, {color: 'red'});
			addPopup(circle, "Avoid adding new footprints here", {closeButton: false});

			circle.on('mousemove', function (e) {
				this.closePopup();
				this.openPopup(e.latlng);
			});
			circle.on('mouseout', function (e) {
				this.closePopup();
			});
			circle.on('click', function (e) {
				const lat = e.latlng.lat.toFixed(coordDecimals);
				const lon = e.latlng.lng.toFixed(coordDecimals);
				mapClick(map, lat, lon, self);
			});

			circles.addLayer(circle);
		});
	}

	buildMarkers(geoms, action, selectedStation){
		const markers = this.app.markers;
		markers.clearLayers();

		//First all non selected
		geoms.filter(geom => !selectedStation || geom.id != selectedStation.id).forEach(geom => {
			const marker = L.circleMarker([geom.lat, geom.lon], LCommon.pointIcon(6, 1, 'rgb(255,100,100)', 'black'));

			addPopup(marker, geom.name + " (" + geom.id + ")", {offset:[0,0], closeButton: false});
			addEvents(this.app, marker, action, geom);

			markers.addLayer(marker);
		});

		//Then the selected
		const selected = geoms.find(geom => selectedStation && geom.id == selectedStation.id);
		if (selected){
			const marker = L.circleMarker([selected.lat, selected.lon], LCommon.pointIcon(8, 1, 'rgb(85,131,255)', 'black'));

			addPopup(marker, selected.name + " (" + selected.id + ")", {offset:[0,0], closeButton: false});
			addEvents(this.app, marker, action, selected);

			markers.addLayer(marker);
		}
	}

	shouldComponentUpdate(){
		return false;
	}

	componentWillUnmount() {
		this.app.map.off('click', this.onMapClick);
		this.app.map = null;
	}

	render() {
		return (
			<div ref='map' style={{width: '100%', height: '100%', display: 'block', border: '1px solid darkgrey'}}></div>
		);
	}
}

function mapClick(map, lat, lon, self){
	map.removeLayer(self.app.clickMarker);

	self.app.clickMarker = L.circleMarker([lat, lon], LCommon.pointIcon(8, 1, 'rgb(85,131,255)', 'black'));
	map.addLayer(self.app.clickMarker);
	self.app.clickedPos = {lat, lon};

	self.props.action(self.app.clickedPos);

	checkProximity(self.app.markers, self.app.clickedPos)
}

function checkProximity(markers, clickedPos){
	const clickedPos3857 = toWebMercator(parseFloat(clickedPos.lon), parseFloat(clickedPos.lat));
	var isInside = false;

	markers.eachLayer(marker => {
		const markerPos4326 = marker.getLatLng();
		const markerPos3857 = toWebMercator(markerPos4326.lng, markerPos4326.lat);

		if ((Math.pow(clickedPos3857.x - markerPos3857.x, 2) + Math.pow(clickedPos3857.y - markerPos3857.y, 2)) < Math.pow(warningRadius, 2)) {
			isInside = true;
			return;
		}

		// console.log({markerPos4326, markerPos3857, clickedPos3857, isInside});
	});

	console.log({isInside});
}

function toWebMercator(lon, lat) {
	if ((Math.abs(lon) > 180 || Math.abs(lat) > 90)) return null;

	const num = lon * 0.017453292519943295;
	const x = 6378137.0 * num;
	const a = lat * 0.017453292519943295;
	const y = 3189068.5 * Math.log((1.0 + Math.sin(a)) / (1.0 - Math.sin(a)));

	return {x,y};
}

function addPopup(marker, text, options){
	marker.bindPopup(LCommon.popupHeader(text), options);
}

function addEvents(app, marker, action, geom){
	marker.on('mouseover', function (e) {
		this.openPopup();
	});
	marker.on('mouseout', function (e) {
		this.closePopup();
	});

	marker.on('click', function(){
		app.map.removeLayer(app.clickMarker);

		action(geom);
	});
}