[![Build Status](https://travis-ci.org/carueda/gpdviz2.svg?branch=master)](https://travis-ci.org/carueda/gpdviz2)

# gpdviz2

Web-based visualization of geo-located point data streams in real-time.

This is a preliminary revisit of the [initial idea](https://github.com/carueda/gpdviz).

At the moment, implementation based on:

- [Spray](http://spray.io/)
- [Angular 1.5](https://angularjs.org/)
- [Leaflet](http://leafletjs.com/)
- [Highcharts](http://www.highcharts.com/)
- [Pusher](https://pusher.com/)


## local run

	$ source setenv.sh  # or use whatever mechanism to define the required env vars, see application.conf
	$ sbt run

Open http://localhost:5050/ss1/

On another terminal (this requires [httpie](https://httpie.org/)):

	$ data/ss1.demo.sh


![](https://github.com/carueda/gpdviz2/blob/master/static/gpdviz2.gif)


## build/install fat jar

still pretty ad hoc ...

	$ sbt assembly
	$ scp target/scala-2.11/gpdviz-0.0.2.jar server.example.net:gpdviz/
	$ tar zcf gpdviz_static.tgz static
	$ scp gpdviz_static.tgz  server.example.net:gpdviz/


## model

- "chart data" very ad hoc still
- just values, no names or any other md
- currently only associated with a stream
- could also be associated with concrete feature
- ...

