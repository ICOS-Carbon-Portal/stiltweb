import 'whatwg-fetch';

function checkStatus(response) {
	if(response.status >= 200 && response.status < 300)
		return response;
		else throw new Error(response.statusText || "Ajax response status: " + response.status);
}

export function fetchStiltResult(columns){
	return fetch('getData', {
			method: 'post',
			headers: {
				'Content-Type': 'application/json'
			},
			body: JSON.stringify({data: columns})
		})
		.then(checkStatus)
		.then(response => response.json());
}

export function startStiltComputation(site){
	return fetch('startStilt', {
		method: 'post',
		headers: {
			'Content-Type': 'application/json'
		},
		body: JSON.stringify({site})
	})
	.then(checkStatus)
	.then(response => response.text());
}

