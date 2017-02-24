export default class StationInfo {
	constructor(lat, lon, alt, siteId, name) {
		this._lat = lat || undefined,
		this._lon = lon || undefined,
		this._alt = alt || (lat && lon ? 100 : undefined),
		this._siteId = siteId || '',
		this._name = name || ''
	}

	withSelectedStation(selSt){
		return new StationInfo(selSt.lat, selSt.lon, selSt.alt, selSt.siteId, selSt.name);
	}

	withLat(lat){
		return new StationInfo(lat, this._lon, this._alt, this._siteId, this._name);
	}

	withLon(lon){
		return new StationInfo(this._lat, lon, this._alt, this._siteId, this._name);
	}

	get isExisting(){
		return !!this._siteId;
	}

	get hasPosition(){
		return !isNaN(parseFloat(this._lat)) && isFinite(this._lat)
			&& !isNaN(parseFloat(this._lon)) && isFinite(this._lon)
	}

	get lat(){
		return this._lat;
	}

	get lon(){
		return this._lon;
	}

	get alt(){
		return this._alt;
	}

	get siteId(){
		return this._siteId;
	}

	get name(){
		return this._name;
	}
}
