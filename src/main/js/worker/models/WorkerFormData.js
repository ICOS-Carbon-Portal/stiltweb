export default class WorkerFormData {
	constructor(lat, lon, alt, siteId, start, stop) {
		this._lat = lat || undefined,
		this._lon = lon || undefined,
		this._alt = alt || (lat && lon ? 100 : undefined),
		this._siteId = siteId || undefined,
		this._start = start || undefined,
		this._stop = stop || undefined
	}

	withUpdate(updateKey, updateVal){
		switch(updateKey){
			case "lat":
				return Object.assign(this, {_lat: updateVal});
			case "lon":
				return Object.assign(this, {_lon: updateVal});
			case "alt":
				return Object.assign(this, {_alt: updateVal});
			case "siteId":
				return Object.assign(this, {_siteId: updateVal});
			case "start":
				return Object.assign(this, {_start: updateVal});
			case "stop":
				return Object.assign(this, {_stop: updateVal});
			default:
				return this;
		}
	}

	withSelectedStation(selectedStation){
		return new WorkerFormData(
			selectedStation.lat,
			selectedStation.lon,
			selectedStation.alt,
			selectedStation.siteId,
			this._start,
			this._stop
		);
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

	get start(){
		return this._start;
	}

	get stop(){
		return this._stop;
	}

	get isComplete(){
		return this._lat !== undefined
			&& this._lon !== undefined
			&& this._alt !== undefined
			&& this._siteId !== undefined
			&& this._start !== undefined
			&& this._stop !== undefined
			&& Date.parse(this._stop) > Date.parse(this._start);
	}
}