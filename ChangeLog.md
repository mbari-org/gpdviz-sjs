# gpdviz2

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