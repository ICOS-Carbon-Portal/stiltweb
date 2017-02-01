import {getInitialData, getStationData} from './backend';

export const FETCHED_INITDATA = 'FETCHED_INITDATA';
export const ERROR = 'ERROR';


function failWithError(error){
	console.log(error);
	return {
		type: ERROR,
		error
	};
}

export const fetchInitData = dispatch => {
	getInitialData().then(
		initData => dispatch(Object.assign({type: FETCHED_INITDATA}, initData)),
		err => dispatch(failWithError(err))
	);
}