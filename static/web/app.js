(function () {
  'use strict';

  angular.module('gpdviz', [])
    .controller('GpdvizController', GpdvizController)
  ;

  var debug = window && window.location.toString().match(/.*\?debug/);

  GpdvizController.$inject = ['$scope', '$window', '$timeout'];
  function GpdvizController($scope, $window, $timeout) {
    var vm = this;
    vm.debug = debug;

    var byStrId = [];

    var center = [36.62, -122.04];
    var map = L.map('mapid', {maxZoom: 20}).setView(center, 11);
    var esriOceansLayer = L.esri.basemapLayer('Oceans').addTo(map);
    var osm = L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {
      attribution: '&copy; <a target="_blank" href="http://osm.org/copyright">OpenStreetMap</a> contributors'
    });

    L.control.mousePosition({position: 'topright', emptyString: ''}).addTo(map);

    var markersLayer = new L.LayerGroup();
    var markersLayerMG = new L.LayerGroup();
    markersLayer.addTo(map);

    var controlLayers = L.control.layers(
      {
        'ESRI Oceans': esriOceansLayer,
        'OpenStreetMap': osm,
        'Google satellite': L.gridLayer.googleMutant({
          type: 'satellite' // valid values are 'roadmap', 'satellite', 'terrain' and 'hybrid'
        })
      }
    ).addTo(map);

    var overlayGroupByStreamId = {};

    var positionsByTime = {};  // time -> [lat, lon]
    function setPositionByTime(timeMs, position) {
      positionsByTime[timeMs.toFixed(0)]           = position;
      positionsByTime[(timeMs / 1000).toFixed(0)]           = position; // by second
      positionsByTime[(timeMs / 1000 / 60).toFixed(0)]      = position; // by minute
      positionsByTime[(timeMs / 1000 / 60 / 60).toFixed(0)] = position; // by hour
    }
    function getPositionByTime(timeMs) {
      return positionsByTime[timeMs.toFixed(0)]
          || positionsByTime[(timeMs / 1000).toFixed(0)]      // by minute
          || positionsByTime[(timeMs / 1000 / 60).toFixed(0)]      // by minute
          || positionsByTime[(timeMs / 1000 / 60 / 60).toFixed(0)] // by hour
      ;
    }

    var selectionGroup = new L.LayerGroup().addTo(map);

    function addMarker(str, createMarker) {
      var strid = str.strid;

      var marker = createMarker();
      marker.addTo(map);
      byStrId[strid].marker = marker;
      markersLayer.addLayer(marker);

      var group = overlayGroupByStreamId[strid];
      if (!group) {
        group = new L.LayerGroup().addTo(map);
        overlayGroupByStreamId[strid] = group;
        controlLayers.addOverlay(group, strid);
      }
      group.addLayer(marker);

      marker = createMarker();
      markersLayerMG.addLayer(marker);
    }

    function clearMarkers() {
      markersLayer.clearLayers();
      markersLayerMG.clearLayers();
      _.each(overlayGroupByStreamId, function(group) {
        controlLayers.removeLayer(group);
        map.removeLayer(group);
      });
      overlayGroupByStreamId = {};
    }

    (function setMagnifyingGlass() {
      var magnifyingGlass = L.magnifyingGlass({
        //zoomOffset: 3,
        //fixedZoom: 14,
        //radius: 200,
        layers: [
          //L.esri.basemapLayer('Oceans'),
          markersLayerMG
        ]
      });

      //map.addLayer(magnifyingGlass);
      map.on('contextmenu', function (mouseEvt) {
        if (!map.hasLayer(magnifyingGlass)) {
          map.addLayer(magnifyingGlass);
          magnifyingGlass.setLatLng(mouseEvt.latlng);
        }
        else {
          map.removeLayer(magnifyingGlass);
        }
      });
    })();

    function addSelectionPoint(p) {
      // console.debug("addSelectionPoint: p=", p);
      selectionGroup.clearLayers();
      if (!p) return;
      var marker = L.marker([p[0], p[1]], {
        keyboard: false,
        opacity: 0.6
      }).addTo(map);
      selectionGroup.addLayer(marker);
    }

    vm.ss = Gpdviz.initialSs;

    if (vm.ss) {
      if (vm.ss.center && vm.ss.center.lat && vm.ss.center.lon) {
        map.setView([vm.ss.center.lat, vm.ss.center.lon], 11)
      }

      var streams = _.sortBy(_.values(vm.ss.streams), 'zOrder');
      _.each(streams, function (str) {
        byStrId[str.strid] = {str: str};

        // little hack: add non-charData observations first...
        _.each(str.obs, function (obs) {
          if (!obs.chartTsData)
            addObs(str, obs);
        });
        // ... so the marker has already been associated to the relevant streams:
        _.each(str.obs, function (obs) {
          if (obs.chartTsData)
            addObs(str, obs);
        });

        str.numberObs = 0;
        if (str.obs && str.obs.length) {
          var latestTsd = str.obs[str.obs.length-1];
          if (latestTsd.chartTsData && latestTsd.chartTsData.length) {
            var timestamp = latestTsd.chartTsData[latestTsd.chartTsData.length - 1].timestamp;
          }
          else {
            timestamp = latestTsd.timestamp;
          }
          str.latestIso = moment.utc(timestamp).format();

          _.each(str.obs, function(obs) {
            if (obs.chartTsData) {
              str.numberObs += obs.chartTsData.length;
            }
            else {
              str.numberObs += 1;
            }
          })
        }
      });
      vm.ss.orderedStreams = _.chain(streams).values().sortBy('strid').value();
    }

    channel.bind('my_event', function (payload) {
      handleNotification(payload);
    });

    function handleNotification(payload) {
      if (debug) console.debug("handleNotification: payload=", payload);
      var what = payload.what;
      var data = payload.data;
      var str;

      if (what === 'sensorSystemRegistered') {
        $scope.$apply(function () {
          vm.ss = data;
          vm.ss.streams = vm.ss.streams || {};
        });
      }

      else if (what === 'sensorSystemRefresh') {
        window.location.reload(true);
      }

      else if (what === 'streamAdded') {
        str = angular.fromJson(data.str);
        str.obs = [];
        $scope.$apply(function () {
          vm.ss.streams[str.strid] = str;
          byStrId[str.strid] = {str: str};
        });
      }

      else if (what === 'observationsAdded') {
        str = vm.ss.streams[data.strid];
        if (debug) console.debug("str=", str, "data.strid=", data.strid, "data.obss=", data.obss);
        str.obs = str.obs || [];
        $scope.$apply(function () {
          _.each(data.obss, function (data_obs) {
            var obs = {
              timestamp: data_obs.timestamp,
              feature:   data_obs.feature ? angular.fromJson(data_obs.feature) : undefined,
              geometry:  data_obs.geometry? angular.fromJson(data_obs.geometry) : undefined,
              chartTsData: data_obs.chartTsData ? angular.fromJson(data_obs.chartTsData) : undefined
            };
            str.obs.push(obs);
            addObs(str, obs);
          });
        });
      }

      else if (what === 'streamRemoved') {
        $scope.$apply(function () {
          delete vm.ss.streams[data.strid];
          delete byStrId[data.strid];
        });
      }

      else if (what === 'sensorSystemUpdated') {

      }

      else if (what === 'sensorSystemUnregistered') {
        $scope.$apply(function () {
          vm.ss = undefined;
        });
        clearMarkers();
      }

      else {
        console.warn("Unexpected notification: payload=", payload)
      }
    }

    map.on('popupopen', function(e) {
      var strid = e.popup && e.popup._strid;
      var str = strid && byStrId[strid].str;
      //console.debug("popupopen: str=", str);
      if (str && byStrId[strid].charter) {
        //console.debug("popupopen: charter=", byStrId[str.strid].charter);
        byStrId[str.strid].charter.activate();
      }
    });
    map.on('popupclose', function(e) {
      var strid = e.popup && e.popup._strid;
      var str = strid && byStrId[strid].str;
      //console.debug("popupclose: str=", str);
      if (str && byStrId[strid].charter) {
        var charter = byStrId[str.strid].charter;
        //console.debug("popupclose: charter=", charter);
        charter.deactivate();
      }
      $scope.$apply(function() {
        vm.hoveredPointIso = undefined;
        addSelectionPoint();
      });
    });

    function addObs(str, obs) {
      if (!obs.chartTsData && !obs.feature && !obs.geometry) {
        console.error("expecting observation with feature, geometry, or chartTsData");
        return;
      }

      var popupInfo;

      if (obs.chartTsData) {
        //console.debug("str=", str, "obs.chartTsData=", obs.chartTsData);
        if (!byStrId[str.strid].charter) {
          byStrId[str.strid].charter = Charter(str.strid, str.variables, function(point) {
            if (point) {
              //console.debug("hovered point=", point.x, vm.hoveredPointIso);
              $scope.$apply(function() {
                vm.hoveredPointIso = moment.utc(point.x).format();
                var p = getPositionByTime(point.x);
                if (p) {
                  addSelectionPoint([p.lat, p.lon]);
                }
                //addSelectionPoint([36.857, -122.341 + Math.random()/10.0]);
              });
            }
          });
        }
        _.each(obs.chartTsData, function(tsd) {
          _.each(tsd.values, function(v, index) {
            if (byStrId[str.strid].maxTimestamp === undefined
              || byStrId[str.strid].maxTimestamp < tsd.timestamp) {
              byStrId[str.strid].maxTimestamp = tsd.timestamp;
              byStrId[str.strid].charter.addChartPoint(index, tsd.timestamp, v);
            }
            if (tsd.position) {
              setPositionByTime(tsd.timestamp, tsd.position);
            }
          });
        });

        if (byStrId[str.strid].marker && !byStrId[str.strid].popupInfo) {
          if (debug) console.debug("setting popup for stream ", str.strid);
          popupInfo = L.popup({
            //autoClose: false, closeOnClick: false
            minWidth: 550
          });
          popupInfo._strid = str.strid;
          popupInfo.setContent('<div id="' +"chart-container-" + str.strid +
            '" style="min-width:500px;height:300px;margin:0 auto"></div>');

          byStrId[str.strid].marker.bindPopup(popupInfo);
          byStrId[str.strid].popupInfo = popupInfo;
        }
        return;
      }

      var style = str.style || {};

      if (obs.feature) {
        var geojson = angular.fromJson(obs.feature);
        var geometry = obs.feature.geometry;
        if (obs.feature.properties && obs.feature.properties.style) {
          style = _.assign(style, obs.feature.properties.style);
        }
      }
      else {
        geojson = angular.fromJson(obs.geometry);
        geometry = obs.geometry;
      }

      if (debug) console.debug("addObs: style=", style, "str=", str, "geojson=", geojson);

      addMarker(str, function() {
        return  L.geoJSON(geojson, {
          style: style,
          pointToLayer: function (feature, latlng) {
            if (!style.radius) {
              style.radius = 5;
            }
            return L.circleMarker(latlng, style);
          }
        });
      });
    }

    (function prepareAdjustMapUponWindowResize() {
      var minHeight = 350;

      var mapContainer = document.getElementById('mapid');
      var marginBottom = 5;

      updateWindowSize();
      angular.element($window).bind('resize', function () {
        updateWindowSize();
        $scope.$digest();
      });

      function updateMapSize(windowHeight) {
        if (windowHeight) {
          $('#mapid').css("height", windowHeight);
        }
        L.Util.requestAnimFrame(function () {
          map.invalidateSize({debounceMoveend: true}); }, map);
      }

      function updateWindowSize(force) {
        var rect = mapContainer.getBoundingClientRect();
        //console.log("getBoundingClientRect=", rect.top, rect.right, rect.bottom, rect.left);
        //console.log("$window.innerHeight=", $window.innerHeight, " - rect.top=", rect.top);

        var restWindowHeight = $window.innerHeight - rect.top - marginBottom;
        //console.log("restWindowHeight=", restWindowHeight);

        if (restWindowHeight < minHeight) {
          restWindowHeight = minHeight;
        }
        updateMapSize(restWindowHeight);
      }

      function delayedUpdate(force, delay) {
        $timeout(function () {
          updateWindowSize(force);
          $scope.$digest();
        }, delay);
      }

      delayedUpdate(true, 2000);
    })();
  }

  Highcharts.setOptions({
    global: {
      useUTC: true
    }
  });

  function Charter(strid, names, hoveredPoint) {
    var title = "";
    var sep = "";
    var initialSeriesData = _.map(names, function(name) {
      title += sep + name;
      sep = " | ";
      return {
        name: name,
        data: []
        ,marker: {
          enabled: true,
          radius: 2
        }
        ,lineWidth: 1
        //,states: {
        //  hover: {
        //    lineWidthPlus: 0
        //  }
        //}
      };
    });
    var chart = undefined;
    var serieses = undefined;

    return {
      strid: strid,
      addChartPoint: addChartPoint,
      activate: activate,
      deactivate: deactivate
    };

    function addChartPoint(seriesIndex, x, y) {
      //console.debug("addChartPoint: strid=", strid);
      initialSeriesData[seriesIndex].data.push([x, y]);
      if (serieses) {
        serieses[seriesIndex].addPoint([x, y], true, true);
      }
    }

    function activate() {
      deactivate();
      _.each(initialSeriesData, function(s) {
        s.data = _.sortBy(s.data, function(xy) { return xy[0] });
      });
      chart = createChart();

      if (hoveredPoint) {
        $(chart.container).mousemove(function(e) {
          var event = chart.pointer.normalize(e.originalEvent);
          // console.debug("normalizedEvent=", event);
          var point = chart.series[0].searchPoint(event, true);
          hoveredPoint(point);
        });
      }
    }

    function deactivate() {
      if (chart) chart.destroy();
      chart = undefined;
      serieses = undefined;
      if (hoveredPoint) {
        // TODO remove mousemove event handler
      }
    }

    function createChart() {
      // http://stackoverflow.com/q/23624448/830737
      return new Highcharts.StockChart({
        chart: {
          renderTo: "chart-container-" + strid,
          events: {
            load: function () {
              serieses = this.series;
            },
            zoomType: 'x'
          }
        },
        //title: { text: "TODO" },
        xAxis: {
          type: 'datetime',
          ordinal: false
        },
        legend: { enabled: false },

        series: initialSeriesData || [],

        tooltip: {
          pointFormat: '<span style="color:{series.color}">{series.name}</span>: <b>{point.y}</b><br/>',
          valueDecimals: 4,
          split: true
        },

        rangeSelector: {
          buttons: [{
            count: 10,
            type: 'minute',
            text: '10m'
          }, {
            count: 1,
            type: 'hour',
            text: '1H'
          }, {
            count: 12,
            type: 'hour',
            text: '12H'
          }, {
            count: 1,
            type: 'day',
            text: '1D'
          }, {
            type: 'all',
            text: 'All'
          }],
          inputEnabled: false,
          selected: 1
        },

        title: {
         text: names[0]
        },

        navigator: {
          enabled: true
        },
        scrollbar: {
          enabled: true
        }

        ,plotOptions: {
          series: {
            states: {
              hover: {
                lineWidthPlus: 0
              }
            }
          }
        }
      });
    }
  }

})();
