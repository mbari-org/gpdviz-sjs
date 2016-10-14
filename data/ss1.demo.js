#!/usr/bin/env node

const request = require('request')

const GPDVIZ = "http://localhost:5050"

const SS = "ss1"

function run() {
  //unregister()
  //unregister(then(register))
  //unregister(then(register, update))
  unregister(then(register, then(update, add_stream)))
}

function unregister(next) {
  console.log("unregister")
  request({
    url: GPDVIZ + "/api/ss/" + SS,
    method: "DELETE"
  }, do_next(next))
}

function register(next) {
  console.log("register")
  request({
    url: GPDVIZ + "/api/ss",
    method: "POST",
    json: {
      "sysid": SS,
      center: {"lat":36, "lon":-122},
      pushEvents: true
    }
  }, do_next(next))
}

function update(next) {
  console.log("update")
  request({
    url: GPDVIZ + "/api/ss/" + SS,
    method: "PUT",
    json: {
      center: {"lat":36.8, "lon":-122.04}
    }
  }, do_next(next))
}

function add_stream(next) {
  console.log("add_stream")
  request({
    url: GPDVIZ + "/api/ss/" + SS,
    method: "POST",
    json: {
      "strid": "str1",
      style: {"color":"green", "dashArray": "5,5"}
    }
  }, do_next(next))
}

////////////////////////////////////////////

function then(next, next2) {
  return function(err, res) {
    if (err) console.error(err)
    else if (next2) next(then(next2))
    else next()
  }
}
function do_next(next) {
  return function(err, res) {
    if (err) console.error(err)
    else if (next) next()
    else console.log("Done")
  }
}

run()
