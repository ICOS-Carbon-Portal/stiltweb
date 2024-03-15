export default class DatesValidation{
	constructor(start, stop, minDate, maxDate, disabledDates, disabledMonths){
		this._start = this.getDateObj(start);
		this._stop = this.getDateObj(stop);
		this._minDate = minDate;
		this._maxDate = maxDate;
		this._disabledDates = disabledDates || [];
		this._disabledMonths = disabledMonths || [];
		this._startErrorMsg = this.validateStart();
		this._stopErrorMsg = this.validateStop();
		this._gapWarning = this._startErrorMsg === undefined && this._stopErrorMsg === undefined
			? this.validateForGapWarning()
			: undefined;
	}

	getDateObj(date){
		if (!date ) return date
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
		return date ? date.toISOString().substring(0, 10) : date
	}

	get start(){
		return this.getISOStr(this._start);
	}

	get stop(){
		return this.getISOStr(this._stop);
	}

	isInvalidDates(first, second){
		return (!first || !second) ? false : first.getTime() > second.getTime()
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

		return this.validateForGapError()
			? "There is no meteorological input data in the time frame you specified"
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

		return this.validateForGapError()
			? "There is no meteorological input data in the time frame you specified"
			: undefined;
	}

	validateForGapError(){
		if (!this._start || !this._stop || this._disabledDates.length === 0) {
			return undefined;
		}

		const msStart = this._start.getTime();
		const msStop = this._stop.getTime();
		const hits = this._disabledDates.filter(d => msStart <= d.getTime() && d.getTime() <= msStop).length;
		const daysSpan = ((msStop - msStart) / 86400000) + 1;

		return hits === daysSpan;
	}

	validateForGapWarning(){
		if (!this._start || !this._stop || this._disabledDates.length === 0) {
			return undefined;
		}

		const msStart = this._start.getTime();
		const msStop = this._stop.getTime();
		//const hits = this._disabledDates.filter(d => msStart <= d.getTime() && d.getTime() <= msStop).length;

		return this._disabledDates.filter(d => msStart <= d.getTime() && d.getTime() <= msStop).length > 0
			? "Your selected dates stretches over a gap in the meteorological input data: "
				+ this._disabledMonths.map(d => formatDate(d)).join(', ')
			: undefined;
	}

	get startError(){
		return this._startErrorMsg;
	}

	get stopError(){
		return this._stopErrorMsg;
	}

	get gapWarning(){
		return this._gapWarning;
	}
}

function formatDate(d){
	const date = new Date(`${d[0]}-${d[1]}-01`);
	return date.toLocaleString('en-us', { month: 'long' }) + ' ' + d[0];
}
