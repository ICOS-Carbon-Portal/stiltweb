import { FETCHED_RESULT, FINISHED_COMPUTATION, ERROR} from './actions';

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

		case FINISHED_COMPUTATION:
			return updatedState({
				status: FINISHED_COMPUTATION,
				computationStatus: action.computationStatus
			});

		default:
			return state;
	}
}
