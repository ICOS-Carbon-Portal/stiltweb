'use strict';

var gulp = require('gulp');
var browserify = require('browserify');
var del = require('del');
var source = require('vinyl-source-stream');
var babelify = require('babelify');

var paths = {
	main: 'src/main/js/main.jsx',
	jsx: ['src/main/js/**/*.jsx'],
	js: ['src/main/js/**/*.js'],
	target: 'src/main/resources/www/',
	bundleFile: 'stilt.js'
};

gulp.task('clean', function(done) {
	del([paths.target + paths.bundleFile], done);
});

gulp.task('js', ['clean'], function() {

	return browserify({
			entries: [paths.main],
			debug: false
		})
		.transform(babelify, {presets: ["es2015", "react"]})
		.bundle()
		.on('error', function(err){
			console.log(err);
			this.emit('end');
		})
		.pipe(source(paths.bundleFile))
		.pipe(gulp.dest(paths.target));

});

gulp.task('watch', function() {
	var sources = paths.js.concat(paths.jsx);
	gulp.watch(sources, ['js']);
});

gulp.task('stiltweb', ['watch', 'js']);

