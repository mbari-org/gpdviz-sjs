#!/usr/bin/env bash

GPDVIZ=http://localhost:5050

SS=ss1

BASE_TIMESTAMP_MS=1503090553000
RANDOM=1341

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
	now=${BASE_TIMESTAMP_MS}
	secs=60
	timestamp=$(( now - secs * 1000 ))
	add_str1_polygon ${timestamp}
	add_scalars str1 $(( timestamp + 1000 )) $(( secs - 1 ))
}

function add_str1() {
    echo "add stream str1"
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
	     }, {
	       "name": "temperature",
	       "units": "°",
	       "chartStyle": {
	         "yAxis": 1
	       }
	     }]' \
	     mapStyle:='{"color":"green", "dashArray": "5,5"}' > /dev/null
}

function add_str1_polygon() {
	timestamp=$1
    read -r -d '' geometry <<-EOF
      "${timestamp}": [{
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
	timestamp=$(( timestamp - secs * 1000 ))
	
	# geometries:
	read -r -d '' geometry <<-EOF
	  "${timestamp}": [
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
		timestamp=$(( timestamp + 1000 ))
		val0=$(( (RANDOM % 100) + 1 ))
		val1=$(( (RANDOM % 100) + 1 ))
		
		# element='{ "timestamp": '${timestamp}', "chartTsData": [ { "timestamp": '${timestamp}', "values": [ '${val0}', '${val1}' ] } ]}'
		
        read -r -d '' element <<-EOF
          "${timestamp}": [{
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

	timestamp=${BASE_TIMESTAMP_MS}
    read -r -d '' geometry <<-EOF
      "${timestamp}": [{
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

	timestamp=${BASE_TIMESTAMP_MS}
    read -r -d '' geometry <<-EOF
      "${timestamp}": [{
          "geometry": {
            "type": "Point",
            "coordinates": [-122.09,36.865]
          }
      }]
EOF
    observations="{${geometry}}"
    add_observations "${strid}" "${observations}"
    echo "added ${strid} point: timestamp=${timestamp}"
	
	add_delayed_data ${strid} temperature 10
}

function add_delayed_data() {
    strid=$1
    varName=$2
	secs=$3
    timestamp=${BASE_TIMESTAMP_MS}
	for i in `seq ${secs}`; do
        sleep 1
	    timestamp=$(( timestamp + 1000 ))
		val=$(( (RANDOM % 100) + 1 ))
        read -r -d '' element <<-EOF
          "${timestamp}": [{
            "scalarData": {
              "vars": ["${varName}"],
              "vals": [${val}]
            }}]
EOF
        observations="{${element}}"
        add_observations "${strid}" "${observations}"
	    echo "added observation to ${strid}: timestamp=${timestamp}"  # ${observations}"
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
	timestamp=$2
	secs=$3
	echo "scalarData: ${strid} timestamp=${timestamp}  secs=${secs}"
	observations='{'
	comma=""
	for i in `seq ${secs}`; do
		val0=$(( RANDOM % 1000 - 500 ))
		val1=$(( RANDOM % 100 ))
		lat="36.8$((   RANDOM % 1000 ))"
		lon="-122.1$(( RANDOM % 1000 ))"
    	read -r -d '' element <<-EOF
		  "${timestamp}": [{
            "scalarData": {
              "vars": ["baz","temperature"],
              "vals": [${val0}, ${val1}],
              "position": {"lat": ${lat}, "lon": ${lon}}
            }}]
EOF
		observations="${observations}${comma}${element}"
		comma=", "
		timestamp=$(( timestamp + 1000 ))
	done
	observations="${observations}}"
	add_observations "${strid}" "${observations}"
}

if [ "$1" != "" ]; then
    $1 $2 $3 $4 $5
else
    run
fi
