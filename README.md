# stiltweb: Web facade for STILT modelling tool

Deployed to [stilt.icos-cp.eu](https://stilt.icos-cp.eu/viewer/)

## Useful URLs and curl command examples (internal use)

To see the table with current STILT ids, names and alternative ids, visit https://stilt.icos-cp.eu/viewer/stationinfo

To submit a job:

`curl --cookie "cpauthToken=..." -H "Content-Type: application/json" -X POST -d '{"userId": "user@email.com", "siteId": "HTM", "lat":56.10, "lon": 13.42, "alt": 150, "start": "2012-06-18", "stop": "2012-06-18"}' http://127.0.0.1:9010/worker/enqueuejob`

To fetch STILT results:

`curl -X POST -H "Content-Type: application/json" --data '{"columns": ["isodate", "co2.stilt"], "fromDate": "2008-01-01", "toDate": "2008-01-02", "stationId": "BAL"}'  https://stilt.icos-cp.eu/viewer/stiltresult`

Column `isodate` is synthetic, will contain timestamp (seconds since Unix epoch in UTC).

Property `"columns"` is optional. If provided, the returned result is a JSON array of JSON arrays with corresponding values. If omitted, the result is a JSON array of JSON objects with all the available properties and values (useful to discover the available columns).

To get raw STILT results instead of "pre-packaged" summaries, post the same type of request to a similar URL that ends in `stiltrawresult` istead of `stiltresult`.
