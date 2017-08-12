[![Build Status](https://travis-ci.org/carueda/gpdviz2.svg?branch=master)](https://travis-ci.org/carueda/gpdviz2)

# gpdviz2

Web-based visualization of geo-located point data streams in real-time.

This is a preliminary revisit of the [initial idea](https://github.com/carueda/gpdviz).

Implementation currently based on:

- [Akka-HTTP](http://doc.akka.io/docs/akka-http/current/scala/http/)
- [Angular 1.5](https://angularjs.org/)
- [Leaflet](http://leafletjs.com/)
- [Highcharts](http://www.highcharts.com/)
- [Pusher](https://pusher.com/)


## build and run

    $ npm install jsdom
	$ source setenv.sh  # or use whatever mechanism to define the required env vars, see application.conf
	$ sbt "gpdvizJVM/runMain gpdviz.server.GpdvizServer"

Open http://localhost:5050/ss1/

On another terminal (this requires [httpie](https://httpie.org/)):

	$ data/ss1.demo.sh


![](https://github.com/carueda/gpdviz2/blob/master/static/gpdviz2.gif)


## dist

	$ sbt assembly
	$ source setenv.sh  
	$ bin/run.sh

## model

- model very ad hoc still
- currently only associated with a stream
- could also be associated with concrete feature
- ...
