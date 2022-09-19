'use strict';

var uglify = require('gulp-uglify');
var browserify = require('browserify');
var bcss = require('browserify-css');
var buffer = require('vinyl-buffer');
var del = require('del');
var source = require('vinyl-source-stream');
var babelify = require('babelify');
const { watch, dest, series, parallel } = require('gulp');

var SCALA_VERSION = 'scala-3.2.0';
var PROD = 'production'

function applyProd() {
	process.env.NODE_ENV = PROD;
	return Promise.resolve();
};

function tasks(project){

	var projSrc = 'src/main/js/' + project;

	var paths = {
		main: projSrc + '/main.jsx',
		jsx: projSrc + '/**/*.jsx',
		js: projSrc + '/**/*.js',
		alljs: projSrc + '/**/*.js',
		commonjs: 'src/main/js/common/**/*.js*',
		commonjsx: 'src/main/js/common/**/*.jsx*',
		target: 'target/' + SCALA_VERSION + '/classes/www/',
		bundleFile: project + '.js'
	};

	function clean() {
		return del([paths.target + paths.bundleFile]);
	};

	function compileJs() {
		var browser = browserify({entries: [paths.main], debug: false})
			.transform(bcss, {global: true})
			.transform(babelify, {presets: ["env", "react"]})
			.bundle()
			.pipe(source(paths.bundleFile))
			.on('error', function(err){
				console.log(err);
				this.emit('end');
			});

		var minified = process.env.NODE_ENV === PROD
			? browser.pipe(buffer()).pipe(uglify())
			: browser;

		return minified.pipe(dest(paths.target));
	}

	return {
		build: function(){
			watch(
				[paths.js, paths.jsx, paths.commonjs, paths.commonjsx],
				{ignoreInitial: false},
				series(clean, compileJs)
			)
		},
		publish: series(applyProd, clean, compileJs)
	}
}

var viewer = tasks('viewer');
var worker = tasks('worker');
var publishViewer = viewer.publish;
var publishWorker = worker.publish;
var publish = parallel(publishViewer, publishWorker);

exports.buildViewer = viewer.build;
exports.buildWorker = worker.build;
exports.publish = publish;
