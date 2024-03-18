import React, { Component } from 'react';
import ReactDOM from 'react-dom';
import * as LCommon from 'icos-cp-leaflet-common';
import {MarkerClusterGroup} from 'leaflet.markercluster';
import config from '../../worker/config'

const {geoBoundary, proximityTolerance} = config

export default class LMap extends Component{
	constructor(props){
		super(props);
		this.app = {
			map: null,
			markers: new MarkerClusterGroup({
				maxClusterRadius: function(zoom){
					return 0.1;
				},
				spiderfyDistanceMultiplier: 1.8
			}),
			circles: L.layerGroup(),
			clickMarker: L.circleMarker(),
			maskHole: undefined,
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
				mapClick(map, e.latlng, self);
			});
		}

		// Update map if we are returning to this view from another view
		this.componentWillReceiveProps(this.props);
	}

	componentWillReceiveProps(nextProps){
		const map = this.app.map;

		if (nextProps.workerMode) this.buildWarningCircles(nextProps.stations)
		this.buildMarkers(nextProps.stations, nextProps.action, nextProps.selectedStation);
		this.panMap(nextProps.selectedStation, this.app.markers, map);

		if (!map.getZoom()) {
			if (nextProps.stations.length > 0){
				LCommon.setView(map, nextProps.stations);
			} else {
				const bdry = geoBoundary;
				map.fitBounds([[bdry.latMin, bdry.lonMin], [bdry.latMax, bdry.lonMax]]);
			}
		}

		this.addMask()
	}

	panMap(selectedStation, markers, map){
		if (!map.getZoom()
			|| !selectedStation
			|| !Number.isFinite(selectedStation.lat)
			|| !Number.isFinite(selectedStation.lon)
			|| markers.getLayers().length === 0) return

		const mapBounds = map.getBounds();
		const selectedStationPosition = L.latLng(selectedStation.lat, selectedStation.lon);
		const markerOptions = markers.getLayers()[0].options;
		const markerPoint = map.latLngToLayerPoint(selectedStationPosition)
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

	addMask(){
		const app = this.app;

		if(!this.props.workerMode || app.maskHole || !this.app.map.getZoom()) return;

		app.maskHole = LCommon.polygonMask(geoBoundary);
		app.maskHole.addTo(app.map);
	}

	buildWarningCircles(stations){
		if (stations.length === 0) return;

		const circles = this.app.circles;
		circles.clearLayers();
		const map = this.app.map;
		const self = this;

		stations.forEach(st => {
			const circle = L.circle([st.lat, st.lon], {radius: proximityTolerance, color: 'red'})

			circle.on('click', function (e) {
				mapClick(map, e.latlng, self)
			})

			circles.addLayer(circle)
		})
	}

	buildMarkers(stations, action, selectedStation){
		const markers = this.app.markers;
		markers.clearLayers();

		stations.forEach(st => {
			const marker = L.circleMarker([st.lat, st.lon], LCommon.pointIcon(6, 1, 'rgb(255,100,100)', 'black'));

			addPopup(marker, getPopupTxt(st), {offset:[0,0], closeButton: false});
			addEvents(this.app, marker, action, st);

			markers.addLayer(marker);
		})

		if(selectedStation && Number.isFinite(selectedStation.lat) && Number.isFinite(selectedStation.lon)){
			const clickMarkerPos = this.app.clickMarker.getLatLng()
			if (!clickMarkerPos || selectedStation.lat !== clickMarkerPos.lat || selectedStation.lon !== clickMarkerPos.lng) {
				mapClick(this.app.map, L.latLng(selectedStation.lat, selectedStation.lon), this, false)
			}
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
	if (triggerAction) self.props.action({lat: pos.lat, lon: pos.lng, siteId: null})
	map.removeLayer(self.app.clickMarker);

	self.app.clickMarker = L.circleMarker(pos, LCommon.pointIcon(8, 1, 'rgb(85,131,255)', 'black'));
	map.addLayer(self.app.clickMarker);

}

function roundPos(pos){
	return {
		lat: parseFloat(parseFloat(pos.lat).toFixed(2)),
		lng: parseFloat(parseFloat(pos.lng).toFixed(2))
	}
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
