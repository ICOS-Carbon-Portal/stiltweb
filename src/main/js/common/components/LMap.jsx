import React, { Component, PropTypes } from 'react';
import ReactDOM from 'react-dom';
import * as LCommon from 'icos-cp-leaflet-common';

const warningRadius = (lat => {
	return 20000 * Math.cos(Math.PI / 180 * lat);
});
const coordDecimals = 2;
// const mask = L.latLngBounds(
// 	L.latLng(latMin, lonMin),
// 	L.latLng(latMax, lonMax)
// );

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
		};
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
				mapClick(map, e.latlng, self);
			});
		}
		//a hack to fix the spagetti situation in this.applyChanges (former this.componentWillReceiveProps)
		//it simulates the situation of first getting the empty station list, and then non-empty station list with props,
		//even if the station list is non-empty from the beginning
		if(this.props.stations.length) {
			const noStationsProps = Object.assign({}, this.props, {stations: []});
			this.applyChanges(this.props, noStationsProps);
		}
	}

	componentWillReceiveProps(nextProps){
		this.applyChanges(nextProps, this.props);
	}

	applyChanges(nextProps, prevProps){
		const clickedPos = this.app.clickedPos;
		const map = this.app.map;

		const buildMarkers = (nextProps.stations.length > 0 && prevProps.stations.length != nextProps.stations.length) ||
			(nextProps.selectedStation != undefined && prevProps.selectedStation != nextProps.selectedStation);
		const updateCircleMarker = nextProps.selectedStation != undefined && nextProps.selectedStation.id === undefined
			&& clickedPos != null && (nextProps.selectedStation.lat != clickedPos.lat || nextProps.selectedStation.lon != clickedPos.lon);

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

function mapClick(map, clickedPosLatlng, self){
	map.removeLayer(self.app.clickMarker);

	self.app.clickMarker = L.circleMarker(clickedPosLatlng, LCommon.pointIcon(8, 1, 'rgb(85,131,255)', 'black'));
	map.addLayer(self.app.clickMarker);

	self.app.clickedPos = {
		lat: clickedPosLatlng.lat.toFixed(coordDecimals),
		lon: clickedPosLatlng.lng.toFixed(coordDecimals)
	};

	self.props.action(self.app.clickedPos);

	if (checkProximity(self.app.markers, clickedPosLatlng)){
		self.props.toastWarning("The position of your new footprint is too close to an existing footprint!");
	}
}

function checkProximity(markers, clickedPosLatlng){
	var isTooClose = false;

	markers.eachLayer(marker => {
		if (marker.getLatLng().distanceTo(clickedPosLatlng) < warningRadius(clickedPosLatlng.lat)){
			isTooClose = true;
			return;
		}
	});

	return isTooClose;
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
