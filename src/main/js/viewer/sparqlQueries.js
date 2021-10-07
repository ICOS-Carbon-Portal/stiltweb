
export function wdcggTimeSeriesQuery(wdcggStationNames){

	//TODO Remove the extra trailing space at the end of wdcggName when it is removed from the metadata
	const wdcggIdValues = wdcggStationNames.map(wdcggName => `"${wdcggName} "`).join(' ');

	return {"text": `prefix cpmeta: <http://meta.icos-cp.eu/ontologies/cpmeta/>
prefix prov: <http://www.w3.org/ns/prov#>
select distinct ?wdcggId ?dobj ?nRows ?ackStartTime ?ackEndTime
FROM <http://meta.icos-cp.eu/resources/wdcgg/>
where {
	VALUES ?wdcggId { ${wdcggIdValues} }
	?acquisition prov:wasAssociatedWith [cpmeta:hasName ?wdcggId] .
	?dobj cpmeta:wasAcquiredBy ?acquisition ;
		<http://meta.icos-cp.eu/resources/wdcgg/PARAMETER> "CO2"^^xsd:string ;
		<http://meta.icos-cp.eu/resources/wdcgg/TIME%20INTERVAL> "hourly"^^xsd:string ;
		cpmeta:hasNumberOfRows ?nRows .
	?acquisition prov:endedAtTime ?ackEndTime ;
		prov:startedAtTime ?ackStartTime .
}`};

}

