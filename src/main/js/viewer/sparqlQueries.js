
export function icosAtmoReleaseQuery(spec){
	const query = `prefix cpmeta: <http://meta.icos-cp.eu/ontologies/cpmeta/>
	prefix prov: <http://www.w3.org/ns/prov#>
	select ?stationId ?dobj ?samplingHeight ?nRows ?ackStartTime ?acqEndTime
	where {
		?dobj cpmeta:hasObjectSpec <${spec}> ;
			cpmeta:hasNumberOfRows ?nRows .
		?dobj cpmeta:wasAcquiredBy/prov:wasAssociatedWith ?station .
		?dobj cpmeta:hasSizeInBytes ?size .
		?dobj cpmeta:wasAcquiredBy [
			prov:startedAtTime ?ackStartTime ;
			prov:endedAtTime ?acqEndTime ;
			cpmeta:hasSamplingHeight ?samplingHeight
		]
		FILTER NOT EXISTS {[] cpmeta:isNextVersionOf ?dobj}
		?station cpmeta:hasStationId ?stationId
	}`;
	return {"text": query};
}
