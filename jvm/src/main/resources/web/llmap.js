function setupLLMap() {
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
    //byStrId[strid].marker = marker;
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

  return {
    addSelectionPoint: function(p) {
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
    },

    addGeoJson: function(strid, feature, style) {
      var geojson = JSON.parse(feature);
      var mapStyle = geojson && geojson.properties && geojson.properties.style ||
        style && JSON.parse(style);
      console.debug("addGeoJson: geojson=", geojson);
      console.debug("addGeoJson: mapStyle=", mapStyle);
      addMarker(strid, markerCreator(geojson, mapStyle));
    }
  }
}
