'use strict';

var gulp = require('gulp');
var uglify = require('gulp-uglify');
var browserify = require('browserify');
var bcss = require('browserify-css');
var buffer = require('vinyl-buffer');
var del = require('del');
var source = require('vinyl-source-stream');
var babelify = require('babelify');

var SCALA_VERSION = 'scala-3.1.0';
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

		return minified.pipe(gulp.dest(paths.target));
	}

	return {
		build: gulp.series(clean, compileJs),
		publish: gulp.series(applyProd, clean, compileJs)
	}
}

var viewer = tasks('viewer');
var worker = tasks('worker');
var publishViewer = viewer.publish;
var publishWorker = worker.publish;
var publish = gulp.parallel(publishViewer, publishWorker);

exports.buildViewer = viewer.build;
exports.buildWorker = worker.build;
exports.publish = publish;
exports.default = publish;
