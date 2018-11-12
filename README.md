# stiltweb: Web facade for STILT modelling tool

## Useful URLs and curl command examples

To see the table with current STILT ids, names and alternative ids, visit https://stilt.icos-cp.eu/viewer/stationinfo

To submit a job (local test during development):

`curl -H "Content-Type: application/json" -X POST -d '{"siteId": "HTM", "lat":56.10, "lon": 13.42, "alt": 150, "start": "2012-06-18", "stop": "2012-06-18"}' http://127.0.0.1:9010/worker/enqueuejob`
