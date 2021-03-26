var gulp = require('gulp'),
    jshint = require('gulp-jshint'),
    concat = require('gulp-concat'),
    uglify = require('gulp-uglify'),
    less = require('gulp-less'),
    minifyCss = require('gulp-minify-css'),
    stripCssComments = require('gulp-strip-css-comments');

var bases = {
    application: 'app/',
    production: 'build/web/'
};

var paths = {
    scripts: ['scripts/**/*.js', '!scripts/libs/**/*.js'],
    less: ['less/*.less'],
    libs: [
        'scripts/libs/angular.min.js',              // AngularJS
        'scripts/libs/angular-animate.min.js',      // ngAnimate
        'scripts/libs/jquery-2.2.0.min.js',         // jQuery
        'scripts/libs/angular-ui-router.min.js',    // ngRoute
        'scripts/libs/loading-bar.min.js',          // Loading animation
        'scripts/libs/angular-toastr.min.js',       // angular messages
        'scripts/libs/bootstrap.min.js'             // boostrap
        ],
    html: ['index.html'],
	fonts: ['fonts/**/'],
    images: ['images/**/'],
    extras: ['.htaccess'],
    templates: ['templates/**/*.html'],
    json: ['json-data/**/']
};

// minify, concatenate libs
// minify and concatenate scripts in default script directory
gulp.task('scripts', gulp.series(function(done) {
    gulp.src(paths.libs, {cwd: bases.application})
        .pipe(uglify())
        .pipe(concat('libs.min.js'))
        .pipe(gulp.dest(bases.production + 'scripts/'));

    gulp.src(paths.scripts, {cwd: bases.application})
        .pipe(jshint())
        .pipe(jshint.reporter('default'))
        //.pipe(uglify())
        .pipe(concat('app.min.js'))
        .pipe(gulp.dest(bases.production + 'scripts/'));
    done();
}));

// convert less to css remove comments, minify and concatenate css styles, copy css file to production
gulp.task('styles', gulp.series(function() {
    return gulp.src(paths.less, {cwd: bases.application})
        .pipe(less())
        .pipe(stripCssComments({all:true}))
        .pipe(concat('styles.css'))
        .pipe(minifyCss())
        .pipe(gulp.dest(bases.production + 'css/'));
}));

// copy all other files to production directly
gulp.task('copy', gulp.series(function(done) {
    // Copy html
    gulp.src(paths.html, {cwd: bases.application, allowEmpty: true})
        .pipe(gulp.dest(bases.production));

    // Copy extra html files
    gulp.src(paths.extras, {cwd: bases.application, allowEmpty: true })
        .pipe(gulp.dest(bases.production));

    gulp.src(paths.images, {cwd: bases.application, allowEmpty: true })
        .pipe(gulp.dest(bases.production + 'images/'));

    // Copy templates
    gulp.src(paths.templates, {cwd: bases.application, allowEmpty: true })
        .pipe(gulp.dest(bases.production + 'templates/'));

    // Copy fonts
	gulp.src(paths.fonts, {cwd: bases.application, allowEmpty: true })
		.pipe(gulp.dest(bases.production + 'fonts/'));
	done();
}));

gulp.task('watchForChanges', gulp.series(function() {
    gulp.watch(bases.application + paths.less, gulp.series('styles'));
    gulp.watch(bases.application + 'scripts/**/*.js', gulp.series('scripts'));
    gulp.watch(bases.application + '**/*.html', gulp.series('copy'));
}));

gulp.task('default', gulp.series('scripts','styles', 'copy', 'watchForChanges', function(done) {
    done();
}));
