import React, { Component, PropTypes } from 'react';
import ReactDOM from 'react-dom';
import * as LCommon from 'icos-cp-leaflet-common';

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
			clickMarker: L.circleMarker()
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
				mapClick(map, e, self);
			});
		}
	}

	componentWillReceiveProps(nextProps){
		const prevProps = this.props;
		const buildMarkers = (nextProps.stations.length > 0 && prevProps.stations.length != nextProps.stations.length) ||
			(nextProps.selectedStation != undefined && prevProps.selectedStation != nextProps.selectedStation);

		if (buildMarkers) {
			const map = this.app.map;

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
	}

	buildCircles(geoms){
		const circles = this.app.circles;
		circles.clearLayers();

		geoms.forEach(geom => {
			circles.addLayer(L.circle([geom.lat, geom.lon], 50000, {color: 'red'}));
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

function mapClick(map, e, self){
	map.removeLayer(self.app.clickMarker);

	const lat = e.latlng.lat.toFixed(5);
	const lon = e.latlng.lng.toFixed(5);

	self.app.clickMarker = L.circleMarker([lat, lon], LCommon.pointIcon(8, 1, 'rgb(85,131,255)', 'black'));
	map.addLayer(self.app.clickMarker);

	self.props.action({lat, lon});
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