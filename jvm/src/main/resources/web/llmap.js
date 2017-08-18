function setupLLMap() {

  var debug = window && window.location.toString().match(/.*\?debug/);

  var byStrId = {};

  // TODO capture center and zoom from sensor system properties
  var center = [36.79, -122.02];
  var zoom = 11;

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
        if (!strTimePoss[strid]) return;
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

  (function popupEvents() {
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
      setTimeout(function() {
        //vm.hoveredPoint = {};
        addSelectionPoint();
      });
    });

    map.on('click', function (e) {
      // TODO
      // if (!vm.ss || !vm.ss.clickListener) return;
      //
      // var shiftKey = e.originalEvent.shiftKey;
      // var altKey   = e.originalEvent.altKey;
      // var metaKey  = e.originalEvent.metaKey;
      // // console.debug("clickListener=", vm.ss.clickListener, "MAP CLICK: e=", e
      // //   ,"latlng=", e.latlng
      // //   ,"shiftKey=", shiftKey
      // //   ,"altKey=",   altKey
      // //   ,"metaKey=",  metaKey
      // // );
      // if (shiftKey || altKey || metaKey) {
      //   var params = { lat: e.latlng.lat, lon: e.latlng.lng};
      //   if (shiftKey) params.shiftKey = true;
      //   if (altKey)   params.altKey = true;
      //   if (metaKey)  params.metaKey = true;
      //   $http({ method: "POST", url: vm.ss.clickListener, params: params}
      //   ).then(function (response) {
      //     // console.debug("url=", vm.ss.clickListener, "response=", response)
      //   }, function (error) {
      //     console.warn("url=", vm.ss.clickListener, "error=", error)
      //   })
      // }
    });
  })();

  function markerCreator(geojson, mapStyle) {
    //console.debug(":::::: geojson=", geojson);
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

  function addMarker(strid, createMarker) {
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
    selectionGroup.clearLayers();
    markersLayer.clearLayers();
    markersLayerMG.clearLayers();
    _.each(overlayGroupByStreamId, function(group) {
      controlLayers.removeLayer(group);
      map.removeLayer(group);
    });
    overlayGroupByStreamId = {};
  }

  function sensorSystemRegistered() {
    clearMarkers();
  }

  function sensorSystemUnregistered() {
    clearMarkers();
  }

  // TODO
  function addStream(str) {
    // json-ify some stuff
    str.mapStyle   = str.mapStyle   ? JSON.parse(str.mapStyle) : {};
    str.chartStyle = str.chartStyle ? JSON.parse(str.chartStyle) : {};
    if (str.variables) {
      str.variables = _.map(str.variables, function (variable) {
        if (variable.chartStyle) {
          variable.chartStyle = JSON.parse(variable.chartStyle);
        }
        return variable;
      });
    }
    console.debug("addStream: str=", _.cloneDeep(str));

    str.observations = {}; // TODO check already provided observation (not the case at the moment)
    byStrId[str.strid] = {
      str:      str,
      //charter:  createCharter(str),
      geoJsons: {}
    };
  }

  // TODO
  function removeStream(strid) {
    console.debug("TODO removeStream: strid=", strid)
  }

  function addGeoJson(strid, timestamp, geoJsonStr) {
    var str = byStrId[strid] && byStrId[strid].str;
    if (!str) {
      console.warn("addGeoJson: unknown stream by strid=", strid);
      return;
    }

    var geoJsonKey = timestamp + "->" + geoJsonStr;
    if (byStrId[strid].geoJsons[geoJsonKey]) {
      console.warn("addGeoJson: already added: strid=", strid, "geoJsonKey=", geoJsonKey);
      return;
    }

    var geoJson = JSON.parse(geoJsonStr);

    if (geoJson.properties && geoJson.properties.style) {
      var mapStyle = geoJson.properties.style;
    }
    else {
      mapStyle = str.mapStyle ? _.cloneDeep(str.mapStyle) : {};
    }

    console.debug("addGeoJson: timestamp=", timestamp, "geoJson=", geoJson, "mapStyle=", mapStyle);

    byStrId[strid].geoJsons[geoJsonKey] = geoJson;

    addMarker(strid, markerCreator(geoJson, mapStyle));
  }

  function addObsScalarData(strid, timestamp, scalarData) {
    var str = byStrId[strid] && byStrId[strid].str;
    if (!str) {
      console.warn("addObsScalarData: unknown stream by strid=", strid);
      return;
    }
    var charter = byStrId[strid].charter;
    if (!charter) {
      charter = byStrId[strid].charter = createCharter(str);
    }

    var indexes = _.map(scalarData.vars, function (varName) {
      return _.indexOf(_.map(str.variables, "name"), varName);
    });
    //console.debug("& indexes=", indexes);
    _.each(scalarData.vals, function (v, valIndex) {
      var varIndex = indexes[valIndex];
      charter.addChartPoint(varIndex, timestamp, v);
    });

    // console.debug(str.strid, "scalarData.position=", scalarData.position);
    if (scalarData.position) {
      positionsByTime.set(str.strid, timestamp, scalarData.position);
    }

    if (!byStrId[str.strid].marker) {
      return;
    }
    var chartId = "chart-container-" + str.strid;

    var useChartPopup = str.chartStyle && str.chartStyle.useChartPopup;

    if (useChartPopup) {
      if (byStrId[str.strid].popupInfo) return;
    }
    else if (byStrId[str.strid].absChartUsed) return;

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
      byStrId[str.strid].absChartUsed = true;
      // vm.absoluteCharts[chartId] = {
      //   id: chartId,
      //   heightStr: chartHeightStr,
      //   minWidthStr: minWidthStr
      // };

      // console.debug("ADDED absoluteChart=", vm.absoluteCharts[chartId]);
      byStrId[str.strid].marker.on('click', function (e) {
        var idElm = $("#" + chartId);
        //console.debug("CLICK: idElm=", idElm, " visible=", idElm && idElm.is(":visible"));
        idElm.stop();
        if (idElm.is(":visible")) {
          idElm.fadeOut(700);
          setTimeout(charter.deactivateChart, 700);
        }
        else {
          charter.activateChart();
          idElm.fadeIn('fast');
        }
      });

      $(document).keyup(function (e) {
        if (e.keyCode === 27) {
          var idElm = $("#" + chartId);
          //console.debug("ESC: idElm=", idElm);
          idElm.fadeOut(700);
          setTimeout(charter.deactivateChart, 700);
        }
      });
    }
  }

  function createCharter(str) {
    return Charter(str, function(point) {
      if (point) {
        console.debug("hovered point=", point.x);
        //$scope.$apply(function() {
        //  vm.hoveredPoint.isoTime = moment.utc(point.x).format();
          var p = positionsByTime.get(str.strid, point.x);
          if (p) {
            //vm.hoveredPoint.position = p;
            addSelectionPoint([p.lat, p.lon]);
          }
        //});
      }
    });
  }

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

  return {
    sensorSystemRegistered:    sensorSystemRegistered,
    sensorSystemUnregistered:  sensorSystemUnregistered,
    addStream:                 addStream,
    removeStream:              removeStream,
    addGeoJson:                addGeoJson,
    addObsScalarData:          addObsScalarData,
    addSelectionPoint:         addSelectionPoint
  };

  function getSizeStr(size) {
    return typeof size === 'number' ? size + 'px' : size;
  }
}
