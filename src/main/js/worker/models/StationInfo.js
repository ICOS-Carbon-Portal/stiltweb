export default class StationInfo {
	constructor(lat, lon, alt, siteId, name) {
		this._lat = lat || undefined,
		this._lon = lon || undefined,
		this._alt = alt || (lat && lon ? 100 : undefined),
		this._siteId = siteId || '',
		this._name = name || ''
	}

	get isExisting(){
		return !!this._siteId;
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
