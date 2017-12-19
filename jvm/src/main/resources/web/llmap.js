function setupLLMap(center, zoom, hoveredPoint, mouseOutside, clickHandler, includeGoogleMap) {

  var debug = window && window.location.toString().match(/.*\?debug/);

  var byStrId = {};

  var esriOceansLayer = L.esri.basemapLayer('Oceans');
  var osm = L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {
    attribution: '&copy; <a target="_blank" href="http://osm.org/copyright">OpenStreetMap</a> contributors'
  });

  var baseLayers = {
    'ESRI Oceans': esriOceansLayer,
    'OpenStreetMap': osm
    ,'Empty': L.tileLayer('', {opacity:0})
  };
  if (includeGoogleMap) {
    baseLayers['Google satellite'] = L.gridLayer.googleMutant({
      type: 'satellite' // valid values are 'roadmap', 'satellite', 'terrain' and 'hybrid'
    });
  }

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
      var shiftKey = e.originalEvent.shiftKey;
      var altKey   = e.originalEvent.altKey;
      var metaKey  = e.originalEvent.metaKey;

      if (debug) console.debug("MAP CLICK: e=", e
        ,"latlng=",   e.latlng
        ,"shiftKey=", shiftKey
        ,"altKey=",   altKey
        ,"metaKey=",  metaKey
      );

      clickHandler({
        lat:       e.latlng.lat,
        lon:       e.latlng.lng,
        shiftKey:  shiftKey,
        altKey:    altKey,
        metaKey:   metaKey
      });
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

  function sensorSystemAdded(center, zoom) {
    clearMarkers();
    setView(center, zoom);
  }

  function sensorSystemDeleted() {
    clearMarkers();
  }

  function addDataStream(str) {
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
    //console.debug("addDataStream: str=", _.cloneDeep(str));

    // initialize observations to empty (stream addition not expected to include any)
    str.observations = {};
    byStrId[str.strid] = {
      str:      str,
      geoJsons: {}
    };
  }

  // TODO
  function deleteDataStream(strid) {
    console.debug("TODO deleteDataStream: strid=", strid)
  }

  function addVariableDef(strid, vd) {
    var str = byStrId[strid] && byStrId[strid].str;
    if (!str) {
      console.warn("addVariableDef: unknown stream by strid=", strid);
      return;
    }

    var variable = {
      name:       vd.name,
      units:      vd.units,
      chartStyle: JSON.parse(vd.chartStyle)
    };

    if (!str.variables) {
      str.variables = [];
    }
    str.variables.push(variable);
    console.debug("addVariableDef: variable=", _.cloneDeep(variable));
  }

  function addGeoJson(strid, timeMs, geoJsonStr) {
    var str = byStrId[strid] && byStrId[strid].str;
    if (!str) {
      console.warn("addGeoJson: unknown stream by strid=", strid);
      return;
    }

    var geoJsonKey = timeMs + "->" + geoJsonStr;
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

    //console.debug("addGeoJson: timeMs=", timeMs, "geoJson=", geoJson, "mapStyle=", mapStyle);

    byStrId[strid].geoJsons[geoJsonKey] = geoJson;

    addMarker(strid, markerCreator(geoJson, mapStyle));
  }

  function addObsScalarData(strid, timeMs, scalarData) {
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
      charter.addChartPoint(varIndex, timeMs, v);
    });

    if (!byStrId[str.strid].marker) {
      return;
    }
    var chartId = "chart-container-" + str.strid;

    var useChartPopup = str.chartStyle && str.chartStyle.useChartPopup;

    if (useChartPopup) {
      if (byStrId[str.strid].popupInfo) return;
    }
    else if (byStrId[str.strid].absChartUsed) return;

    if (str.chartHeightPx === undefined) {
      str.chartHeightPx = 370;
      if (str.chartStyle && str.chartStyle.height) {
        str.chartHeightPx = str.chartStyle.height;
      }
      //console.debug(str.strid, "str.chartHeightPx=", str.chartHeightPx);
    }

    if (useChartPopup) {
      if (debug) console.debug("setting popup for stream ", str.strid);

      var chartHeightStr = getSizeStr(str.chartHeightPx);
      var minWidthPx = str.chartStyle && str.chartStyle.minWidthPx || 500;
      var minWidthStr = minWidthPx + 'px';

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
      if (point && point.x) {
        var isoTime = moment.utc(point.x).format();
        //console.debug("hovered point=", point, isoTime);
        hoveredPoint({
          strid:   str.strid,
          x:       point.x,
          y:       point.y,
          isoTime: isoTime
        });
      }
    }, mouseOutside);
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

  function setView(center, zoom) {
    map.setView([center[0], center[1]], zoom);
  }

  function setZoom(zoom) {
    map.setZoom(zoom);
  }

  prepareAdjustMapUponWindowResize();

  return {
    sensorSystemAdded:         sensorSystemAdded,
    sensorSystemDeleted:       sensorSystemDeleted,
    addDataStream:             addDataStream,
    deleteDataStream:          deleteDataStream,
    addVariableDef:            addVariableDef,
    addGeoJson:                addGeoJson,
    addObsScalarData:          addObsScalarData,
    addSelectionPoint:         addSelectionPoint,
    setView:                   setView
  };

  function getSizeStr(size) {
    return typeof size === 'number' ? size + 'px' : size;
  }

  function prepareAdjustMapUponWindowResize() {
    var minHeight = 350;
    var mapContainer = document.getElementById('mapid');
    var marginBottom = 5;

    function updateWindowSize() {
      var rect = mapContainer.getBoundingClientRect();
      // console.log("getBoundingClientRect=", rect.top, rect.right, rect.bottom, rect.left);
      // console.log("window.innerHeight=", window.innerHeight, " - rect.top=", rect.top);

      var restWindowHeight = window.innerHeight - rect.top - marginBottom;
      //console.log("restWindowHeight=", restWindowHeight);

      if (restWindowHeight < minHeight) {
        restWindowHeight = minHeight;
      }
      updateMapSize(restWindowHeight);
    }

    function updateMapSize(windowHeight) {
      if (windowHeight) {
        $('#mapid').css("height", windowHeight);
      }
      L.Util.requestAnimFrame(function () {
        map.invalidateSize({debounceMoveend: true}); }, map);
    }

    setTimeout(function () {
      updateWindowSize();
      $(window).resize(function () {
        updateWindowSize();
      });
    }, 0);
  }
}
