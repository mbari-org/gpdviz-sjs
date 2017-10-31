#!/usr/bin/env bash

GPDVIZ="${GPDVIZ_SERVER:-http://localhost:5050}"

echo "Using gpdviz server = ${GPDVIZ}"

SS=ss1

baseTimeSec=1503090553

RANDOM=1341

function timeSec2iso() {
    secs=$1
    gdate --date="@${secs}" --iso-8601=seconds
}

function run() {
	unregister
	register

    generate_str1
    generate_str2
    generate_str3
    generate_str4
}

function unregister() {
    echo "unregister ${SS}"
	http delete ${GPDVIZ}/api/ss/${SS} > /dev/null
}

function register() {
    echo "register ${SS}"
	http post ${GPDVIZ}/api/ss sysid=${SS} \
	     name='Test sensor system' \
	     description='Description of Test sensor system' \
	     center:='{"lat":36.82, "lon":-122}' \
	     pushEvents:=true > /dev/null
}

function generate_str1() {
	add_str1
	now=${baseTimeSec}
	secs=60
	timeSec=$(( now - secs ))
	add_str1_polygon ${timeSec}
	add_scalars str1 $(( timeSec + 1 )) $(( secs - 1 ))
}

function add_str1() {
    echo "add stream str1 (with baz variable)"
	http post ${GPDVIZ}/api/ss/${SS} strid=str1 \
	     name="Stream one" \
	     description="Description of Stream one" \
	     chartStyle:='{
	       "useChartPopup": true,
	       "height": 500,
            "yAxis": [{
	             "height": "50%",
	             "title": {"text": "baz (m)"},
	             "opposite": false,
	             "offset": -10
	           }, {
	             "top": "55%",
	             "height": "45%",
	             "title": {"text": "temperature (°)"},
	             "opposite": false,
	             "offset": -10
            }]
	     }' \
	     variables:='[
	     {
	       "name": "baz",
	       "units": "m",
	       "chartStyle": {
	         "yAxis": 0,
	         "type": "column"
	       }
	     }]' \
	     mapStyle:='{"color":"green", "dashArray": "5,5"}' > /dev/null

    echo "add temperature variable to stream str1"
	http post ${GPDVIZ}/api/ss/${SS}/str1/vd \
	       name="temperature" \
	       units="°" \
	       chartStyle:='{
	         "yAxis": 1
	       }'
}

function add_str1_polygon() {
	timeSec=$1
    read -r -d '' geometry <<-EOF
      "`timeSec2iso ${timeSec}`": [{
        "geometry": {
          "type": "Polygon",
          "coordinates": [
            [[-121.8564,36.9], [-122.2217,36.9574], [-122.0945,36.6486], [-121.8674,36.6858]]
          ]
        }
      }]
EOF
    observations="{${geometry}}"
    add_observations str1 "${observations}"
}

function generate_str2() {
	add_str2
	
	secs=30
	timeSec=$(( timeSec - secs ))
	
	# geometries:
	read -r -d '' geometry <<-EOF
	  "`timeSec2iso ${timeSec}`": [
	    {
		  "geometry": {
			"type": "Point",
			"coordinates": [-121.906,36.882]
		  }
		}, {
		  "feature": {
			"properties": {
			  "style": {"color":"cyan", "radius": 20, "dashArray": "5,5"}
			},
			"geometry": {
			  "type": "Point",
			  "coordinates": [-121.965,36.81]
			}
		  }
		}]
EOF
    observations="{${geometry}}"
    add_observations str2 "${observations}"
	
	# data:
	echo "chart data: str2"
	observations='{'
	comma=""
	for i in `seq ${secs}`; do
		timeSec=$(( timeSec + 1 ))
		val0=$(( (RANDOM % 100) + 1 ))
		val1=$(( (RANDOM % 100) + 1 ))
		
        read -r -d '' element <<-EOF
          "`timeSec2iso ${timeSec}`": [{
            "scalarData": {
              "vars": ["foo", "bar"],
              "vals": [${val0}, ${val1}]
            }}]
EOF
		observations="${observations}${comma}${element}"
		comma=","
	done
	observations="${observations}}"
	add_observations str2 "${observations}"
}

