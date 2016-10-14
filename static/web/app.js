(function () {
  'use strict';

  angular.module('gpdviz', [])
    .controller('GpdvizController', GpdvizController)
  ;

  GpdvizController.$inject = ['$scope'];
  function GpdvizController($scope) {
    console.debug("==GpdvizController==");
    var vm = this;
    vm.debug = false;

    var center = [36.8, -122.04];
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

    function addMarker(strid, createMarker, popupInfo) {
      var m = createMarker();
      m.addTo(map);
      if (popupInfo) m.bindPopup(popupInfo);
      markersLayer.addLayer(m);

      var group = overlayGroupByStreamId[strid];
      if (!group) {
        group = new L.LayerGroup().addTo(map);
        overlayGroupByStreamId[strid] = group;
        controlLayers.addOverlay(group, strid);
      }
      group.addLayer(m);

      m = createMarker();
      //if (popupInfo) m.bindPopup(popupInfo);
      markersLayerMG.addLayer(m);
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
        _.each(str.obs, function (obs) {
          addObs(str, obs);
        });
      });
    }

    channel.bind('my_event', function (payload) {
      handleNotification(payload);
    });

    function handleNotification(payload) {
      //console.debug("handleNotification: payload=", payload);
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
        });
      }

      else if (what === 'observationsAdded') {
        str = vm.ss.streams[data.strid];
        console.debug("str=", str, "data.strid=", data.strid, "data.obss=", data.obss);
        str.obs = str.obs || [];
        $scope.$apply(function () {
          _.each(data.obss, function (data_obs) {
            var obs = {
              timestamp: data_obs.timestamp,
              feature:   data_obs.feature ? angular.fromJson(data_obs.feature) : undefined,
              geometry:  data_obs.geometry? angular.fromJson(data_obs.geometry) : undefined,
              chartData:  data_obs.chartData? angular.fromJson(data_obs.chartData) : undefined
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
      var str = e.popup && e.popup._gpdviz_str;
      //console.debug("popupopen: str=", str);
      if (str) {
        var charter = str._charter;
        //console.debug("popupopen: charter=", charter);
        if (charter) {
          charter.activate();

          if (charter.strid === 'chartDataTest') {
            charter._interval_code = setInterval(function() {
              var x = new Date().getTime();
              charter.addChartPoint(0, x, Math.round(Math.random() * 100));
            },1000);
          }
        }
      }
    });
    map.on('popupclose', function(e) {
      var str = e.popup && e.popup._gpdviz_str;
      //console.debug("popupclose: str=", str);
      if (str) {
        var charter = str._charter;
        //console.debug("popupclose: charter=", charter);
        if (charter) {
          charter.deactivate();

          if (charter.strid === 'chartDataTest') {
            clearInterval(charter._interval_code);
            delete charter._interval_code;
          }
        }
      }
    });

    function randomData(secs) {
      var time = new Date().getTime();
      var data = [];
      for (var i = -secs; i <= 0; i += 1) {
        data.push([
          time + i * 1000,
          Math.round(Math.random() * 100)
        ]);
      }
      return data;
    }

    function addObs(str, obs) {
      var style = str.style || {};
      var timestamp = obs.timestamp;

      if (obs.feature) {
        var geojson = angular.fromJson(obs.feature);
        var geometry = obs.feature.geometry;
        if (obs.feature.properties && obs.feature.properties.style) {
          style = _.assign(style, obs.feature.properties.style);
        }
      }
      else if (obs.geometry) {
        geojson = angular.fromJson(obs.geometry);
        geometry = obs.geometry;
      }
      else if (obs.chartData) {
        //console.debug("obs.chartData=", obs.chartData);
        if (!str._charter) {
          var names = _.map(obs.chartData, function(v, index) {
            return "char data #" + index;
          });
          str._charter = Charter(str.strid, names);
        }
        _.each(obs.chartData, function(v, index) {
          str._charter.addChartPoint(index, timestamp, v);
        });
        return;
      }
      else {
        console.error("expecting observation with feature, geometry, or chartData");
        return;
      }

      console.debug("addObs: style=", style, "str=", str, "geojson=", geojson);

      //var popupInfo = L.popup({autoClose: false, closeOnClick: false});
      var popupInfo = L.popup();
      popupInfo._gpdviz_str = str;
      popupInfo.setContent('<div id="' +"chart-container-" + str.strid+
        '" style="min-width:300px;height:250px;margin:0 auto"></div>');

      if (str.strid === 'chartDataTest') {
        popupInfo._gpdviz_str._charter = Charter(str.strid, ["chartDataTest"]);
        _.each(randomData(30), function(xy) {
          popupInfo._gpdviz_str._charter.addChartPoint(0, xy[0], xy[1]);
        });
      }

      addMarker(str.strid, function () {
        return L.geoJSON(geojson, {
          style: style,
          pointToLayer: function (feature, latlng) {
            if (!style.radius) {
              style.radius = 5;
            }
            return L.circleMarker(latlng, style);
          }
        });
      }, popupInfo);
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
            }
          }
        },

        series: initialSeriesData || [],

        rangeSelector: {
          buttons: [{
            count: 1,
            type: 'minute',
            text: '1M'
          }, {
            count: 5,
            type: 'minute',
            text: '5M'
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
          enabled: false
        },
        scrollbar: {
          enabled: true
        }
      });
    }
  }

})();
