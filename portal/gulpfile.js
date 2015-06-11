var gulp = require('gulp');
var tar = require('gulp-tar');
var gzip = require('gulp-gzip');
var bower = require('gulp-bower');
var concat = require('gulp-concat');
var uglify = require('gulp-uglify');
var ngAnnotate = require('gulp-ng-annotate');

var base = 'build';
var bower_base = 'bower_components';

gulp.task('bower', function() {
  return bower()
    .pipe(gulp.dest(bower_base))
});

gulp.task('dependencies', function () {
  gulp.src([
      bower_base + '/google/index.js',
      bower_base + '/facebook/index.js',
      bower_base + '/angular/angular.js',
      bower_base + '/aws-sdk-js/dist/aws-sdk.js',
      bower_base + '/angular-bootstrap/ui-bootstrap-tpls.js',
      bower_base + '/login-with-amazon/index.js'
    ], {base: bower_base})
      .pipe(gulp.dest(base + '/js/'))
  gulp.src([
      bower_base + '/aws-sdk-js-dynamodb-document/lib/*.js'
    ], {base: 'bower_components'})
        .pipe(concat('aws-sdk-js-dynamodb-doc.js'))
        .pipe(ngAnnotate())
        .pipe(uglify())
        .pipe(gulp.dest(base + '/js/'))
  gulp.src([
    bower_base + '/bootstrap/dist/css/*min*',
    bower_base + '/bootstrap-dashboard/index.css',
  ])
  .pipe(gulp.dest(base + '/css'));
  gulp.src([
    bower_base + '/bootstrap/dist/fonts/**/*',
  ])
    .pipe(gulp.dest(base + '/fonts'));      
});

gulp.task('application', function () {
  gulp.src(['src/**/*'], {base: 'src'} )
    .pipe(gulp.dest(base))
});

gulp.task('watch', function() {
  gulp.watch([
  'src/**/*'
  ], ['application']);
});

gulp.task('tarball', [ 'bower', 'dependencies', 'application' ], function() {
  gulp.src(base + '/**/*')
    .pipe(tar('sample-appstream-developer-entitlement-portal.tar'))
    .pipe(gzip())
    .pipe(gulp.dest('.'));
});
