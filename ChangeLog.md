# Gpdviz

2017-10-30

- remove PostgresDbDoobie and PostgresDbQuill
- route to add variable definitions
- adjust response for successful creation of entities
- overall register/unregister-to-add/delete renaming
- add drop-tables, especially to avoid error noise during local testing

2017-10-25

- complete retrieval of full sensor system
- add option to import existing models in json files

2017-10-23

- first slick-based version

2017-10-20

- due to doobie issues, trying slick
- due to quill issues, trying again with doobie
- some db-related refact
- nested embedded with intermediate Option still not handled by quill 

2017-10-19

- do not forget to also update the swagger response types!
- adjustments to API mainly in terms of responses (so they are all objects)
- tests re-enabled (except for one involving quill's update operation) 

2017-10-14

- focus on quill/postgres as the database system. 
  Some tests ignored for now.
  ss1.demo.sh working.
  Define config property `gpdviz.postgres` to exercise it.
  DB setup:

        psql -c 'create database gpdviz;' -U postgres
        psql -U postgres -d gpdviz < create_tables.sql

2017-09-14 0.3.2

- fix api-docs link
- update some doc

2017-09-13 0.3.1

- simplify execution via explicit `gpdviz.conf` file

2017-09-12 0.3.1

- add demo data entry generated from [gpdviz_python_client_example](https://github.com/gpdviz/gpdviz_python_client_example)
- repo moved to gpdviz organization
- swagger adjustments, in particular, use of `@(ApiModelProperty @field)(dataType = "object")`
  to annotate fields with type involving `JsObject`, `Feature`, or `Geometry`, which are not
  properly exposed; moreover, the `object` dataType gives good flexibility (for now).
- dispatch root request with list of registered systems

2017-09-01 0.3.0

- include link to api-docs
- capture externalUrl in configuration
- use reference.conf to expose version to app
- use swagger to generate openApi spec and api-docs ui
  - generated spec at `.../api-docs/swagger.json`, eg., http://localhost:5050/api-docs/swagger.json
  - Swagger UI at `.../api-docs/`, eg., http://localhost:5050/api-docs
    
2017-08-31

- some refact - separation of rest services
- expose tool version in web page
- preliminaries to enable support for mongo as db

2017-08-30

- make data interfaces non-blocking

2017-08-29

- apache config to support websocket per https://stackoverflow.com/a/29823699/830737
- various adjustments while testing on external servers and back in local mode.

2017-08-25

- version 0.2.0
- repo renamed to https://github.com/carueda/gpdviz.
  (old one to https://github.com/carueda/gpdviz0)
- publish websocket messages for the specific sysid
- simplify details panel
- restore window resize handling
- some investigation re "Websocket handler failed with Processor actor" when exiting the server 
  while some client currently connected. TODO check https://stackoverflow.com/a/41359874/830737 
- handle chart height and minWidth
- handle map center and zoom
- enable the new scalajs based dispatch
- websocket client: connect upon page load and keep connection alive.
  "Reconnect" button enabled if connection closed by server; this just tries to 
  re-establish connection. (Complete refresh needs page reload.)
- refresh initial state of sensor system
- factor out publish functionality 

2017-08-18

- first working websocket version.
  Undefine `gpdviz.pusher` in `application.conf` to exercise.
- enable click handler
- enable hoveredPoint behavior
- handle popup chart (besides static chart)
- pass mapStyle, chartStyle to js part
- incorporating charts in new scalajs-based scheme
- TODO: consider using https://scalacenter.github.io/scalajs-bundler/
 
2017-08-16

- extract Charter from app.js to charter.js for reuse
 
2017-08-15

- exploring some stuff ... before adding Var/Vars within the Vm model... 
  https://github.com/OlivierBlanvillain/monadic-html/issues/13
- LLMap to interface with Leaflet
- make ss1.demo.sh generate deterministic timestamps and values
- some better organization
- less opaque variable definition structure
- start using binding.scala
- more scalajs preps re notification handling

2017-08-12

- using webjars for several of the js libraries.
  Note: still using `leaflet.css` from CDN as I could not find a way to extract css 
  resources from the leaflet webjar.
- `sbt package` to copy js resources so they get resolved for webapp
- start using autowire
- webSocket preparations. Pusher config now optional; if missing, webSockets (to be) used

2017-08-11

- move web stuff to jvm/src/main/resources
- preparing for scalaJs

2017-08-10

- tried again to enable scoverage, but got warnings/errors 
 (coverage: `[warn] Could not instrument [EmptyTree$/null]. No pos.`;
 coverageReport: `(*:coverageReport) No source root found for '/Users/carueda/github/carueda/gpdviz2/<macro>' (source roots: '/Users/carueda/github/carueda/gpdviz2/src/main/scala/')`)
 ).
- use carueda.cfg
- use sbt 0.13.13, scala 2.12; some dependency updates

2017-06-07

- some style adjustment (incl large mouse position font)

2017-05-19

- add cors
- migrate to akka-http

2017-05-16

- reflect updates upon sensor system unregistration and stream removal

2017-05-12

- sensor system can now have a clickListener, a URL that will be called with information about 
  clicks on the map but only those with at least one of the shiftKey/altKey/metaKey modifiers.

2017-05-11

- now use absolute-positioned chart unless str.chartStyle.useChartPopup
  (TODO handling of multiple absolute chars)
- use leaflet@1.0.0

2017-05-10

- improved chart redrawing logic
- improved handling of mouse-hover to highlight position on map

2017-05-08

- skip the doobie/postgres code for now (issues with assembly duplicates...)
- experimenting with doobie/postgres.
  define config property `gpdviz.postgres.connection.url` to exercise it.
  db setup:

        psql -c 'create user postgres createdb'
        psql -c 'create database gpdviz;' -U postgres
        
        psql -d gpdviz -U postgres
        
        gpdviz=# CREATE EXTENSION Postgis;
        CREATE EXTENSION
        gpdviz=# \dT geography
                                List of data types
         Schema |   Name    |                 Description
        --------+-----------+----------------------------------------------
         public | geography | postgis type: Ellipsoidal spatial data type.
        (1 row)
        
2017-05-07

- extract Db trait to prepare for use of actual database system 

2017-05-06

- chart: addPoint with no redraw, and set timer for redraw 
- update numberObs and latestIso per stream 
- removal of previous "chart_ts_data" stuff 
- yAxis indicated by client
- DataStream: 'variables' as an JsObject to be able to associate properties per variable definition
- DataStream: rename style to mapStyle, and add chartStyle
- positionsByTime indexed by strid and time
- more with scalarData (position)
- add name and description to DataStream
- preliminaries with scalarData

2017-05-01

- positionsByTime mapping: capture various quantized levels of given time per position

2017-04-27

- set ordinal=false to better reflect points across (time) xAxis
- add some map layers
- set useUTC=true for the charts

2017-04-25

- exploring new model for observations
- capture position in timestamped data element; along with timestamp this allows to show it on mousemove on chart
- preliminaries for handling of hovered mouse position on chart toward highlighting corresponding locations on map
- dynamic adjustment of map height depending on window height (rather ad hoc for now) 
- capture iso time of latest observation (still under revision) and other very sketchy display adjustments

2017-04-24

- add cfg.gpdviz.serverName as a way to specify particular instance (so UI subscribes
  only to events from corresponding server instance)
- highchart adjustments 

2017-04-23

- now chartTsData, not chartData: as a next step toward better model (but still preliminary)
- at init, add non-charData observations first so the marker has already been 
  associated to the relevant streams when adding the charData

2017-04-20

- capture name and description in sensor system model
- avoid cyclic references (mainly to allow json display/debugging of vm)
- set popup only when chartData is associated
- some rather minor adjs

2016-10-14

- preliminary revisit of the [initial idea](https://github.com/carueda/gpdviz)