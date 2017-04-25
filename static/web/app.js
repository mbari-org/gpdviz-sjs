(function () {
  'use strict';

  angular.module('gpdviz', [])
    .controller('GpdvizController', GpdvizController)
  ;

  var debug = window && window.location.toString().match(/.*\?debug/);

  GpdvizController.$inject = ['$scope'];
  function GpdvizController($scope) {
    var vm = this;
    vm.debug = debug;

    var byStrId = [];

    var center = [36.62, -122.04];
    var map = L.map('mapid', {maxZoom: 20}).setView(center, 11);
    var layer = L.esri.basemapLayer('Oceans').addTo(map);
    L.control.mousePosition({position: 'topright', emptyString: ''}).addTo(map);

    var markersLayer = new L.LayerGroup();
    var markersLayerMG = new L.LayerGroup();
    markersLayer.addTo(map);

    var controlLayers = L.control.layers(
      {
        'ESRI Oceans': layer,
        'Empty': L.tileLayer('')
      },
      {},
      {hideSingleBase: true}).addTo(map);

    var overlayGroupByStreamId = {};

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
      });
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
          byStrId[str.strid].charter = Charter(str.strid, str.variables);
        }
        _.each(obs.chartTsData, function(tsd) {
          _.each(tsd.values, function(v, index) {
            byStrId[str.strid].charter.addChartPoint(index, tsd.timestamp, v);
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
  }

  Highcharts.setOptions({
    global: {
      useUTC: false
    }
  });

  function Charter(strid, names) {
    var initialSeriesData = _.map(names, function(name) {
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
    }

    function deactivate() {
      if (chart) chart.destroy();
      chart = undefined;
      serieses = undefined;
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
        xAxis: { type: 'datetime' },
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
          selected: 0
        },

        //title: {
        //  text: 'title..'
        //},

        navigator: {
          enabled: true
        },
        scrollbar: {
          enabled: true
        }
      });
    }
  }

})();
