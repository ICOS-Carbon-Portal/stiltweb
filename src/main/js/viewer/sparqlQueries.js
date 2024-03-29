
export function observationDataQuery(specs){
	const query = `prefix cpmeta: <http://meta.icos-cp.eu/ontologies/cpmeta/>
	prefix prov: <http://www.w3.org/ns/prov#>
	select ?isIcos ?stationId ?spec ?dobj ?samplingHeight ?nRows ?acqStartTime ?acqEndTime
	where {
		VALUES ?spec {<${specs.join("> <")}>}
		?dobj cpmeta:hasObjectSpec ?spec ;
			cpmeta:hasNumberOfRows ?nRows .
		?dobj cpmeta:wasAcquiredBy/prov:wasAssociatedWith ?station .
		?dobj cpmeta:hasSizeInBytes ?size .
		?dobj cpmeta:wasAcquiredBy [
			prov:startedAtTime ?acqStartTime ;
			prov:endedAtTime ?acqEndTime ;
			cpmeta:hasSamplingHeight ?samplingHeight
		]
		FILTER NOT EXISTS {[] cpmeta:isNextVersionOf ?dobj}
		?station cpmeta:hasStationId ?stationId .
		bind(exists{?station cpmeta:hasStationClass ?stClass} as ?isIcos)
	}`;
	return {"text": query};
}
