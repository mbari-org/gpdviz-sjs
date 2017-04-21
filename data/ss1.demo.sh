#!/usr/bin/env bash

GPDVIZ=http://localhost:5050

SS=ss1

function run() {
	unregister
	register

	add_str1
	add_str2
	add_str3

	add_str1_values
	add_str2_values
	add_str3_values

	add_str2_data

	add_str4_and_point
	add_str4_data
}

function unregister() {
    echo "unregister ${SS}"
	http delete ${GPDVIZ}/api/ss/${SS} > /dev/null
}

function register() {
    echo "register ${SS}"
	http post ${GPDVIZ}/api/ss sysid=${SS} name='Test sensor system' center:='{"lat":36.62, "lon":-122}' pushEvents:=true > /dev/null
}

function add_str1() {
    echo "add stream str1"
	http post ${GPDVIZ}/api/ss/${SS} strid=str1 style:='{"color":"green", "dashArray": "5,5"}' > /dev/null
}

function add_str2() {
    echo "add stream str2"
	http post ${GPDVIZ}/api/ss/${SS} strid=str2 style:='{"color":"red", "radius": 14}' zOrder:=10 > /dev/null
}

function add_str3() {
    echo "add stream str3"
	http post ${GPDVIZ}/api/ss/${SS} strid=str3 style:='{"color":"blue"}' > /dev/null
}

function add_str1_values() {
	timestamp="`date +%s`000"
	read -r -d '' values <<-EOF
		[{
		  "timestamp": ${timestamp},
		  "feature": {
			"properties": {},
			"geometry": {
			  "type": "Polygon",
			  "coordinates": [
				[[-121.8564,36.9], [-122.2217,36.9574], [-122.0945,36.6486], [-121.8674,36.6858]]
			  ]
			}
		  }
		}]
EOF
	add_values str1 "${values}"
}

function add_str2_values() {
	timestamp="`date +%s`000"
	read -r -d '' values <<-EOF
		[{
		  "timestamp": ${timestamp},
		  "geometry": {
			"type": "Point",
			"coordinates": [-121.906,36.882]
		  }
		 },{
		  "timestamp": ${timestamp},
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
	add_values str2 "${values}"
}

function add_str3_values() {
	timestamp="`date +%s`000"
	read -r -d '' values <<-EOF
		[{"timestamp": ${timestamp},
		  "geometry": {
			"type": "LineString",
			"coordinates": [
			[-122.123,36.92], [-122.186,36.774], [-121.9,36.7]
			]
		  }
		}]
EOF
	add_values str3 "${values}"
}

function add_values() {
	strid=$1
	values=$2
	http post ${GPDVIZ}/api/ss/${SS}/${strid} values:="${values}" > /dev/null
}

function add_str2_data() {
	strid=str2
	echo "chart data: ${strid}"
	timestamp="`date +%s`000"
	secs=30
	timestamp=$(( timestamp - secs * 1000 ))
	values='['
	comma=""
	for i in `seq ${secs}`; do
		val0=$(( (RANDOM % 100) + 1 ))
		val1=$(( (RANDOM % 100) + 1 ))
		element='{ "timestamp": '${timestamp}', "chartData": [ '${val0}', '${val1}' ]}'
		values="${values}${comma}${element}"
		comma=","
		timestamp=$(( timestamp + 1000 ))
	done
	values="${values}]"
	add_values "${strid}" "${values}"
}

function add_str4_and_point() {
    strid=str4
	http post ${GPDVIZ}/api/ss/${SS} strid=${strid} style:='{"color":"yellow", "radius": 10}' zOrder:=10 > /dev/null
	timestamp="`date +%s`000"
	read -r -d '' values <<-EOF
		[{
		  "timestamp": ${timestamp},
		  "geometry": {
			"type": "Point",
			"coordinates": [-122.09,36.865]
		  }
		}]
EOF
	http post ${GPDVIZ}/api/ss/${SS}/${strid} values:="${values}" > /dev/null
	
}
function add_str4_data() {
    strid=str4
	secs=60
	for i in `seq ${secs}`; do
	    timestamp="`date +%s`000"
		val=$(( (RANDOM % 100) + 1 ))
		element='{ "timestamp": '${timestamp}', "chartData": [ '${val}' ]}'
        add_values "${strid}" "[${element}]"
	    echo "added value to ${strid}: ${val}"
        sleep 1
	done
}

if [ "$1" != "" ]; then
    $1
else
    run
fi
