export default {
	sparqlEndpoint: 'https://meta.icos-cp.eu/sparql',
	cpmetaOntoUri: 'http://meta.icos-cp.eu/ontologies/cpmeta/',
	cpmetaResUri: 'http://meta.icos-cp.eu/resources/cpmeta/',
	primaryComponents(selectedYear){
		const obsColumns = !selectedYear || selectedYear.dataObject ? wdcggColumns.slice(1) : [];
		return obsColumns.concat(stiltResultColumns.slice(1,3));
	},
	secondaryComponents(){
		return stiltResultColumns.slice(3);
	},
	stations: [ //STILT id, STILT lat, STILT lon, ICOS short name, WDCGG name
		['BAL', 55.35, 17.22, '',       'Baltic Sea'],
		['BGU',     0,     0, '',       'Begur'],
		['BI5', 53.23, 23.03, '',       ''],
		['BSC', 44.17, 28.68, '',       ''],
		['CB1',     0,     0, 'NL-Cab', 'Cabauw 20m'],
		['CB4',     0,     0, 'NL-Cab', 'Cabauw 200m'],
		['EGH', 51.43, -0.56, '',       ''],
		['GIF', 48.71,  2.15, '',       ''],
		['HEI', 49.42,  8.67, '',       ''],
		['HPB',     0,     0, 'HPB',    'Hohenpeissenberg'],
		['HU1',     0,     0, '',       'Hegyhatsal'],
		['IPR', 45.81,  8.63, '',       ''],
		['JFJ',     0,     0, 'JFJ',    'Jungfraujoch'],
		['KAS', 49.25, 19.98, '',       ''],
		['LMP',     0,     0, 'Lmp',    'Lampedusa'],
		['LPO', 48.80, -3.58, '',       ''],
		['LU1',     0,     0, 'NL-Lut', ''],
		['MHD',     0,     0, '',       'Mace Head'],
		['OX3',     0,     0, 'OXK',    'Ochsenkopf'],
		['PAL',     0,     0, 'ATM-PAL','Pallas-Sammaltunturi'],
		['PDM',     0,     0, '',       'Pic du Midi'],
		['PUY',     0,     0, 'PUY',    'Puy de Dome'],
		['SIL',     0,     0, '',       'Schauinsland'],
		['TR2',     0,     0, 'TRN',    ''],
		['TR4',     0,     0, 'TRN',    ''],
		['TT1', 56.55, -2.98, '',       ''],
		['TT2', 56.55, -2.98, '',       ''],
		['WEY', 52.95,  1.12, 'WAO',    ''],
		['FIK', 35.34, 25.67, '',       'Finokalia'],
		['FZJ', 50.91,  6.41, 'JUE',    ''],
		['GAR', 53.07, 11.44, 'GAT',    ''],
		['HYL', 56.10, 13.42, 'SE-Htm', ''],
		['KIT050',49.09,8.43, 'KIT',    ''],
		['KIT200',49.09,8.43, 'KIT',    ''],
		['LIN', 52.21, 14.12, 'LIN',    ''],
		['OP3', 48.55,  5.50, 'OPE',    ''],
		['WES', 54.92,  8.30, '',       'Westerland'],
		['ARR', 69.28, 16.01, '',       ''],
		['BIR', 58.38,  8.25, 'BIR',    ''],
		['BIS', 44.38, -1.23, '',       ''],
		['CSP', 52.06, -6.33, '',       ''],
		['DEB', 40.73,  0.79, '',       ''],
		['F3P', 54.85,  4.73, '',       ''],
		['GOE', 51.48,  3.78, '',       ''],
		['HEL', 54.18,   7.9, 'HEL',    ''],
		['HEN', 52.34,  6.75, '',       ''],
		['HFD', 50.98,  0.23, '',       ''],
		['HOV', 56.44,  8.15, '',       ''],
		['HYY', 61.85, 24.29, 'ATM-HYY',''],
		['IZN', 37.28, -4.38, '',       ''],
		['JEM', 52.35, 15.28, '',       ''],
		['KKY', 50.19, 19.12, '',       ''],
		['KRE', 49.57, 15.07, 'KRE',    ''],
		['MAH', 55.37, -7.34, '',       ''],
		['NOR', 60.09, 17.48, 'SE-Nor', ''],
		['OHP', 43.93,  5.71, '',       ''],
		['OST', 57.05,  8.88, '',       ''],
		['PEE', 51.37,  5.98, '',       ''],
		['PRW', 53.17, 16.26, '',       ''],
		['PTR', 45.94,  7.71, 'P-Rosa', 'Plateau Rosa'],
		['PUI', 62.91, 27.66, 'ATM-PUI',''],
		['RHL', 52.00, -2.54, '',       ''],
		['RIS', 55.65, 12.09, '',       ''],
		['RTR', 48.41, -3.91, '',       ''],
		['SKK', 55.51, -2.84, '',       ''],
		['SVA', 64.26, 19.77, 'SE-Svb', ''],
		['TAC', 52.52,  1.14, '',       'Tacolneston Tall Tower'],
		['TSE', 52.37,  8.03, '',       ''],
		['UTO', 59.78, 21.38, 'ATM-Utö',''],
		['VOI', 59.95,  30.7, '',       '']
	],
	defaultDelay: 100 //ms
}

