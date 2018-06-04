
export function parseUrlQuery(){
	return window.location.search.substring(1).split("&").reduce((acc, next) => {
		try{
			const [key, value] = next.split("=");
			return Object.assign(acc, {[key]: value});
		} catch(err){
			return acc;
		}
	}, {});
}
