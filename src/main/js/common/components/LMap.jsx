import React, { Component } from 'react';
import ReactDOM from 'react-dom';
import * as LCommon from 'icos-cp-leaflet-common';
import {MarkerClusterGroup} from 'leaflet.markercluster';
import config from '../../worker/config'

const {geoBoundary, proximityTolerance} = config
const stationIcon = LCommon.pointIcon(6, 1, 'rgb(255,100,100)', 'black')
const selectedStationIcon = LCommon.pointIcon(8, 1, 'rgb(85,131,255)', 'black')

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
		}
		const self = this
		self.onMapClick = e => self.mapClick(e.latlng)
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
		)

		L.control.layers(baseMaps).addTo(map);
		map.addControl(new LCommon.CoordViewer({decimals: 4}));
		map.addLayer(this.app.circles);
		map.addLayer(this.app.markers);

		if(this.props.workerMode) {
			map.on('click', this.onMapClick)
			LCommon.polygonMask(geoBoundary).addTo(map)
		}

		if(this.props.stations.length <= 1){
			const {latMin, latMax, lonMin, lonMax} = geoBoundary
			map.fitBounds([[latMin, lonMin], [latMax, lonMax]])
		}

		// the rest of map initialization
		this.componentWillReceiveProps(this.props);
	}

	componentWillReceiveProps(nextProps){
		const map = this.app.map
		const {stations, workerMode} = nextProps

		if (stations.length > 1){
			const stCoords = stations.map(s => L.latLng(s.lat, s.lon))
			const selPos = this.getSelectedPos(nextProps)
			if (selPos) stCoords.push(selPos)
			map.fitBounds(L.latLngBounds(stCoords))
		}

		if (workerMode) this.buildWarningCircles(stations)
		this.buildMarkers(nextProps)
	}

	buildWarningCircles(stations){
		if (stations.length === 0) return;

		const circles = this.app.circles;
		circles.clearLayers();
		const self = this;

		stations.forEach(st => {
			const circle = L.circle([st.lat, st.lon], {radius: proximityTolerance, color: 'red'})

			circle.on('click', self.onMapClick)

			circles.addLayer(circle)
		})
	}

	buildMarkers(props){
		const app = this.app
		app.markers.clearLayers()

		props.stations.forEach(st => {
			const marker = L.circleMarker([st.lat, st.lon], stationIcon)
			const popupTxt = st.name
					? st.siteId + " (" + st.name + ")"
					: st.siteId;

			marker.bindPopup(LCommon.popupHeader(popupTxt), {offset:[0,0], closeButton: false})

			marker.on('mouseover', () => marker.openPopup())
			marker.on('mouseout',  () => marker.closePopup())
			marker.on('click',     () => props.action(st))

			app.markers.addLayer(marker)
		})

		const selPos = this.getSelectedPos(props)
		if(selPos) {
			app.map.removeLayer(app.clickMarker)
			app.clickMarker = L.circleMarker(selPos, selectedStationIcon)
			app.map.addLayer(app.clickMarker)
		}
	}

	getSelectedPos(props){
		const ss = props.selectedStation
		if(!ss || !Number.isFinite(ss.lat) || !Number.isFinite(ss.lon)) return null
		return L.latLng(ss.lat, ss.lon)
	}

	shouldComponentUpdate(){
		return false;
	}

	componentWillUnmount() {
		const {app} = this
		this.props.action({lat: NaN, lon: NaN, siteId: null})
		app.map.off('click')
		app.circles.eachLayer(c => c.off('click'))
		app.markers.eachLayer(m => {
			m.off('mouseover')
			m.off('mouseout')
			m.off('click')
		})
		app.circles.clearLayers()
		app.markers.clearLayers()
		app.map.eachLayer(l => app.map.removeLayer(l))
		app.map = null
	}

	mapClick(pos){
		this.props.action({
			lat: parseFloat(parseFloat(pos.lat).toFixed(2)),
			lon: parseFloat(parseFloat(pos.lng).toFixed(2)),
			alt: NaN,
			siteId: null
		})
	}

	render() {
		return (
			<div ref='map' style={{width: '100%', height: '100%', display: 'block', border: '1px solid darkgrey'}}></div>
		);
	}
}
