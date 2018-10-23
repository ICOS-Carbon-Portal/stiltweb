import React, { Component } from 'react';
import ReactDOM from 'react-dom';
import * as LCommon from 'icos-cp-leaflet-common';

const warningRadius = (lat => {
	return 20000 * Math.cos(Math.PI / 180 * lat);
});


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
			maskHole: undefined,
			isOutside: undefined
		};
	}

	componentDidMount() {
		const baseMaps = LCommon.getBaseMaps(21);

		const map = this.app.map = L.map(ReactDOM.findDOMNode(this.refs.map),
			{
				layers: [baseMaps.Topographic],
				worldCopyJump: false,
				continuousWorld: true,
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
				self.app.isOutside = isOutside(self.props.geoBoundary, e.latlng);
				mapClick(map, e.latlng, self);
			});
		}

		// Update map if we are returning to this view from another view
		this.componentWillReceiveProps(this.props);
	}

	componentWillReceiveProps(nextProps){
		const map = this.app.map;
		this.app.isOutside = isOutside(nextProps.geoBoundary, nextProps.selectedStation);

		this.buildWarningCircles(nextProps.workerMode, nextProps.stations);
		this.buildMarkers(nextProps.stations, nextProps.action, nextProps.selectedStation);
		this.panMap(nextProps.selectedStation, this.app.markers, map);

		if (!map.getZoom()) {
			if (nextProps.stations.length > 0){
				LCommon.setView(map, nextProps.stations);
			} else if (nextProps.geoBoundary){
				const bdry = nextProps.geoBoundary;
				map.fitBounds([[bdry.latMin, bdry.lonMin], [bdry.latMax, bdry.lonMax]]);
			} else {
				map.setView([55, 10], 3);
			}
		}

		this.updateClickMarker(map, nextProps.selectedStation);
		this.addMask(nextProps.geoBoundary);
	}

	panMap(selectedStation, markers, map){
		if (!map.getZoom()
			|| selectedStation === null
			|| selectedStation.lat === undefined
			|| selectedStation.lon === undefined
			|| selectedStation.lat === ''
			|| selectedStation.lon === ''
			|| markers.getLayers().length === 0
			|| this.app.isOutside) return;

		const mapBounds = map.getBounds();
		const selectedStationPosition = L.latLng(selectedStation.lat, selectedStation.lon);
		const markerOptions = markers.getLayers()[0].options;
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

	addMask(geoBoundary){
		const app = this.app;

		if(!geoBoundary || app.maskHole || !this.app.map.getZoom()) return;

		app.maskHole = LCommon.polygonMask(geoBoundary);
		app.maskHole.addTo(app.map);
	}

	updateClickMarker(map, selectedStation){
		if (!selectedStation) return;

		if (selectedStation.isExisting || !selectedStation.hasPosition) {
			map.removeLayer(this.app.clickMarker);
			this.app.clickMarker = L.circleMarker();
		} else if (selectedStation.hasPosition) {
			const clickMarkerPos = this.app.clickMarker.getLatLng();

			if (!clickMarkerPos || selectedStation.lat !== clickMarkerPos.lat || selectedStation.lon !== clickMarkerPos.lng) {
				mapClick(map, L.latLng(selectedStation.lat, selectedStation.lon), this, false)
			}
		}
	}

	buildWarningCircles(workerMode, geoms){
		if (!workerMode || geoms.length === 0) return;

		const circles = this.app.circles;
		circles.clearLayers();
		const map = this.app.map;
		const self = this;

		geoms.forEach(geom => {
			const circle = L.circle([geom.lat, geom.lon], {radius: warningRadius(geom.lat), color: 'red'});
			addPopup(circle, "Avoid adding new footprints here", {closeButton: false});

			circle.on('mousemove', function (e) {
				this.closePopup();
				this.openPopup(e.latlng);
			});
			circle.on('mouseout', function (e) {
				this.closePopup();
			});
			circle.on('click', function (e) {
				mapClick(map, e.latlng, self);
			});

			circles.addLayer(circle);
	 	});
	}

	buildMarkers(geoms, action, selectedStation){
		const markers = this.app.markers;
		markers.clearLayers();

		//First all non selected
		geoms.filter(geom => !selectedStation || geom.siteId !== selectedStation.siteId).forEach(geom => {
			const marker = L.circleMarker([geom.lat, geom.lon], LCommon.pointIcon(6, 1, 'rgb(255,100,100)', 'black'));

			addPopup(marker, getPopupTxt(geom), {offset:[0,0], closeButton: false});
			addEvents(this.app, marker, action, geom);

			markers.addLayer(marker);
		});

		//Then the selected
		const selected = geoms.find(geom => selectedStation && geom.siteId === selectedStation.siteId);
		if (selected){
			const marker = L.circleMarker([selected.lat, selected.lon], LCommon.pointIcon(8, 1, 'rgb(85,131,255)', 'black'));

			addPopup(marker, getPopupTxt(selected), {offset:[0,0], closeButton: false});
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

function getPopupTxt(station){
	return station.name
		? station.siteId + " (" + station.name + ")"
		: station.siteId;
}

function mapClick(map, clickedPosLatlng, self, triggerAction = true){
	const pos = roundPos(clickedPosLatlng);
	if (triggerAction) self.props.action(pos);
	map.removeLayer(self.app.clickMarker);

	if (self.app.isOutside){
		self.app.clickMarker = L.circleMarker(pos);
		self.props.toastError("The position is outside of the boundary!");
		return;
	}

	self.app.clickMarker = L.circleMarker(pos, LCommon.pointIcon(8, 1, 'rgb(85,131,255)', 'black'));
	map.addLayer(self.app.clickMarker);

	if (isTooClose(self.app.markers, pos)) {
		self.props.toastWarning("The position of your new footprint is too close to an existing footprint!");
	}
}

function roundPos(pos){
	return {
		lat: parseFloat(parseFloat(pos.lat).toFixed(2)),
		lng: parseFloat(parseFloat(pos.lng).toFixed(2))
	}
}

function isOutside(geoBoundary, pos){
	if (geoBoundary && pos.lat && (pos.lng || pos.lon)){
		const latLng = pos.hasOwnProperty("lng")
			? pos
			: {
				lat: parseFloat(pos.lat),
				lng: parseFloat(pos.lon)
			};

		return latLng.lat < geoBoundary.latMin || latLng.lat > geoBoundary.latMax
			|| latLng.lng < geoBoundary.lonMin || latLng.lng > geoBoundary.lonMax;
	} else {
		return false;
	}
}

function isTooClose(markers, clickedPosLatlng){
	let proximityFail = false;

	markers.eachLayer(marker => {
		if (marker.getLatLng().distanceTo(clickedPosLatlng) < warningRadius(clickedPosLatlng.lat)){
			proximityFail = true;

		}
	});

	return proximityFail;
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