function add_str2() {
    echo "add stream str2"
	http post ${GPDVIZ}/api/ss/${SS} strid=str2 \
	     variables:='[ { "name": "foo"}, {"name": "bar" } ]' \
	     mapStyle:='{"color":"red", "radius": 14}' zOrder:=10 > /dev/null
}

function generate_str3() {
	add_str3

	timeSec=${baseTimeSec}
    read -r -d '' geometry <<-EOF
      "`timeSec2iso ${timeSec}`": [{
        "geometry": {
			"type": "LineString",
			"coordinates": [
			    [-122.123,36.92], [-122.186,36.774], [-121.9,36.7]
			]
        }
      }]
EOF
    observations="{${geometry}}"
    add_observations str3 "${observations}"
}

function add_str3() {
    echo "add stream str3"
	http post ${GPDVIZ}/api/ss/${SS} strid=str3 \
	     mapStyle:='{"color":"blue"}' > /dev/null
}

function generate_str4() {
    strid=str4
	http post ${GPDVIZ}/api/ss/${SS} strid=${strid} \
	    name="${strid}_name" \
	    variables:='[ { "name": "temperature", "units": "°C" } ]' \
	    mapStyle:='{"color":"yellow", "radius": 10}' zOrder:=10 > /dev/null

	timeSec=${baseTimeSec}
    read -r -d '' geometry <<-EOF
      "`timeSec2iso ${timeSec}`": [{
          "geometry": {
            "type": "Point",
            "coordinates": [-122.09,36.865]
          }
      }]
EOF
    observations="{${geometry}}"
    add_observations "${strid}" "${observations}"
    echo "added ${strid} point: timeSec=${timeSec}"
	
	add_delayed_data ${strid} temperature 10
}

function add_delayed_data() {
    strid=$1
    varName=$2
	secs=$3
    timeSec=${baseTimeSec}
	for i in `seq ${secs}`; do
        sleep 1
	    timeSec=$(( timeSec + 1 ))
		val=$(( (RANDOM % 100) + 1 ))
        read -r -d '' element <<-EOF
          "`timeSec2iso ${timeSec}`": [{
            "scalarData": {
              "vars": ["${varName}"],
              "vals": [${val}]
            }}]
EOF
        observations="{${element}}"
        add_observations "${strid}" "${observations}"
	    echo "added observation to ${strid}: timeSec=${timeSec}"  # ${observations}"
	done
}

function add_observations() {
	strid=$1
	observations=$2
	http post ${GPDVIZ}/api/ss/${SS}/${strid}/obs \
	     observations:="${observations}" > /dev/null
}

function add_scalars() {
	strid=$1
	timeSec=$2
	secs=$3
	echo "scalarData: ${strid} timeSec=${timeSec}  secs=${secs}"
	observations='{'
	comma=""
	for i in `seq ${secs}`; do
		val0=$(( RANDOM % 1000 - 500 ))
		val1=$(( RANDOM % 100 ))
		lat="36.8$((   RANDOM % 1000 ))"
		lon="-122.1$(( RANDOM % 1000 ))"
    	read -r -d '' element <<-EOF
		  "`timeSec2iso ${timeSec}`": [{
            "scalarData": {
              "vars": ["baz","temperature"],
              "vals": [${val0}, ${val1}],
              "position": {"lat": ${lat}, "lon": ${lon}}
            }}]
EOF
		observations="${observations}${comma}${element}"
		comma=", "
		timeSec=$(( timeSec + 1 ))
	done
	observations="${observations}}"
	add_observations "${strid}" "${observations}"
}

if [ "$1" != "" ]; then
    $1 $2 $3 $4 $5
else
    run
fi
