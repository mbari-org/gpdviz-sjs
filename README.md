[![Build Status](https://travis-ci.org/gpdviz/gpdviz.svg?branch=master)](https://travis-ci.org/gpdviz/gpdviz)

# Gpdviz

Gpdviz is a tool for web-based visualization of geo-located point data streams in real-time.

Gpdviz is motivated by the value of having a web-based, lightweight tool for easy
visualization of sensor data information especially in non-data intensive scenarios.
It is intended to complement the available tool set for the user.

Gpdviz uses a simple data model and a REST API for data providers to register sensor systems,
data streams, and observations.
This API is specified using [OpenAPI 2.0](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md).
The visualization is updated in real-time using WebSockets.

Assuming Gpdviz is deployed at `http://example.net/gpdviz` and a particular sensor system with 
ID `mysysid`:
- Users would visualize the system at `http://example.net/gpdviz/mysysid/`
- The REST API endpoint would be located at `http://example.net/gpdviz/api`, 
  and a page with the complete API documentation, with ability to directly exercise it 
  (powered by Swagger UI), would be located at `http://example.net/gpdviz/api-docs`.

**Data model**

Gpdviz's data model is intended to be simple.
There are three main entities in the data model:
_sensor systems_, _data streams_, and _observations_.

- A sensor system has associated metadata (name, description, etc.) 
  and consists of a set of data streams.

- A data stream has associated metadata (name, description, map/chart styles, etc.) 
  and consists of a set of variable definitions and associated timestamped data observations.

- A variable definition consists of a name, units, and chart style. 

- An observation is timestamped and can capture scalar data (eg., temperature values),
  as well as features and geometries (in geo-json format).
 
**Operations for data providers**

Data providers interact with a Gpdviz endpoint to perform the following operations:

- Register/unregister sensor systems
- Add/remove data streams
- Register data stream observations

The Gpdviz server maintains a registry with the provided information for each sensor system
including a window of most recent reported observations for each stream in the system.
Users will be able to see the latest reported state whenever they open the particular
URL corresponding to the desired sensor system, and in real time see any subsequent
updates reported by the provider.


## Running

Download the latest executable JAR `gpdviz-x.y.z.jar` from https://github.com/gpdviz/gpdviz/releases/.

To execute Gpdviz you will need a [Java runtime environment](https://www.java.com/),
access to a PostgreSQL server, and ability to create a database and a user on that
server for Gpdviz purposes.

- Gpdviz expects a number of parameters for its regular execution. These parameters are to be
  indicated in a local `conf/gpdviz.conf` file. A template of such file, with
  a description of the various parameters, can be generated as follows:

        $ java -jar gpdviz-x.y.z.jar generate-conf

- Edit `conf/gpdviz.conf` as needed.

- Create the database and user indicated in `gpdviz.conf`, with the user being granted
  all privileges on the database.

- Execute the Gpdviz server: 

        $ java -jar gpdviz-x.y.z.jar run-server


This [httpie](https://httpie.org/)-based 
[bash script](https://github.com/gpdviz/gpdviz/blob/master/data/ss1.demo.sh)
can be used as a demo of a data provider. Define the `GPDVIZ_SERVER` environment
variable prior to running this script.

For a similar demo in Python, see https://github.com/gpdviz/gpdviz_python_client_example,
which uses the automatically generated client module https://github.com/gpdviz/gpdviz_python_client
based on the Swagger REST API specification.

## Development

Gpdviz implementation is based on:

- [Akka-HTTP](http://doc.akka.io/docs/akka-http/current/scala/http/)
- [Slick](https://github.com/slick/slick)
- [PostgreSQL](https://github.com/postgres/postgres)
- [ScalaJS](https://www.scala-js.org/)
- [Leaflet](http://leafletjs.com/)
- [Highcharts](http://www.highcharts.com/)


### Build and run

    $ npm install jsdom source-map-support
	$ sbt
	> package   # to copy js resources to jvm's classpath needed for the webapp
	> gpdvizJVM/runMain gpdviz.Gpdviz generate-conf
	> gpdvizJVM/runMain gpdviz.Gpdviz create-tables
	> gpdvizJVM/runMain gpdviz.Gpdviz add-some-data
	> gpdvizJVM/runMain gpdviz.Gpdviz run-server

For the demo with sensor system ID "ss1":
 
- Open http://localhost:5050/ss1/ in your browser.
- On another terminal, either run the bash script:

	    $ data/ss1.demo.sh
	
    or the similar client in python as explained at 
    https://github.com/gpdviz/gpdviz_python_client_example


![](https://github.com/gpdviz/gpdviz/blob/master/static/gpdviz2.gif)


### Dist

To generate the executable JAR `jvm/target/scala-2.12/gpdviz-x.y.z.jar`:

	$ sbt gpdvizJVM/assembly

For the dockerized version, see [docker/readme.md](docker/readme.md).

### Model

Data model details are still wip.

### API

OpenAPI spec generated by [swagger-akka-http](https://github.com/swagger-akka-http/swagger-akka-http).
 
**Swagger UI**

A Swagger UI instance is embedded as follows:

- copy [Swagger UI dist/*](https://github.com/swagger-api/swagger-ui/tree/master/dist) 
  contents to `jvm/src/main/resources/swaggerui/`;
- adjust the `url` entry in `swaggerui/index.html` to refer to the generated spec:
  `url: window.location + "/swagger.json"`
- rebuild

Under local development:
- generated spec at http://localhost:5050/api-docs/swagger.json
- Swagger UI at http://localhost:5050/api-docs



## history

[Original idea](https://github.com/carueda/gpdviz0).
