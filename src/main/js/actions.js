import * as backend from './backend';

export const FETCHED_RESULT = 'FETCHED_RESULT';
export const SITE_CHOSEN = 'SITE_CHOSEN';
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

export const chooseSite = site => {
	return {
		type: SITE_CHOSEN,
		site
	};
}
