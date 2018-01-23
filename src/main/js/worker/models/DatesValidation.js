export default class DatesValidation{
	constructor(start, stop, minDate, maxDate, disabledDates){
		this._start = this.getDateObj(start);
		this._stop = this.getDateObj(stop);
		this._minDate = minDate;
		this._maxDate = maxDate;
		this._disabledDates = disabledDates || [];
		this._startErrorMsg = this.validateStart();
		this._stopErrorMsg = this.validateStop();
	}

	getDateObj(date){
		if (date === undefined) return undefined;
		// date as an object assumes it is defined with no offset ("Z")
		// date as string assumes ISO format without time (e.g. "2000-01-01")
		if (typeof date === "object"){
			return this.adjustDateToTZ(date);
		} else {
			return new Date(date);
		}
	}

	adjustDateToTZ(date){
		return new Date(date.getTime() - date.getTimezoneOffset() * 60 * 1000);
	}

	getISOStr(date){
		return date === undefined ? undefined : date.toISOString().substring(0, 10);
	}

	get start(){
		return this.getISOStr(this._start);
	}

	get stop(){
		return this.getISOStr(this._stop);
	}

	isInvalidDates(first, second){
		return first === undefined || second === undefined
			? false
			: first.getTime() > second.getTime();
	}

	validateStart(){
		let errorMsg = this.isInvalidDates(this._start, this._stop)
			? "Start date must be before stop date"
			: undefined;
		if (errorMsg) return errorMsg;

		errorMsg = this._minDate && this._start && this.isInvalidDates(this._minDate, this._start)
			? "Start date cannot be before " + this.getISOStr(this._minDate)
			: undefined;
		if (errorMsg) return errorMsg;

		errorMsg = this._maxDate && this._start && this.isInvalidDates(this._start, this._maxDate)
			? "Start date cannot be after " + this.getISOStr(this._maxDate)
			: undefined;
		if (errorMsg) return errorMsg;

		return this._disabledDates.length && this._start && this._disabledDates.some(d => d.getTime() === this._start.getTime())
			? "Start date cannot be one of these " + this._disabledDates.map(d => this.getISOStr(d)).join(', ')
			: undefined;
	}

	validateStop(){
		let errorMsg = this.isInvalidDates(this._start, this._stop)
			? "Start date must be before stop date"
			: undefined;
		if (errorMsg) return errorMsg;

		errorMsg = this._minDate && this._stop && this.isInvalidDates(this._minDate, this._stop)
			? "Stop date cannot be before " + this.getISOStr(this._minDate)
			: undefined;
		if (errorMsg) return errorMsg;

		errorMsg = this._maxDate && this._stop && this.isInvalidDates(this._stop, this._maxDate)
			? "Stop date cannot be after " + this.getISOStr(this._maxDate)
			: undefined;
		if (errorMsg) return errorMsg;

		return this._disabledDates && this._stop && this._disabledDates.some(d => d.getTime() === this._stop.getTime())
			? "Stop date cannot be one of these " + this._disabledDates.map(d => this.getISOStr(d)).join(', ')
			: undefined;
	}

	get startError(){
		return this._startErrorMsg;
	}

	get stopError(){
		return this._stopErrorMsg;
	}
}
