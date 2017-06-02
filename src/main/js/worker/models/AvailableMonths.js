export default class AvailableMonths{
	constructor(availableMonths){
		this._availableMonths = availableMonths;
		this._disabledDates = this.getDisabledDates();
	}

	get min(){
		if (this._availableMonths) {
			return this.firstPossibleDate(new Date(this._availableMonths[0]));
		} else {
			return null;
		}
	}

	get max(){
		if (this._availableMonths) {
			const lastDate = new Date(this._availableMonths.slice(-1)[0]);
			const nextMonth = this.getNextMonth(lastDate);
			return new Date(nextMonth.setDate(nextMonth.getDate() - 1));
		} else {
			return null;
		}
	}

	get disabledDates(){
		return this._disabledDates;
	}

	firstPossibleDate(date){
		return new Date(date.setDate(date.getDate() + 10));
	}

	getNextMonth(date){
		date.setUTCMonth(date.getUTCMonth() + 1);
		return date;
	}

	getDisabledDates(){
		function calcDisabledDates(self, start, stop){
			var ms = start.getTime();
			const msStop = self.firstPossibleDate(stop);
			const dates = [];

			while(ms < msStop){
				dates.push(new Date(ms));
				ms += 24 * 3600000;
			}

			return dates;
		}

		if (this._availableMonths) {
			var disabledDates = [];

			for (var i=1; i<this._availableMonths.length; i++){
				const prev = new Date(this._availableMonths[i - 1]);
				const nextMonth = this.getNextMonth(new Date(this._availableMonths[i - 1]));
				const curr = new Date(this._availableMonths[i]);

				if (nextMonth.getTime() !== curr.getTime()) {
					disabledDates = disabledDates.concat(calcDisabledDates(this, nextMonth, curr));
				}
			}

			return disabledDates;
		} else {
			return [];
		}
	}
}