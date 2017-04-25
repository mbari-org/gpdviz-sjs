# gpdviz2

2017-04-24

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