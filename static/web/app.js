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

    vm.hoveredPoint = {};
    vm.absoluteCharts = {};

    var byStrId = {};

    // TODO capture center and zoom from sensor system properties
    var center = [36.62, -122.04];
    var zoom = 12;

    var esriOceansLayer = L.esri.basemapLayer('Oceans');
    var osm = L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {
      attribution: '&copy; <a target="_blank" href="http://osm.org/copyright">OpenStreetMap</a> contributors'
    });

    var baseLayers = {
      'ESRI Oceans': esriOceansLayer,
      'OpenStreetMap': osm
      ,'Empty': L.tileLayer('', {opacity:0})
      // ,'Google satellite': L.gridLayer.googleMutant({
      //   type: 'satellite' // valid values are 'roadmap', 'satellite', 'terrain' and 'hybrid'
      // })
    };

    var map = L.map('mapid', {
      maxZoom: 20,
      layers: [baseLayers['ESRI Oceans']]
    }).setView(center, zoom);

    L.control.mousePosition({position: 'topright', emptyString: ''}).addTo(map);

    var markersLayer = new L.LayerGroup();
    var markersLayerMG = new L.LayerGroup();
    markersLayer.addTo(map);

    var controlLayers = L.control.layers(baseLayers).addTo(map);

    var overlayGroupByStreamId = {};

    var positionsByTime = (function() {
      var strTimePoss = {};

      return {
        set: function (strid, timeMs, position) {
          if (strTimePoss[strid] === undefined) {
            strTimePoss[strid] = {list: [], sorted: true};
          }
          strTimePoss[strid].list.push({timeMs: timeMs, position:position});
          strTimePoss[strid].sorted = false;
        },

        get: function (strid, timeMs) {
          if (!strTimePoss[strid].sorted) {
            strTimePoss[strid].list = _.sortBy(strTimePoss[strid].list, "timeMs");
            strTimePoss[strid].sorted = true;
          }
          var list = strTimePoss[strid].list;
          var ii = 0, mid = 0, kk = list.length - 1;
          while (ii < kk) {
            mid = Math.floor((ii + kk) / 2);
            var mid_timeMs = list[mid].timeMs;
            if (timeMs < mid_timeMs) {
              kk = mid;
            }
            else if (timeMs > mid_timeMs) {
              ii = mid + 1
            }
            else break;
          }
          return list[mid].position;
        }
      }
    })();

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

    var selectionIcon = new L.Icon({
      iconUrl: 'https://cdn.rawgit.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png',
      shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
      iconSize: [25, 41],
      iconAnchor: [12, 41],
      popupAnchor: [1, -34],
      shadowSize: [41, 41]
    });
    function addSelectionPoint(p) {
      // console.debug("addSelectionPoint: p=", p);
      selectionGroup.clearLayers();
      if (!p) return;

      var marker = L.marker([p[0], p[1]], {
        keyboard: false,
        icon: selectionIcon,
        riseOnHover: true,
        opacity: 0.9
      }).addTo(map);
      selectionGroup.addLayer(marker);
    }

    vm.ss = Gpdviz.initialSs;

    if (vm.ss) {
      if (vm.ss.center && vm.ss.center.lat && vm.ss.center.lon) {
        map.setView([vm.ss.center.lat, vm.ss.center.lon], 12)
      }

      var streams = _.sortBy(_.values(vm.ss.streams), 'zOrder');
      _.each(streams, function (str) {
        byStrId[str.strid] = {str: str, charter: createCharter(str)};

        // console.debug("stream", str.strid, "has", _.size(str.observations), "observations");

        // TODO improve the following.  Little hack: add non-scalarData observations first...
        _.each(str.observations, function (obss, timestamp) {
          _.each(obss, function(obs) {
            if (!obs.scalarData) {
              addObservation(str, timestamp, obs);
            }
          });
        });

        str.numberObs = 0;

        // ... so the marker has already been associated to the relevant streams:
        _.each(str.observations, function (obss, timestamp) {
          // TODO refine the following for numberObs, latestIso
          str.latestIso = moment.utc(+timestamp).format();

          _.each(obss, function(obs) {
            str.numberObs += 1;
            if (obs.scalarData) {
              addObservation(str, timestamp, obs);
            }
          });
        });
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

      else if (what === 'sensorSystemUnregistered') {
        $scope.$apply(function () {
          vm.ss = undefined;
        });
        clearMarkers();
      }

      else if (!vm.ss) {
        console.warn("handleNotification: vm.ss is undefined! Ignoring:", what);
      }

      else if (what === 'streamAdded') {
        str = angular.fromJson(data.str);
        str.observations = {};
        $scope.$apply(function () {
          vm.ss.streams[str.strid] = str;
          byStrId[str.strid] = {str: str, charter: createCharter(str)};
        });
      }

      else if (what === 'observations2Added') {
        str = vm.ss.streams[data.strid];
        //if (debug)
        //console.debug("observations2Added: str=", str, "data.strid=", data.strid);
        // console.debug("observations2Added: data.obss=", _.cloneDeep(data.obss));
        str.observations = str.observations || {};
        $scope.$apply(function () {
          _.each(data.obss, function (obsData, timestamp) {
            var obs = {};
            if (obsData.feature)  obs.feature  = angular.fromJson(obsData.feature);
            if (obsData.geometry) obs.geometry = angular.fromJson(obsData.geometry);
            if (obsData.scalarData) obs.scalarData  = angular.fromJson(obsData.scalarData);
            //console.debug("&& timestamp=", +timestamp, moment.utc(+timestamp).format(), "obs=", obs);
            str.observations[+timestamp] = obs;
            str.numberObs += 1;
            str.latestIso = moment.utc(+timestamp).format();
            addObservation(str, +timestamp, obs);
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

      else {
        console.error("Unexpected notification: payload=", payload)
      }
    }

    map.on('popupopen', function(e) {
      var strid = e.popup && e.popup._strid;
      var str = strid && byStrId[strid].str;
      var charter = str && byStrId[strid].charter;
      // console.debug("popupopen: str=", str, "has-charter=", !!charter);
      if (charter) {
        charter.activateChart();
      }
    });
    map.on('popupclose', function(e) {
      var strid = e.popup && e.popup._strid;
      var str = strid && byStrId[strid].str;
      //console.debug("popupclose: str=", str);
      if (str && byStrId[strid].charter) {
        var charter = byStrId[str.strid].charter;
        //console.debug("popupclose: charter=", charter);
        charter.deactivateChart();
      }
      $scope.$apply(function() {
        vm.hoveredPoint = {};
        addSelectionPoint();
      });
    });

    function createCharter(str) {
      return Charter(str, function(point) {
        if (point) {
          //console.debug("hovered point=", point.x, vm.hoveredPoint);
          $scope.$apply(function() {
            vm.hoveredPoint.isoTime = moment.utc(point.x).format();
            var p = positionsByTime.get(str.strid, point.x);
            if (p) {
              vm.hoveredPoint.position = p;
              addSelectionPoint([p.lat, p.lon]);
            }
          });
        }
      });
    }

    function addObservation(str, timestamp, obs) {
      if (obs.scalarData) {
        addObsScalarData(str, timestamp, obs);
      }
      else {
        addObsFeatureOrGeometry(str, timestamp, obs);
      }
    }

    function addObsScalarData(str, timestamp, obs) {
      //console.debug("addObsScalarData: str=", str, "obs.scalarData=", obs.scalarData);
      var charter = byStrId[str.strid].charter;
      if (!charter) {
        charter = byStrId[str.strid].charter = createCharter(str);
      }

      var indexes = _.map(obs.scalarData.vars, function (varName) {
        return _.indexOf(_.keys(str.variables), varName);
      });
      //console.debug("& indexes=", indexes);
      _.each(obs.scalarData.vals, function (v, valIndex) {
        var varIndex = indexes[valIndex];
        charter.addChartPoint(varIndex, timestamp, v);

      });

      // console.debug(str.strid, "obs.scalarData.position=", obs.scalarData.position);
      if (obs.scalarData.position) {
        positionsByTime.set(str.strid, timestamp, obs.scalarData.position);
      }

      if (!byStrId[str.strid].marker) {
        return;
      }

      var chartId = "chart-container-" + str.strid;

      var useChartPopup = false;//str.chartStyle && str.chartStyle.useChartPopup;

      if (useChartPopup) {
        if (byStrId[str.strid].popupInfo) return;
      }
      else if (vm.absoluteCharts[chartId]) return;

      if (str.chartHeight === undefined) {
        str.chartHeight = 400;
        if (str.chartStyle && str.chartStyle.height) {
          str.chartHeight = str.chartStyle.height;
        }
      }
      var chartHeightStr = getSizeStr(str.chartHeight);
      var minWidthPx = str.chartStyle && str.chartStyle.minWidthPx || 600;
      var minWidthStr = minWidthPx + 'px';

      if (useChartPopup) {
        if (debug) console.debug("setting popup for stream ", str.strid);
        var chartContainer = '<div id="' + chartId +
          '" style="min-width:' + minWidthStr + ';height:' + chartHeightStr + ';margin:0 auto"></div>';

        var popupInfo = L.popup({
          //autoClose: false, closeOnClick: false
          minWidth: minWidthPx + 50
        });
        popupInfo._strid = str.strid;

        popupInfo.setContent(chartContainer);

        byStrId[str.strid].marker.bindPopup(popupInfo);
        byStrId[str.strid].popupInfo = popupInfo;
      }

      else {
        vm.absoluteCharts[chartId] = {
          id: chartId,
          heightStr: chartHeightStr,
          minWidthStr: minWidthStr
        };
        // console.debug("ADDED absoluteChart=", vm.absoluteCharts[chartId]);
        byStrId[str.strid].marker.on('click', function (e) {
          var idElm = $("#" + chartId);
          idElm.stop();
          if (idElm.is(":visible")) {
            idElm.fadeOut(700);
            $timeout(charter.deactivateChart, 700);
          }
          else {
            charter.activateChart();
            idElm.fadeIn('fast');
          }
        });
        $(document).keyup(function (e) {
          if (e.keyCode === 27) {
            var idElm = $("#" + chartId);
            idElm.fadeOut(700);
            $timeout(charter.deactivateChart, 700);
          }
        });
      }
    }

    function addObsFeatureOrGeometry(str, timestamp, obs) {
      function markerCreator(geojson, mapStyle) {
        return function() {
          return L.geoJSON(geojson, {
            style: mapStyle,
            pointToLayer: function (feature, latlng) {
              if (!mapStyle.radius) {
                mapStyle.radius = 5;
              }
              return L.circleMarker(latlng, mapStyle);
            }
          });
        }
      }

      if (obs.feature) {
        var mapStyle = str.mapStyle ? _.cloneDeep(str.mapStyle) : {};
        // console.debug("addObsFeatureOrGeometry: str=", str, "obs.feature=", obs.feature);
        var geojson = angular.fromJson(obs.feature);
        if (obs.feature.properties && obs.feature.properties.style) {
          mapStyle = _.assign(mapStyle, obs.feature.properties.style);
        }
        addMarker(str, markerCreator(geojson, mapStyle));
      }

      if (obs.geometry) {
        // console.debug("addObsFeatureOrGeometry: obs.geometry=", obs.geometry);
        mapStyle = str.mapStyle ? _.cloneDeep(str.mapStyle) : {};
        // console.debug("addObsFeatureOrGeometry: str=", str, "obs.geometry=", obs.geometry);
        geojson = angular.fromJson(obs.geometry);
        addMarker(str, markerCreator(geojson, mapStyle));
      }

      if (debug) console.debug("addObsFeatureOrGeometry: mapStyle=", mapStyle, "str=", str, "geojson=", geojson);
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

  function Charter(str, hoveredPoint) {
    var strid = str.strid;
    var variables = str.variables;

    // FIXME should be indicated which series to use for map location
    var seriesIndexTemperature = undefined;

    var title = (function() {
      var t = str.chartStyle && str.chartStyle.title || (str.strid + (str.name ? ' - ' + str.name : ''));
      return '<span style="font-size: small">' + t + '</span>';
    })();
    var subtitle = str.chartStyle && str.chartStyle.subtitle;

    var yAxisList = str.chartStyle && str.chartStyle.yAxis;

    var initialSeriesData = [];
    _.each(variables, function(varProps, varName) {
      // console.debug("varName=", varName, "varProps=", varProps);

      if (varName === 'temperature') {
        seriesIndexTemperature = initialSeriesData.length;
      }
      var chartStyle = varProps.chartStyle || {};

      var options = {
        yAxis: chartStyle.yAxis,
        name: varName + (varProps.units ? ' (' +varProps.units+ ')' : ''),
        data: []
        ,marker: {
          enabled: true,
          radius: 2
        }
        ,lineWidth: chartStyle.lineWidth || 1
        ,type: chartStyle.type
        ,dataGrouping: chartStyle.dataGrouping || { enabled: true, approximation: 'open'}

        //,states: {
        //  hover: {
        //    lineWidthPlus: 0
        //  }
        //}
      };
      // console.debug("varName=", varName, "options=", _.cloneDeep(options));

      initialSeriesData.push(options);
    });

    var chart = undefined;
    var serieses = undefined;

    var lastAddedX = {};  // {seriesIndex -> x}

    var needRedraw = true;

    setInterval(function() {
      if (chart && needRedraw) {
        // console.debug("redrawing chart ", strid);
        chart.redraw();
        needRedraw = false;
      }
    }, 2000);

    return {
      strid: strid,
      addChartPoint: addChartPoint,
      activateChart: activateChart,
      deactivateChart: deactivateChart
    };

    function addChartPoint(seriesIndex, x, y) {
      //console.debug("addChartPoint: strid=", strid, "x=", x, "y=", y);
      x = +x;

      needRedraw = true;

      initialSeriesData[seriesIndex].data.push([x, y]);

      if (serieses) {
        var lastX = lastAddedX[seriesIndex];
        if (lastX === undefined) {
          lastAddedX[seriesIndex] = x;
          // console.error("strid=" +strid+ " seriesIndex=" +seriesIndex+": FIRST x(" +x+ ")");
        }
        else if (x <= lastX) {
          console.error("strid=" +strid+ " seriesIndex=" +seriesIndex+": x(" +x+ ") <= lastX(" +lastX+ ") diff=" + (lastX - x));
        }
        else {
          lastAddedX[seriesIndex] = x;
        }

        // addPoint (Object options, [Boolean redraw], [Boolean shift], [Mixed animation])
        serieses[seriesIndex].addPoint([x, y], false);
      }
      // else console.error("!!! no serieses !!!!!");
    }

    function activateChart() {
      deactivateChart();
      needRedraw = true;
      _.each(initialSeriesData, function(s) {
        s.data = _.sortBy(s.data, function(xy) { return xy[0] });
      });
      chart = createChart();

      if (hoveredPoint) {
        $(chart.container).on('mousemove', _.throttle(mousemove, 250) )
      }

      function mousemove(e) {
        if (seriesIndexTemperature !== undefined && chart) {
          var event = chart.pointer.normalize(e.originalEvent);
          var point = chart.series[seriesIndexTemperature].searchPoint(event, true);
          // console.debug("strid=", strid, "normalizedEvent=", event, "point=", point);
          hoveredPoint(point);
        }
      }
    }

    function deactivateChart() {
      needRedraw = false;
      lastAddedX = {};
      if (chart) chart.destroy();
      chart = undefined;
      serieses = undefined;
      if (hoveredPoint && chart && chart.container) {
        $(chart.container).off('mousemove', mousemove);
      }
    }

    function createChart() {
      // http://stackoverflow.com/q/23624448/830737
      return new Highcharts.StockChart({
        chart: {
          renderTo: "chart-container-" + strid,
          height: str.chartHeight - 4,
          events: {
            load: function () {
              serieses = this.series;
            },
            zoomType: 'x'
          }
        },
        xAxis: {
          type: 'datetime',
          ordinal: false
        },
        legend: { enabled: false },

        series: initialSeriesData,

        yAxis: yAxisList,

        tooltip: {
          // pointFormat: '<span style="color:{series.color}">{series.name}</span>: <b>{point.y}</b><br/>',
          valueDecimals: 4
          // ,shared: true

          // why is not the <table> working?
          // ,useHTML: true
          // ,headerFormat: '<small>{point.key}</small><table>'
          // ,pointFormat: '<tr><td style="color: {series.color}">{series.name}:</td>' +
          //   '<td style="text-align: right"><b>{point.y}</b></td></tr>'
          // ,footerFormat: '</table>'
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
            count: 6,
            type: 'hour',
            text: '6H'
          }, {
            count: 12,
            type: 'hour',
            text: '12H'
          }, {
            count: 1,
            type: 'day',
            text: '1D'
          }, {
            count: 7,
            type: 'day',
            text: '1W'
          }, {
            type: 'all',
            text: 'All'
          }],
          inputEnabled: false,
          selected: 1
        },

        title: { text: title },
        subtitle: { text: subtitle },

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

  function getSizeStr(size) {
    return typeof size === 'number' ? size + 'px' : size;
  }

})();
