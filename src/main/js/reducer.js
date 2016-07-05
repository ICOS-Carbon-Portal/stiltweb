import { FETCHED_RESULT, ERROR} from './actions';

export default function(state, action){

	switch(action.type){

		case ERROR:
			return Object.assign({}, state, {status: ERROR, error: action.error});

		case FETCHED_RESULT:
			return Object.assign({}, state, {
				status: FETCHED_RESULT,
				result: action.result
			});

		default:
			return state;
	}
}
