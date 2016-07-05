
$(function(){
	$("#startStiltButton").on('click', startStilt);
	$("#checkBoxButton").on('click', drawCheckboxGraph);
});

function startStilt() {
	const site = JSON.stringify({
		site: $('#site option:selected').val()
	});

	$.ajax({
		type: "POST",
		url: "/startStilt",
		data: site,
		contentType: 'application/json'
	}).done(function () {
		setHeaderTexts();
		$('#visualizationDIV').removeAttr('hidden');
		visualize();
	})
}

function setHeaderTexts() {
	$('#graphsHeader h3').text('Data from ' + $('#site option:selected').val());
	$('#checkboxDiv h3').text('Choose concentrations for ' + $('#site option:selected').val());
}

function visualize() {
	var units = ["[ppm]"];
	var components = {
		data: ["co2.total"]
	};
	const g1 = drawGraph("STILTCO2MoleFractionGraph", JSON.stringify(components), units);

	units = ["[ppm]"];
	components = {
		data: ["co2.bio", "gee.all", "resp.all", "co2.fuel", "co2.cat.fuel.oil", "co2.cat.fuel.coal", "co2.cat.fuel.gas", "co2.cat.fuel.bio"]
	};
	const g2 = drawGraph("bioFuleCO2Graph", JSON.stringify(components), units);

	units = ["[m]", "[Bq/m2]"];
	components = {
		data: ["zi", "rn"]
	};
	const g3 = drawGraph("BoundHeightandRadonGraph", JSON.stringify(components), units);
}

function drawGraph(div, components, units) {
	const request = $.ajax({
		type: "POST",
		url: "/getData",
		data: components,
		dataType: 'json',
		contentType: 'application/json'
	});

	request.done(function (data) {
		drawDyghraphs(div, data.data, data.dates, data.labels, units);
	}).fail(function (ett, tva, tre) {
		console.log({ett, tva, tre});
	});
}

function drawDyghraphs(div, data, dates, labels, units) {
	labels.splice(0, 0, "date");

	if (units.length == 1) {
		const y1Label = units[0];

		return new Dygraph(document.getElementById(div), customZip(dates, data), {labels: labels, ylabel: y1Label, legend: "always"});
	} else {
		const yLabel = "zi - " + units[0];
		const y2Label = "rn - " + units[1];

		return new Dygraph(document.getElementById(div), customZip(dates, data), {
			labels: labels,
			series: {"rn": {axis: 'y2'}},
			ylabel: yLabel,
			y2label: y2Label,
			axisLabelWidth: 65,
			legend: "always"
		});
	}

}

function customZip(dateTimes, dataPoints) {
	const retArray = [];

	for (var i = 0; i < dateTimes.length; i++) {
		const tmpArray = [];
		tmpArray.push(new Date(dateTimes[i][0].replace("#", " ")));
		for (var j = 0; j < dataPoints[i].length; j++) {
			tmpArray.push(dataPoints[i][j]);
		}
		retArray.push(tmpArray);
	}
	return retArray;
}

function drawCheckboxGraph() {
	const cbxList = $('.footprint');
	const checkedBoxes = [];
	for (var i = 0; i < cbxList.length; i++) {
		if (cbxList[i].checked) {
			checkedBoxes.push(cbxList[i].value);
		}
	}

	const columns = {
		data: checkedBoxes
	};

	getData(JSON.stringify(columns));
}

function getData(footprints) {
	const request = $.ajax({
		type: "POST",
		url: "/getData",
		data: footprints,
		dataType: 'json',
		contentType: 'application/json'
	});

	request.done(function (data) {
		dygraph(data.data, data.dates, data.labels);
	}).fail(function (ett, tva, tre) {
		console.log({ett, tva, tre});
	});

}

function dygraph(data, dates, labels) {
	labels.splice(0, 0, "date");

	const graph = new Dygraph(document.getElementById("freeGraphdiv"), customZip(dates, data), {labels: labels, ylabel: "[ppm]", legend: "always"});
}

