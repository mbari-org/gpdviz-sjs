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
  _.each(variables, function(varProps) {
    //console.debug("varProps=", varProps);
    var varName = varProps.name;

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
    //console.debug(str.strid, "createChart: str.chartHeightPx=", str.chartHeightPx);
    // http://stackoverflow.com/q/23624448/830737
    return new Highcharts.StockChart({
      chart: {
        renderTo: "chart-container-" + strid,
        height: str.chartHeightPx - 4,
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
          count: 15,
          type: 'minute',
          text: '15m'
        }, {
          count: 1,
          type: 'hour',
          text: '1H'
        }, {
          count: 3,
          type: 'hour',
          text: '3H'
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
