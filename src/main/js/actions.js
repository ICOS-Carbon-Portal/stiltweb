import {fetchStiltResult} from './backend';

export const FETCHED_RESULT = 'FETCHED_RESULT';
export const ERROR = 'ERROR';


function failWithError(error){
	console.log(error);
	return {
		type: ERROR,
		error
	};
}

export const fetchData = columns => dispatch => {
	fetchStiltResult(columns).then(
		result => dispatch({
			type: FETCHED_RESULT,
			result
		}),
		err => dispatch(failWithError(err))
	);
}

