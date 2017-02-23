import StationInfo from './StationInfo';
import WorkerFormData from './WorkerFormData';

export default class WorkerData{
	constructor(stations, workerFormData, selectedStation){
		this._stations = stations || [];
		this._workerFormData = workerFormData || new WorkerFormData();
		this._selectedStation = selectedStation || new StationInfo();
	}

	withExistingStationData(){
		return new WorkerData(
			this._stations,
			new WorkerFormData(this._selectedStation.lat, this._selectedStation.lon, this._selectedStation.alt, this._selectedStation.siteId),
			this._selectedStation
		);
	}

	withStations(stations){
		const stationsFormatted = stations.map(s => {
			return {
				siteId: s.id,
				lat: s.lat,
				lon: s.lon,
				alt: s.alt,
				name: s.name
			};
		});

		return new WorkerData(stationsFormatted, this._workerFormData, this._selectedStation);
	}

	resetAndAddNewStation(station){
		return new WorkerData(this._stations.concat([station]));
	}

	get stations(){
		return this._stations;
	}

	withUpdatedFormData(update){
		const keys = Object.keys(update);
		const key = keys.length == 1 ? keys[0] : undefined;
		const val = update[key];

		if (val) {
			switch(key) {
				case "siteId":
					const existingStation = this._stations.find(station => station.siteId == val);
					const workerData = new WorkerData(this._stations, this._workerFormData.withUpdate(key, val), this._selectedStation);

					if (existingStation)
						return this.withSelectedStation(existingStation, false);
					else if (this.selectedStation.isExisting)
						return this.withClearedSelectedStation();
					else
						return workerData;

				case "lat":
					return new WorkerData(this._stations, this._workerFormData.withUpdate(key, val), this._selectedStation.withLat(val));

				case "lon":
					return new WorkerData(this._stations, this._workerFormData.withUpdate(key, val), this._selectedStation.withLon(val));

				default:
					return new WorkerData(this._stations, this._workerFormData.withUpdate(key, val), this._selectedStation);
			}
		} else {
			return this;
		}
	}

	get formData(){
		const isExisting = this.selectedStation.isExisting;

		return {
			siteId: getDataforForm(this._workerFormData, this._selectedStation, 'siteId', isExisting),
			lat: round(getDataforForm(this._workerFormData, this._selectedStation, 'lat', isExisting)),
			lon: round(getDataforForm(this._workerFormData, this._selectedStation, 'lon', isExisting)),
			alt: getDataforForm(this._workerFormData, this._selectedStation, 'alt', isExisting),
			start: this._workerFormData.start,
			stop: this._workerFormData.stop
		};
	}

	get jobDef(){
		return this.isJobDefComplete
			? {
				lat: round(this._workerFormData.lat),
				lon: round(this._workerFormData.lon),
				alt: this._workerFormData.alt,
				siteId: this._workerFormData.siteId,
				start: this._workerFormData.start,
				stop: this._workerFormData.stop
			}
			: {};
	}

	withSelectedStation(selectedStation, isSourceMapClick){
		const isExisting = !!selectedStation.siteId;

		const workerFormData = isSourceMapClick
			? new WorkerFormData(selectedStation.lat, selectedStation.lon || selectedStation.lng, selectedStation.alt, selectedStation.siteId)
			: isExisting && this._workerFormData.lat && this._workerFormData.lon
				? this._workerFormData // Do not overwrite form if it has values
				: new WorkerFormData(selectedStation.lat, selectedStation.lon || selectedStation.lng, selectedStation.alt, selectedStation.siteId)

		return new WorkerData(
			this._stations,
			workerFormData,
			new StationInfo(selectedStation.lat, selectedStation.lon || selectedStation.lng, selectedStation.alt, selectedStation.siteId, selectedStation.name)
		);
	}

	withClearedSelectedStation(){
		return new WorkerData(this._stations, this._workerFormData, undefined);
	}

	get selectedStation(){
		return this._selectedStation;
	}

	get isFormAndSelStationSame(){
		return this._selectedStation.lat === this._workerFormData.lat
			&& this._selectedStation.lon === this._workerFormData.lon
			&& this._selectedStation.alt === this._workerFormData.alt
			&& this._selectedStation.siteId === this._workerFormData.siteId;
	}

	get isFormAndExistingStationDifferent(){
		return this._selectedStation.isExisting && !this.isFormAndSelStationSame;
	}

	get isJobDefComplete(){
		if (this._selectedStation.isExisting){
			return false;
		} else {
			return !!this._workerFormData.lat
				&& !!this._workerFormData.lon
				&& !!this._workerFormData.alt
				&& this._workerFormData.siteId && this._workerFormData.siteId.length >= 3
				&& !!this._workerFormData.start
				&& !!this._workerFormData.stop;
		}
	}
}

function getDataforForm(workerFormData, selectedStation, key, isExisting){
	if (isExisting){
		return workerFormData[key] ? workerFormData[key] : selectedStation[key];
	} else {
		return workerFormData[key] || (isExisting ? undefined : selectedStation[key]);
	}
}

function round(val){
	if (val) {
		return parseFloat(val.toFixed(2));
	} else {
		return val;
	}
}