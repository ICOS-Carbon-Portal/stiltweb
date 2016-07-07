import * as backend from './backend';

export const FETCHED_RESULT = 'FETCHED_RESULT';
export const FINISHED_COMPUTATION = 'FINISHED_COMPUTATION';
export const ERROR = 'ERROR';


function failWithError(error){
	console.log(error);
	return {
		type: ERROR,
		error
	};
}

export const fetchData = columns => dispatch => {
	backend.fetchStiltResult(columns).then(
		result => dispatch({
			type: FETCHED_RESULT,
			result
		}),
		err => dispatch(failWithError(err))
	);
}

export const startStilt = site => dispatch => {
	backend.startStiltComputation(site).then(
		result => dispatch({
			type: FINISHED_COMPUTATION,
			computationStatus: result
		}),
		err => dispatch(failWithError(err))
	);
}
