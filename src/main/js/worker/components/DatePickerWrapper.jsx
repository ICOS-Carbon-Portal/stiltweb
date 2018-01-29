import React, { Component } from 'react';
import DatesValidation from '../models/DatesValidation';
import {DatePickerInput} from 'rc-datepicker';

// Always mount DatePickerInput with undefined defaultValue -> to set start month for calendar but no start value


export default class DatePickerWrapper extends Component{
	constructor(props){
		super(props);

		this.state = {
			defaultValue: undefined,
			hasValError: false
		};

		this.input = undefined;
		this.onKeyUpHandler = this.onKeyUp.bind(this);
	}

	componentWillMount(){
		const props = this.props;

		if (props.name !== 'start' && props.name !== 'stop'){
			const msg = 'Unknown type of DatePickerInput';

			if (props.toastError){
				props.toastError(msg);
			} else {
				throw new Error(msg);
			}
		}
	}

	componentWillReceiveProps(nextProps){
		if (nextProps.minDate && nextProps.maxDate){
			this.setState({defaultValue: getDefaultValue(nextProps)});
		}
	}

	onKeyUp(evt){
		if (evt.target.value === '') this.onDateSet();
	}

	onDateSet(dateObj, dateString){
		if (dateString === 'Invalid date') {
			this.setState({hasValError: true});
			return;
		} else if (this.state.hasValError) {
			this.setState({hasValError: false});
		}

		const {name, siblingValue, minDate, maxDate, disabledDates, disabledMonths,
			updateDates, toastWarning, toastError} = this.props;

		const dates = name === 'start'
			? new DatesValidation(dateString, siblingValue, minDate, maxDate, disabledDates, disabledMonths)
			: new DatesValidation(siblingValue, dateString, minDate, maxDate, disabledDates, disabledMonths);

		if (name === 'start' && dates.startError && toastError) toastError(dates.startError);
		if (name === 'stop' && dates.stopError && toastError) toastError(dates.stopError);
		if (dates.gapWarning && toastWarning) toastWarning(dates.gapWarning);

		updateDates(dates);
	}

	render(){
		const {defaultValue, hasValError} = this.state;
		const {name, minDate, maxDate, style, value, hasLogicError} = this.props;

		return (
			<DatePickerInput
				ref={dpi => this.datePickerInput = dpi}
				disabled={!name}
				minDate={minDate}
				maxDate={maxDate}
				defaultValue={defaultValue}
				style={style}
				showOnInputClick={false}
				value={value}
				className={hasLogicError || hasValError ? 'cp-dpi-error' : ''}
				onChange={this.onDateSet.bind(this)}
				onClear={this.onDateSet.bind(this)}
				displayFormat="YYYY-MM-DD"
				returnFormat="YYYY-MM-DD"
			/>
		);
	}

	componentDidMount(){
		// Add handler for emptying date picker input
		this.input = this.datePickerInput.getDatePickerInput().firstChild.firstChild;
		this.input.addEventListener('keyup', this.onKeyUpHandler);

		if (this.props.minDate && this.props.maxDate){
			this.setState({defaultValue: getDefaultValue(this.props)});
		}
	}

	componentWillUnmount(){
		this.input.removeEventListener("keyup", this.onKeyUpHandler);
	}
}

const getDefaultValue = ({name, value, siblingValue, minDate, maxDate}) => {
	return name === 'start'
		? value ? undefined : (siblingValue || minDate)
		: value ? undefined : (siblingValue || maxDate);
};
