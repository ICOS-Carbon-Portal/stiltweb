import StationInfo from './StationInfo';
import WorkerFormData from './WorkerFormData';

export default class WorkerData{
	constructor(stations, workerFormData, selectedStation, errors){
		this._stations = stations || [];
		this._workerFormData = workerFormData || new WorkerFormData();
		this._selectedStation = selectedStation || new StationInfo();
		this._errors = errors || {
			lat: undefined,
			lon: undefined,
			alt: undefined,
			siteId: undefined,
			start: undefined,
			stop: undefined
		};
	}

	withExistingStationData(){
		return new WorkerData(
			this._stations,
			new WorkerFormData(
				this._selectedStation.lat,
				this._selectedStation.lon,
				this._selectedStation.alt,
				this._selectedStation.siteId,
				this._workerFormData.start,
				this._workerFormData.stop
			),
			this._selectedStation,
			this._errors
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

		return new WorkerData(stationsFormatted, this._workerFormData, this._selectedStation,this._errors);
	}

	resetAndAddNewStation(station){
		return new WorkerData(this._stations.concat([station]));
	}

	get stations(){
		return this._stations;
	}

	get hasErrors(){
		const props = Object.keys(this._errors);
		return !!props.find(key => !!this._errors[key], this);
	}

	withUpdatedFormData(update){
		const key = update.propertyName;
		Object.assign(this._errors, {[key]: update.error});

		if (key) {
			const val = update.value;

			switch(key) {
				case "siteId":
					const existingStation = this._stations.find(station => station.siteId == val);

					if (existingStation) {
						return new WorkerData(this._stations, this._workerFormData.withUpdate(key, val), this._selectedStation.withSelectedStation(existingStation), this._errors);
					} else if (this.selectedStation.isExisting) {
						return new WorkerData(this._stations, this._workerFormData.withUpdate(key, val), undefined, this._errors);
					} else {
						return new WorkerData(this._stations, this._workerFormData.withUpdate(key, val), this._selectedStation, this._errors);
					}

				case "lat":
					return new WorkerData(this._stations, this._workerFormData.withUpdate(key, val), this._selectedStation.withLat(val), this._errors);

				case "lon":
					return new WorkerData(this._stations, this._workerFormData.withUpdate(key, val), this._selectedStation.withLon(val), this._errors);

				default:
					return new WorkerData(this._stations, this._workerFormData.withUpdate(key, val), this._selectedStation, this._errors);
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
		const isExisting = this.selectedStation.isExisting;

		return this.isJobDefComplete
			? {
				lat: round(getDataforForm(this._workerFormData, this._selectedStation, 'lat', isExisting)),
				lon: round(getDataforForm(this._workerFormData, this._selectedStation, 'lon', isExisting)),
				alt: getDataforForm(this._workerFormData, this._selectedStation, 'alt', isExisting),
				siteId: this._workerFormData.siteId,
				start: this._workerFormData.start,
				stop: this._workerFormData.stop
			}
			: {};
	}

	withSelectedStation(selectedStation, isSourceMapClick){
		const isExisting = !!selectedStation.siteId;
		const siteId = isSourceMapClick
			? this.isFormSiteIdExisting(this._workerFormData.siteId)
				? ''
				: this._workerFormData.siteId
			: selectedStation.siteId;

		const workerFormData = isSourceMapClick
			? new WorkerFormData(
				selectedStation.lat,
				selectedStation.lon || selectedStation.lng,
				selectedStation.alt,
				siteId,
				this._workerFormData.start,
				this._workerFormData.stop)
			: isExisting && this._workerFormData.lat && this._workerFormData.lon
				? this._workerFormData // Do not overwrite form if it has values
				: new WorkerFormData(
					selectedStation.lat,
					selectedStation.lon || selectedStation.lng,
					selectedStation.alt,
					siteId,
					this._workerFormData.start,
					this._workerFormData.stop)

		return new WorkerData(
			this._stations,
			workerFormData,
			new StationInfo(selectedStation.lat, selectedStation.lon || selectedStation.lng, selectedStation.alt, selectedStation.siteId, selectedStation.name),
			this._errors
		);
	}

	withClearedSelectedStation(){
		return new WorkerData(this._stations, this._workerFormData, undefined, this._errors);
	}

	get selectedStation(){
		return this._selectedStation;
	}

	get isFormAndSelStationSame(){
		return (this._selectedStation.lat === this._workerFormData.lat || !this._workerFormData.lat)
			&& (this._selectedStation.lon === this._workerFormData.lon || !this._workerFormData.lon)
			&& (this._selectedStation.alt === this._workerFormData.alt || !this._workerFormData.alt)
			&& (this._selectedStation.siteId === this._workerFormData.siteId || !this._workerFormData.siteId);
	}

	get isFormAndExistingStationDifferent(){
		return this._selectedStation.isExisting && !this.isFormAndSelStationSame;
	}

	isFormSiteIdExisting(siteId){
		return this._stations.some(st => st.siteId == siteId);
	}

	get isJobDefComplete(){
		const isExisting = this.selectedStation.isExisting;
		const isSiteIdAllowed = (isExisting
			&& this._selectedStation.lat == this._workerFormData.lat
			&& this._selectedStation.lon == this._workerFormData.lon
			&& this._selectedStation.alt == this._workerFormData.alt
			&& this._selectedStation.siteId == this._workerFormData.siteId)
			|| (!isExisting && !!this._workerFormData.siteId);

		return !this.hasErrors && isSiteIdAllowed
			&& !!getDataforForm(this._workerFormData, this._selectedStation, 'lat', isExisting)
			&& !!getDataforForm(this._workerFormData, this._selectedStation, 'lon', isExisting)
			&& !!getDataforForm(this._workerFormData, this._selectedStation, 'alt', isExisting)
			&& !!this._workerFormData.siteId && this._workerFormData.siteId.length >= 3
			&& !!this._workerFormData.start
			&& !!this._workerFormData.stop;
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
	if (val && isNumber(val)){
		const valStr = '' + val;

		if (valStr.match(/\.$/) || valStr.match(/\.0+$/)) {
			return val;
		} else {
			return parseFloat(parseFloat(val).toFixed(2));
		}
	} else {
		return val;
	}
}

function isNumber(n){
	return !isNaN(parseFloat(n)) && isFinite(n);
}