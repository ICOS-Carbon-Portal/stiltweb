import { FETCHED_RESULT, SITE_CHOSEN, ERROR} from './actions';

export default function(state, action){

	function updatedState(update){
		return Object.assign({}, state, update);
	}

	switch(action.type){

		case ERROR:
			return updatedState({status: ERROR, error: action.error});

		case FETCHED_RESULT:
			return updatedState({
				status: FETCHED_RESULT,
				result: action.result
			});

		case SITE_CHOSEN:
			return updatedState({
				site: action.site
			});

		default:
			return state;
	}
}
