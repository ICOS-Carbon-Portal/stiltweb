export default class DatesValidation{
	constructor(start, stop){
		this._start = this.getDateObj(start);
		this._stop = this.getDateObj(stop);
	}

	getDateObj(date){
		// date as an object assumes it is defined with no offset ("Z")
		// date as string assumes ISO format without time (e.g. "2000-01-01")
		if (typeof date == "object"){
			return this.adjustDateToTZ(date);
		} else {
			return new Date(date);
		}
	}

	adjustDateToTZ(date){
		return new Date(date.getTime() - date.getTimezoneOffset() * 60 * 1000);
	}

	getISOStr(date){
		return isNaN(date.getTime())
			? undefined
			: date.toISOString().substring(0, 10);
	}

	get start(){
		return this.getISOStr(this._start);
	}

	get stop(){
		return this.getISOStr(this._stop);
	}

	get startError(){
		return this.isError
			? "Start date must be before stop date"
			: undefined;
	}

	get stopError(){
		return this.isError
			? "Stop date must be after start date"
			: undefined;
	}

	get isError(){
		if (isNaN(this._start.getTime()) || isNaN(this._stop.getTime())){
			return false;
		} else if (this._start.getTime() >= this._stop.getTime()){
			return true;
		} else {
			return false;
		}
	}
}