[![Build Status](https://travis-ci.org/carueda/gpdviz.svg?branch=master)](https://travis-ci.org/carueda/gpdviz)

# gpdviz

Web-based visualization of geo-located point data streams in real-time.

Implementation based on:

- [Akka-HTTP](http://doc.akka.io/docs/akka-http/current/scala/http/)
- [ScalaJS](https://www.scala-js.org/)
- [Leaflet](http://leafletjs.com/)
- [Highcharts](http://www.highcharts.com/)


## build and run

    $ npm install jsdom source-map-support
	$ source setenv.sh  # or use whatever mechanism to define the required env vars, see application.conf
	$ sbt
	> package   # to copy js resources to jvm's classpath needed for the webapp
	> ~gpdvizJVM/runMain gpdviz.server.GpdvizServer

Open http://localhost:5050/ss1/

On another terminal (this requires [httpie](https://httpie.org/)):

	$ data/ss1.demo.sh


![](https://github.com/carueda/gpdviz/blob/master/static/gpdviz2.gif)


## dist and run

	$ sbt assembly
	$ source setenv.sh  
	$ bin/run.sh

## model

Model still very ad hoc.


## history

This is a reimplementation of the [original idea](https://github.com/carueda/gpdviz0).
