export default class AvailableMonths{
	constructor(availableMonths){
		this._availableMonths = availableMonths;
		this._disabledDates = this.getDisabledDates();
		this._disabledMonths = this.getDisabledMonths();
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

	get disabledMonths(){
		return this._disabledMonths;
	}

	firstPossibleDate(date){
		return new Date(date.setDate(date.getDate() + 10));
	}

	getNextMonth(date){
		date.setUTCMonth(date.getUTCMonth() + 1);
		return date;
	}

	getDisabledMonths(){
		if (this._availableMonths) {
			const yearMonths = this._availableMonths.map(d => d.split('-').map(parts => parseInt(parts)));
			const gaps = [];

			yearMonths.forEach((yearMonth, idx) => {
				if (idx === 0) return;

				const currYear = yearMonth[0];
				const currMonth = yearMonth[1];
				const lastYear = yearMonths[idx -1][0];
				const lastMonth = yearMonths[idx -1][1];

				if (currYear === lastYear && currMonth !== lastMonth + 1){
					Array.from(Array(currMonth - lastMonth - 1))
						.forEach((v, i) => gaps.push([currYear, lastMonth + i + 1]));

				} else if (currYear !== lastYear && (lastMonth !== 12 || currMonth !== 1)){
					Array.from(Array(12 - lastMonth))
						.forEach((v, i) => gaps.push([lastYear, lastMonth + i + 1]));

					Array.from(Array(currMonth - 1))
						.forEach((v, i) => gaps.push([currYear, i + 1]));
				}
			});
			return gaps;
		}
	}

	getDisabledDates(){
		function calcDisabledDates(self, start, stop){
			let ms = start.getTime();
			const msStop = self.firstPossibleDate(stop);
			const dates = [];

			while(ms < msStop){
				dates.push(new Date(ms));
				ms += 24 * 3600000;
			}

			return dates;
		}

		if (this._availableMonths) {
			let disabledDates = [];

			for (let i=1; i<this._availableMonths.length; i++){
				// const prev = new Date(this._availableMonths[i - 1]);
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