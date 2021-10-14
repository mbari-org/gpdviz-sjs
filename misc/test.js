/**
 * Created by carueda on 4/25/17.
 */
// Monterey Bay Aquarium Research Institute
// Author: Jeff Sevadjian
// jsevadjian@mbari.org

jQuery(document).ready(function($) {

  function mouseEv(e) {
    var chart, point, i, event, ind;

    for (i = 0; i < Highcharts.charts.length; i = i + 1) {
      chart = Highcharts.charts[i];
      event = chart.pointer.normalize(e.originalEvent); // Find coordinates within the chart

      var str = String(chart.renderTo.id);
      if (str.includes("humidity")) {
        ind = 2;
      } else {
        ind = 0;
      }

      point = chart.series[ind].searchPoint(event, true); // Get the hovered point

      if (point) {
        if (chart['chartHeight'] != 100 && chart.xAxis[0].min > Math.pow(10,12)) { //skip blank, scatter
          point.highlight(e);
        }
      }
    }
  }

  $('#container_Position').bind('mousemove touchmove touchstart', mouseEv);
  $('#container_CTD').bind('mousemove touchmove touchstart', mouseEv);
  $('#container_CO2').bind('mousemove touchmove touchstart', mouseEv);
  $('#container_EcoPuck').bind('mousemove touchmove touchstart', mouseEv);
  $('#container_METwind').bind('mousemove touchmove touchstart', mouseEv);
  $('#container_MET').bind('mousemove touchmove touchstart', mouseEv);
  $('#container_tagID').bind('mousemove touchmove touchstart', mouseEv);
  $('#container_CO2_Equil').bind('mousemove touchmove touchstart', mouseEv);
  $('#container_CO2_Zero').bind('mousemove touchmove touchstart', mouseEv);
  $('#container_CO2_Air').bind('mousemove touchmove touchstart', mouseEv);
  $('#container_CO2_Stnd').bind('mousemove touchmove touchstart', mouseEv);
  $('#container_CO2humidity').bind('mousemove touchmove touchstart', mouseEv);
  $('#container_Power').bind('mousemove touchmove touchstart', mouseEv);

  //Override the reset function, we don't need to hide the tooltips and crosshairs.
  Highcharts.Pointer.prototype.reset = function() {
    return undefined;
  };

  //Highlight a point by showing tooltip, setting hover state and drawing crosshair
  Highcharts.Point.prototype.highlight = function(event) {
    this.onMouseOver(); // Show the hover marker
    // this.series.chart.tooltip.refresh(this); // Show the tooltip
    this.series.chart.xAxis[0].drawCrosshair(event, this); // Show the crosshair
  };

  //Get current time, will use to set x-max
  var jsTime = new Date();
  var currTime = jsTime.getTime();

  //Function to link x-axes on zoom
  function syncExtremes(e) {
    var xMin = e.min;
    var xMax = e.max;
    if (e.trigger !== 'syncExtremes') {
      Highcharts.each(Highcharts.charts, function (chart) {
        if (chart.xAxis[0].min > Math.pow(10,12)) { // update time-series plots ONLY
          if (chart.xAxis[0].setExtremes) {
            if (e.trigger == "rangeSelectorButton") {
              if (e.rangeSelectorButton.type == 'week') {
                chart.xAxis[0].setExtremes(currTime - 7*86400*1000, currTime, 1, 0, {trigger: 'syncExtremes'});
              }
              else if (e.rangeSelectorButton.type == 'day') {
                chart.xAxis[0].setExtremes(currTime - 1*86400*1000, currTime, 1, 0, {trigger: 'syncExtremes'});
              }
              else if (e.rangeSelectorButton.type == 'all') {
                chart.xAxis[0].setExtremes(xMin, currTime, 1, 0, {trigger: 'syncExtremes'});
              }
            } else if (e.trigger == "navigator" || e.trigger == "zoom") {
              chart.xAxis[0].setExtremes(xMin, xMax, 1, 0, {trigger: 'syncExtremes'});
            }
          }
        }
      });
    }
  }

  //Function to parse .csv files in js
  function parseCSV(csvFile) {
    var data = [];
    var lines = csvFile.split('\n');
    $.each(lines, function(lineNo, line) {
      if (lineNo != 0) {
        var items = line.split(',');
        data.push([parseFloat(items[0]), parseFloat(items[1])]);
      }
    });
    return data
  }

  // Define universal settings
  Highcharts.setOptions({

    lang: {
      rangeSelectorZoom: ''
    },
    credits: {
      enabled: false
    },
    exporting: {
      enabled: true
    },

    chart: {
      plotBackgroundColor: {
        linearGradient: {
          x1: 0.5,
          y1: 0,
          x2: 0.5,
          y2: 1
        },
        stops: [
          [0, 'rgb(255, 255, 255)'],
          [1, 'rgb(240, 240, 240)']
        ]
      },
      plotBorderColor:  'rgb(150, 150, 150)',
      plotBorderWidth: 1,
      marginTop: 50,
      marginBottom: 35,
      marginLeft: 110,
      marginRight: 160,
      zoomType: 'x',
      panning: false,
      alignTicks: false
    },

    rangeSelector : {
      buttons: [{
        type: 'all',
        text: 'All'
      }, {
        type: 'week',
        count: 1,
        text: 'Wk'
      }, {
        type: 'day',
        count: 1,
        text: 'Day'
      }],
      buttonSpacing: 2,
      buttonTheme: {
        width: 25
      },
      selected: 2,
      inputEnabled: false,
      buttonPosition: {
        x: 105,
        y: 60
      }
    },

    scrollbar: {
      enabled: false
    },

    title: {
      align: 'left',
      x: 100,
      y: 35,
      style: {
        "font-size": "150%"
      }
    },

    xAxis: {
      ordinal: false,
      opposite: false,
      lineWidth: 0,
      gridLineWidth: 1,
      gridLineDashStyle: 'ShortDot',
      labels: {
        style: {
          "font-size": "100%"
        }
      },
      dateTimeLabelFormats: {
        hour: '%H:%M',
        day: '%H:%M<br><b>%m/%d/%y</b>',
        week: '<b>%m/%d/%y</b>',
        month: '<b>%m/%d/%y</b>'
      },
      title: {
        style: {
          "font-size": "120%",
          "font-weight": "normal",
          "color": "#333333"
        }
      },
      labels: {
        distance: 50,
        style: {
          "font-size": "100%"
        }
      },
      tickPixelInterval: 150
    },

    yAxis: {
      showLastLabel: true,
      ordinal: false,
      opposite: false,
      startOnTick: false,
      endOnTick: false,
      gridLineWidth: 0,
      lineWidth: 1,
      tickWidth: 1,
      labels: {
        y: 5,
        style: {
          "font-size": "100%"
        }
      },
      title: {
        style: {
          "font-size": "120%",
          "font-weight": "normal",
          "color": "#333333"
        }
      }
    },

    tooltip: {
      animation: false,
      shadow: false,
      shared: true,
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      padding: 0,
      borderWidth: 0,
      dateTimeLabelFormats: {
        second: '%m/%d/%Y %H:%M',
        minute: '%m/%d/%Y %H:%M'
      },
      useHTML: true,
      positioner: function (labelWidth, labelHeight, point) {
        var chart = this.chart;
        return { x: chart.plotLeft + chart.plotWidth - labelWidth, y: chart.plotTop - labelHeight - 3 }
      }
    },

    plotOptions: {
      series: {
        states: {
          hover: {
            lineWidthPlus: 0
          }
        },
        dataGrouping: {
          enabled: true,
          groupPixelWidth: 3,
          units: [[
            'minute', [10, 30]
          ],[
            'hour', [1,2,3,6,12]
          ],[
            'day', [1]
          ]],
          dateTimeLabelFormats: {
            minute: ['%m/%d/%y %H:%M', '%m/%d/%y %H:%M', '-%H:%M'],
            hour: ['%m/%d/%y %H:%M', '%m/%d/%y %H:%M', '-%H:%M'],
            day: ['%m/%d/%y', '%m/%d', '-%m/%d/%y']
          }
        },
        gapSize: 0
      }
    }

  });

  $.ajaxSetup({ cache: false });

  //-------------------------------------------------//

  // Empty chart with time slider
  $.get('csv/Blank.csv', function (data) {
    $('#container_Blank').highcharts('StockChart', {
      chart: {
        height: 100,
        borderWidth: 0,
        plotBorderWidth: 0,
        spacingBottom: 20,
        marginBottom: 80,
        marginTop: 5
      },
      xAxis: {
        events: {
          afterSetExtremes: syncExtremes
        },
        visible: false,
        lineWidth: 0,
        labels: {
          enabled: false
        },
        min: currTime - 86400*1000,
        max: currTime
      },
      yAxis: {
        visible: false
      },
      data: {
        csv: data
      },
      exporting: {
        enabled: false
      },
      rangeSelector: {
        enabled: false
      },
      series: {
        marker: {
          enabled: false
        },
        enableMouseTracking: false
      },

      navigator: {
        height: 25,
        enabled: true,
        marginBottom: 0,
        xAxis: {
          lineWidth: 1,
          lineColor: 'rgb(200, 200, 200)',
          labels: {
            align: 'center',
            y: 10
          },
          title: {
            y: -70,
            text: 'Time Selection',
            style: {
              "font-size": "125%",
              "font-weight": "normal",
              "color": "#333333"
            }
          },
          ordinal: false,
          opposite: false,
          max: currTime,
          dateTimeLabelFormats: {
            hour: '<b>%m/%d/%y</b>',
            day: '<b>%m/%d/%y</b>',
            week: '<b>%m/%d/%y</b>',
            month: '<b>%m/%d/%y</b>'
          }
        },
        yAxis: {
          lineColor: 'rgb(200, 200, 200)'
        },
        series: {
          type: "line",
          lineWidth: 0,
          marker: {
            enabled : true,
            radius : 1,
            symbol: "circle",
          }
        }
      },

      tooltip: {
        enabled: false
      }
    })

  })

  //-------------------------------------------------//

  // Chart for Position [longitude,latitude]

  $.get('csv/Position.csv', function (data) {

    $('#container_Position').highcharts('StockChart', {

      title: {
        text: 'Position'
      },
      xAxis: {
        events: {
          afterSetExtremes: syncExtremes
        },
        min: currTime - 86400*1000,
        max: currTime
      },
      yAxis: [{
        // longitude
        labels: {
          y: 5,
          style: {
            "color":"rgb(100, 100, 100)"
          }
        },
        title: {
          text: 'Longitude [Â°E]',
          style: {
            "color":"rgb(100, 100, 100)"
          }
        },
        lineColor: 'rgb(100, 100, 100)',
        tickColor: 'rgb(100, 100, 100)'
      },{
        // Latitude
        opposite: true,
        labels: {
          y: 5,
          style: {
            "color":"rgb(11, 99, 160)"
          }
        },
        title: {
          text: 'Latitude [Â°N]',
          style: {
            "color":"rgb(11, 99, 160)"
          }
        },
        lineColor: 'rgb(11, 99, 160)',
        tickColor: 'rgb(11, 99, 160)'
      }],

      data : {
        csv : data
      },

      series: [{
        // longitude
        index: 1,
        color: 'rgba(100, 100, 100, 0.9)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgb(100, 100, 100)'
        },
        tooltip: {
          pointFormat: '<div style="float:right; height:20px">' +
          '<div style="float:left"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float:left">Lon&nbsp;</div>' +
          '<div style="float:right; text-align:right; width:75px"><b>{point.y}</b></div>' +
          '</div>'
        }
      },{
        // latitude
        index: 0,
        color: 'rgba(11, 99, 160, 0.9)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgb(11, 99, 160)'
        },
        tooltip: {
          pointFormat: '<div style="float:right; height:20px">' +
          '<div style="float:left"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float:left">Lat&nbsp;</div>' +
          '<div style="float:right; text-align:right; width:60px"><b>{point.y}</b></div>' +
          '</div>'
        },
        yAxis: 1
      }],

      navigator : {
        enabled : false
      },

      tooltip: {
        valueDecimals: 5,
        headerFormat:'<p style="text-align:right; margin-bottom:0px; margin-top:0px">{point.key}</p>' +
        '<div style="overflow:auto; width:235px">',
        footerFormat: '</div>'
      }

    }); //end Position container

  }); //end $.get fn for Position

  //-------------------------------------------------//

  // Chart for CTD [temperature,salinity,oxygen]

  $.get('csv/CTD.csv', function (data) {

    $('#container_CTD').highcharts('StockChart', {

      title: {
        text: 'CTD'
      },
      xAxis: {
        events: {
          afterSetExtremes: syncExtremes
        },
        min: currTime - 86400*1000,
        max: currTime
      },
      yAxis: [{
        // temperature
        labels: {
          y: 5,
          style: {
            "color":"rgb(11, 99, 160)"
          }
        },
        title: {
          text: 'Temperature [Â°C]',
          style: {
            "color":"rgb(11, 99, 160)"
          }
        },
        lineColor: 'rgb(11, 99, 160)',
        tickColor: 'rgb(11, 99, 160)'
      },{
        // salinity
        opposite: true,
        labels: {
          y: 5,
          style: {
            "color":"rgb(255, 127, 14)"
          }
        },
        title: {
          text: 'Salinity',
          style: {
            "color":"rgb(255, 127, 14)"
          }
        },
        lineColor: 'rgb(255, 127, 14)',
        tickColor: 'rgb(255, 127, 14)'
      },{
        // oxygen
        opposite: true,
        labels: {
          y: 5,
          style: {
            "color":"rgb(44, 160, 44)"
          }
        },
        title: {
          text: 'Oxygen [Î¼mol/kg]',
          style: {
            "color":"rgb(44, 160, 44)"
          }
        },
        lineColor: 'rgb(44, 160, 44)',
        tickColor: 'rgb(44, 160, 44)'
      }],

      data : {
        csv : data
      },

      series: [{
        // temperature
        index: 2,
        color: 'rgba(11, 99, 160, 0.9)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgb(11, 99, 160)'
        },
        tooltip: {
          valueDecimals: 3,
          pointFormat: '<div style="float:right; height:20px">' +
          '<div style="float:left"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float:left">T&nbsp;</div>' +
          '<div style="float:right; text-align:right; width:45px"><b>{point.y}</b></div>' +
          '</div>'
        }
      },{
        // salinity
        index: 1,
        color: 'rgba(255, 127, 14, 0.5)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgb(255, 127, 14)'
        },
        tooltip: {
          valueDecimals: 3,
          pointFormat: '<div style="float:right; height:20px">' +
          '<div style="float:left"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float:left">S&nbsp;</div>' +
          '<div style="float:right; text-align:right; width:45px"><b>{point.y}</b></div>' +
          '</div>'
        },
        yAxis: 1
      },{
        // oxygen
        index: 0,
        color: 'rgba(44, 160, 44, 0.5)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgb(44, 160, 44)'
        },
        tooltip: {
          valueDecimals: 2,
          pointFormat: '<div style="float:right; height:20px">' +
          '<div style="float:left"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float:left">O<sub>2</sub>&nbsp;</div>' +
          '<div style="float:right; text-align:right; width:45px"><b>{point.y}</b></div>' +
          '</div>'
        },
        yAxis: 2
      }],

      navigator : {
        enabled : false
      },

      tooltip: {
        headerFormat:'<p style="text-align:right; margin-bottom:0px; margin-top:0px">{point.key}</p>' +
        '<div style="overflow:auto; width:275px">',
        footerFormat: '</div>'
      }

    }); //end CTD container

  }); //end $.get fn for CTD

  //-------------------------------------------------//

  // Chart for pCO2 [pH,pCO2water,pCO2air]

  $.get('csv/CO2.csv', function (data) {

    $('#container_CO2').highcharts('StockChart', {

      title: {
        useHTML: true,
        text: 'pCO<sub>2</sub>'
      },
      xAxis: {
        events: {
          afterSetExtremes: syncExtremes
        },
        min: currTime - 86400*1000,
        max: currTime
      },
      yAxis: [{
        // pH
        labels: {
          y: 5,
          style: {
            "color":"rgb(50, 50, 50)"
          }
        },
        title: {
          text: 'pH [sea water scale]',
          style: {
            "color":"rgb(50, 50, 50)"
          }
        },
        lineColor: 'rgb(50, 50, 50)',
        tickColor: 'rgb(50, 50, 50)'
      },{
        // pCO2water
        opposite: true,
        labels: {
          y: 5,
          style: {
            "color":"rgb(11, 99, 160)"
          }
        },
        title: {
          text: 'pCO2 [ppm]',
          style: {
            "color":"rgb(11, 99, 160)"
          }
        },
        lineColor: 'rgb(11, 99, 160)',
        tickColor: 'rgb(11, 99, 160)'
      },{
        // pCO2air (same y-axis)
        lineWidth: 0,
        gridLineWidth: 0
      }],

      data : {
        csv : data
      },

      series: [{
        // pH
        color: 'rgba(50, 50, 50, 0.5)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgb(50, 50, 50)'
        },
        tooltip: {
          valueDecimals: 3,
          pointFormat: '<div style="float:right; height:20px">' +
          '<div style="float:left"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float:left">pH&nbsp;</div>' +
          '<div style="float:right; text-align:right; width:35px"><b>{point.y}</b></div>' +
          '</div>'
        }
      },{
        // pCO2water
        connectNulls: true,
        color: 'rgba(11, 99, 160, 0.9)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgb(11, 99, 160)'
        },
        tooltip: {
          valueDecimals: 2,
          pointFormat: '<div style="float:right; height:20px">' +
          '<div style="float:left"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float:left">Water&nbsp;</div>' +
          '<div style="float:right; text-align:right; width:40px"><b>{point.y}</b></div>' +
          '</div>'
        },
        yAxis: 1
      },{
        // pCO2air
        connectNulls: true,
        color: 'rgba(134, 159, 192, 0.9)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgb(134, 159, 192)'
        },
        tooltip: {
          valueDecimals: 2,
          pointFormat: '<div style="float:right; height:20px">' +
          '<div style="float:left"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float:left">Air&nbsp;</div>' +
          '<div style="float:right; text-align:right; width:45px"><b>{point.y}</b></div>' +
          '</div>'
        },
        yAxis: 1
      }],

      navigator : {
        enabled : false
      },

      tooltip: {
        headerFormat:'<p style="text-align:right; margin-bottom:0px; margin-top:0px">{point.key}</p>' +
        '<div style="overflow:auto; width:275px">',
        footerFormat: '</div>'
      }

    }); //end CO2 container

  }); //end $.get fn for CO2

  //-------------------------------------------------//

  // Chart for Eco-Puck [fluor,bb470,bb650]

  $.get('csv/EcoPuck.csv', function (data) {

    $('#container_EcoPuck').highcharts('StockChart', {

      title: {
        text: 'Eco-Puck'
      },
      xAxis: {
        events: {
          afterSetExtremes: syncExtremes
        },
        min: currTime - 86400*1000,
        max: currTime
      },
      yAxis: [{
        // fluor
        labels: {
          y: 5,
          style: {
            "color":"rgb(44, 160, 44)"
          }
        },
        title: {
          text: 'Fluorescence [Î¼g/l]',
          style: {
            "color":"rgb(44, 160, 44)"
          }
        },
        lineColor: 'rgb(44, 160, 44)',
        tickColor: 'rgb(44, 160, 44)'
      },{
        // bb470
        opposite: true,
        labels: {
          y: 5,
          style: {
            "color":"rgb(50, 50, 50)"
          }
        },
        title: {
          text: 'bb [1/m]',
          style: {
            "color":"rgb(50, 50, 50)"
          }
        },
        lineColor: 'rgb(50, 50, 50)',
        tickColor: 'rgb(50, 50, 50)'
      }],

      data : {
        csv : data
      },

      series: [{
        // fluor
        color: 'rgba(44, 160, 44, 0.5)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgb(44, 160, 44)'
        },
        tooltip: {
          valueDecimals: 3,
          pointFormat: '<div style="float:right; height:20px">' +
          '<div style="float:left"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float:left">Fluor&nbsp;</div>' +
          '<div style="float:right; text-align:right; width:40px"><b>{point.y}</b></div>' +
          '</div>'
        }
      },{
        // bb470
        color: 'rgba(20, 20, 20, 0.5)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgb(20, 20, 20)'
        },
        tooltip: {
          valueDecimals: 5,
          pointFormat: '<div style="float:right; height:20px">' +
          '<div style="float:left"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float:left">bb470&nbsp;</div>' +
          '<div style="float:right; text-align:right; width:60px"><b>{point.y}</b></div>' +
          '</div>'
        },
        yAxis: 1
      },{
        // bb650
        color: 'rgba(120, 120, 120, 0.5)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgb(120, 120, 120)'
        },
        tooltip: {
          valueDecimals: 5,
          pointFormat: '<div style="float:right; height:20px">' +
          '<div style="float:left"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float:left">bb650&nbsp;</div>' +
          '<div style="float:right; text-align:right; width:60px"><b>{point.y}</b></div>' +
          '</div>'
        },
        yAxis: 1
      }],

      navigator : {
        enabled : false
      },

      tooltip: {
        headerFormat:'<p style="text-align:right; margin-bottom:0px; margin-top:0px">{point.key}</p>' +
        '<div style="overflow:auto; width:350px">',
        footerFormat: '</div>'
      }

    }); //end Eco-Puck container

  }); //end $.get fn for Eco-Puck

  //-------------------------------------------------//

  // Chart for METwind [windSpd,windDir]

  $.get('csv/METwind.csv', function (data) {

    $('#container_METwind').highcharts('StockChart', {

      title: {
        text: 'Wind'
      },
      xAxis: {
        events: {
          afterSetExtremes: syncExtremes
        },
        min: currTime - 86400*1000,
        max: currTime
      },
      yAxis: [{
        // windDir
        min: 0,
        max: 360,
        tickInterval: 90,
        labels: {
          y: 5,
          style: {
            "color":"rgb(100, 100, 100)"
          }
        },
        title: {
          text: 'Wind Direction',
          style: {
            "color":"rgb(100, 100, 100)"
          }
        },
        lineColor: 'rgb(100, 100, 100)',
        tickColor: 'rgb(100, 100, 100)'
      },{
        // windSpd
        opposite: true,
        labels: {
          y: 5,
          style: {
            "color":"rgb(11, 99, 160)"
          }
        },
        title: {
          text: 'Wind Speed [m/s]',
          style: {
            "color":"rgb(11, 99, 160)"
          }
        },
        lineColor: 'rgb(11, 99, 160)',
        tickColor: 'rgb(11, 99, 160)'
      }],

      data : {
        csv : data
      },

      series: [{
        // windDir
        index: 1,
        color: 'rgba(100, 100, 100, 0.5)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgb(100, 100, 100)'
        },
        dataGrouping: {
          approximation: "open",
        },
        tooltip: {
          pointFormat: '<div style="float:right; height:20px">' +
          '<div style="float:left"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float:left">Dir&nbsp;</div>' +
          '<div style="float:right; text-align:right; width:40px"><b>{point.y}</b></div>' +
          '</div>'
        },
      },{
        // windSpd
        index: 0,
        color: 'rgba(11, 99, 160, 0.9)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgb(11, 99, 160)'
        },
        tooltip: {
          pointFormat: '<div style="float:right; height:20px">' +
          '<div style="float:left"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float:left">Speed&nbsp;</div>' +
          '<div style="float:right; text-align:right; width:30px"><b>{point.y}</b></div>' +
          '</div>'
        },
        yAxis: 1
      }],

      navigator : {
        enabled : false
      },

      tooltip: {
        valueDecimals: 1,
        headerFormat:'<p style="text-align:right; margin-bottom:0px; margin-top:0px">{point.key}</p>' +
        '<div style="overflow:auto; width:200px">',
        footerFormat: '</div>'
      }

    }); //end METwind container

  }); //end $.get fn for METwind

  //-------------------------------------------------//

  // Chart for MET [airPress,airTemp]

  $.get('csv/MET.csv', function (data) {

    $('#container_MET').highcharts('StockChart', {

      title: {
        text: 'MET'
      },
      xAxis: {
        events: {
          afterSetExtremes: syncExtremes
        },
        min: currTime - 86400*1000,
        max: currTime
      },
      yAxis: [{
        // airPress
        labels: {
          y: 5,
          style: {
            "color":"rgb(100, 100, 100)"
          }
        },
        title: {
          text: 'Atmospheric Pressure [hPa]',
          style: {
            "color":"rgb(100, 100, 100)"
          }
        },
        lineColor: 'rgb(100, 100, 100)',
        tickColor: 'rgb(100, 100, 100)'
      },{
        // airTemp
        opposite: true,
        labels: {
          y: 5,
          style: {
            "color":"rgb(11, 99, 160)"
          }
        },
        title: {
          text: 'Air Temperature [Â°C]',
          style: {
            "color":"rgb(11, 99, 160)"
          }
        },
        lineColor: 'rgb(11, 99, 160)',
        tickColor: 'rgb(11, 99, 160)'
      }],

      data : {
        csv : data
      },

      series: [{
        // airPress
        index: 0,
        color: 'rgba(100, 100, 100, 0.5)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgb(100, 100, 100)'
        },
        tooltip: {
          pointFormat: '<div style="float:right; height:20px">' +
          '<div style="float:left"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float:left">p&nbsp;</div>' +
          '<div style="float:right; text-align:right; width:50px"><b>{point.y}</b></div>' +
          '</div>'
        }
      },{
        // airTemp
        color: 'rgba(11, 99, 160, 0.9)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgb(11, 99, 160)'
        },
        tooltip: {
          pointFormat: '<div style="float:right; height:20px">' +
          '<div style="float:left"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float:left">T&nbsp;</div>' +
          '<div style="float:right; text-align:right; width:30px"><b>{point.y}</b></div>' +
          '</div>'
        },
        yAxis: 1
      }],

      navigator : {
        enabled : false
      },

      tooltip: {
        valueDecimals: 1,
        headerFormat:'<p style="text-align:right; margin-bottom:0px; margin-top:0px">{point.key}</p>' +
        '<div style="overflow:auto; width:180px">',
        footerFormat: '</div>'
      }

    }); //end MET container

  }); //end $.get fn for MET

  //-------------------------------------------------//

  // Chart for VR2C [tagID]

  $.get('csv/tagID.csv', function (data) {

    $('#container_tagID').highcharts('StockChart', {

      title: {
        text: 'Tag ID'
      },
      xAxis: {
        events: {
          afterSetExtremes: syncExtremes
        },
        min: currTime - 86400*1000,
        max: currTime
      },
      yAxis: [{
        // tagID
        labels: {
          y: 5,
          style: {
            "color":"rgb(100, 100, 100)"
          }
        },
        title: {
          text: 'ID #',
          style: {
            "color":"rgb(100, 100, 100)"
          }
        },
        lineColor: 'rgb(100, 100, 100)',
        tickColor: 'rgb(100, 100, 100)'
      }],

      data : {
        csv : data
      },

      series: [{
        // tagID
        color: 'rgba(75, 75, 75, 0)',
        lineWidth: 0,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgb(75, 75, 75)'
        },
        dataGrouping: {
          approximation: 'open'
        },
        tooltip: {
          pointFormat: '<span style="color:{point.color}">\u25CF</span> Tag ID: <b>{point.y}</b>',
        },
      }],

      navigator : {
        enabled : false
      },

      tooltip: {
        valueDecimals: 0,
        headerFormat:'<p style="text-align:right; margin-bottom:0px; margin-top:0px">{point.key}</p>' +
        '<div style="overflow:auto; width:180px">',
        pointFormat: '<div style="float:right; height:20px">' +
        '<div style="float:left"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
        '<div style="float:left">Tag ID&nbsp;</div>' +
        '<div style="float:right; text-align:right; width:45px"><b>{point.y}</b></div>' +
        '</div>',
        footerFormat: '</div>'
      }

    }); //end VR2C container

  }); //end $.get fn for VR2C

  //-------------------------------------------------//

  // Chart for Equilibrator Diagnostics [T-on, T-off, p-on, p-off, CO2-on, CO2off]

  $.get('csv/CO2equil.csv', function (data) {

    $('#container_CO2_Equil').highcharts('StockChart', {

      title: {
        text: 'Equilibrator'
      },
      xAxis: {
        events: {
          afterSetExtremes: syncExtremes
        },
        min: currTime - 86400*1000,
        max: currTime
      },

      yAxis: [{
        // T
        labels: {
          style: {
            "color":"rgb(11, 99, 160)"
          }
        },
        title: {
          text: 'Temperature [Â°C]',
          style: {
            "color":"rgb(11, 99, 160)"
          }
        },
        lineColor: 'rgb(11, 99, 160)',
        tickColor: 'rgb(11, 99, 160)'
      },{
        // p
        opposite: true,
        labels: {
          style: {
            "color":"rgb(44, 160, 44)"
          }
        },
        title: {
          text: 'Pressure [kPa]',
          style: {
            "color":"rgb(44, 160, 44)"
          }
        },
        lineColor: 'rgb(44, 160, 44)',
        tickColor: 'rgb(44, 160, 44)'
      },{
        // xCO2
        opposite: true,
        labels: {
          style: {
            "color":"rgb(50, 50, 50)"
          }
        },
        title: {
          text: 'xCO2 [ppm]',
          style: {
            "color":"rgb(50, 50, 50)"
          }
        },
        lineColor: 'rgb(50, 50, 50)',
        tickColor: 'rgb(50, 50, 50)'
      }],

      data : {
        csv : data
      },

      series: [{
        // T-ON
        index: 2,
        color: 'rgba(11, 99, 160, 0.5)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgba(11, 99, 160, 0.5)'
        },
        tooltip: {
          pointFormat: '<div style="float: right; height:20px">' +
          '<div style="float: left; text-align: right;"><u>ON:</u><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float: left; text-align: left; width: 12px">T</div>' +
          '<div style="float: right; text-align: right; width: 35px"><b>{point.y}</b></div>' +
          '</div>',
        }
      },{
        // T-OFF
        index: 5,
        color: 'rgba(11, 99, 160, 0.9)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgba(11, 99, 160, 0.9)'
        },
        tooltip: {
          pointFormat: '<div style="float: right; height:20px">' +
          '<div style="float: left; text-align: right"><u>OFF:</u><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float: left; text-align: left; width: 12px">T</div>' +
          '<div style="float: right; text-align:right; width: 35px"><b>{point.y}</b></div>' +
          '</div>',
        }
      },{
        // P-ON
        index: 1,
        color: 'rgba(44, 160, 44, 0.5)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgba(44, 160, 44, 0.5)'
        },
        tooltip: {
          pointFormat: '<div style="float: right; height:20px">' +
          '<div style="float: left;"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float: left; text-align: left; width: 10px">p</div>' +
          '<div style="float: right; text-align:right; width: 45px"><b>{point.y}</b></div>' +
          '</div>',
        },
        yAxis: 1
      },{
        // P-OFF
        index: 4,
        color: 'rgba(44, 160, 44, 0.9)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgba(44, 160, 44, 0.9)'
        },
        tooltip: {
          pointFormat: '<div style="float: right; height:20px">' +
          '<div style="float: left;"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float: left; text-align: left; width: 10px">p</div>' +
          '<div style="float: right; text-align: right; width: 45px"><b>{point.y}</b></div>' +
          '</div>',
        },
        yAxis: 1
      },{
        // CO2-ON
        index: 0,
        color: 'rgba(150, 150, 150, 0.5)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgb(150, 150, 150)'
        },
        tooltip: {
          pointFormat: '<div style="float: right; height:20px">' +
          '<div style="float: left;"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float: left; text-align: left; width: 35px">xCO<sub>2</sub></div>' +
          '<div style="float: right; text-align: right; width: 45px"><b>{point.y}</b></div>' +
          '</div>',
        },
        yAxis: 2
      },{
        // CO2-OFF
        index: 3,
        color: 'rgba(50, 50, 50, 0.5)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgb(50, 50, 50)'
        },
        tooltip: {
          pointFormat: '<div style="float: right; height:22px">' + // !
          '<div style="float: left;"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float: left; text-align: left; width: 35px">xCO<sub>2</sub>&nbsp;</div>' +
          '<div style="float: right; text-align: right; width: 45px"><b>{point.y}</b></div>' +
          '</div>',
        },
        yAxis: 2
      }],

      navigator : {
        enabled : false
      },

      tooltip: {
        valueDecimals: 2,
        headerFormat:'<p style="text-align:right; margin-bottom:0px; margin-top:0px">{point.key}</p>' +
        '<div style="overflow:auto; width:285px">',
        footerFormat: '</div>'
      }

    }); //end CO2_Equil container

  }); //end $.get fn for CO2equil

  //-------------------------------------------------//

  // Chart for Zero Diagnostics [T-on, T-off, p-on, p-off, CO2-on, CO2off]

  $.get('csv/CO2zero.csv', function (data) {

    $('#container_CO2_Zero').highcharts('StockChart', {

      title: {
        text: 'Zero'
      },
      xAxis: {
        events: {
          afterSetExtremes: syncExtremes
        },
        min: currTime - 86400*1000,
        max: currTime
      },

      yAxis: [{
        // T
        labels: {
          style: {
            "color":"rgb(11, 99, 160)"
          }
        },
        title: {
          text: 'Temperature [Â°C]',
          style: {
            "color":"rgb(11, 99, 160)"
          }
        },
        lineColor: 'rgb(11, 99, 160)',
        tickColor: 'rgb(11, 99, 160)'
      },{
        // p
        opposite: true,
        labels: {
          style: {
            "color":"rgb(44, 160, 44)"
          }
        },
        title: {
          text: 'Pressure [kPa]',
          style: {
            "color":"rgb(44, 160, 44)"
          }
        },
        lineColor: 'rgb(44, 160, 44)',
        tickColor: 'rgb(44, 160, 44)'
      },{
        // xCO2
        opposite: true,
        labels: {
          style: {
            "color":"rgb(50, 50, 50)"
          }
        },
        title: {
          text: 'xCO2 [ppm]',
          style: {
            "color":"rgb(50, 50, 50)"
          }
        },
        lineColor: 'rgb(50, 50, 50)',
        tickColor: 'rgb(50, 50, 50)'
      }],

      series: [{
        // T-ON
        index: 2,
        color: 'rgba(11, 99, 160, 0.5)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgba(11, 99, 160, 0.5)'
        },
        tooltip: {
          pointFormat: '<div style="float: right; height:20px">' +
          '<div style="float: left; text-align: right;"><u>ON:</u><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float: left; text-align: left; width: 12px">T</div>' +
          '<div style="float: right; text-align: right; width: 35px"><b>{point.y}</b></div>' +
          '</div>',
        }
      },{
        // T-OFF
        index: 5,
        color: 'rgba(11, 99, 160, 0.9)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgba(11, 99, 160, 0.9)'
        },
        tooltip: {
          pointFormat: '<div style="float: right; height:20px">' +
          '<div style="float: left; text-align: right"><u>OFF:</u><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float: left; text-align: left; width: 12px">T</div>' +
          '<div style="float: right; text-align:right; width: 35px"><b>{point.y}</b></div>' +
          '</div>',
        }
      },{
        // P-ON
        index: 1,
        color: 'rgba(44, 160, 44, 0.5)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgba(44, 160, 44, 0.5)'
        },
        tooltip: {
          pointFormat: '<div style="float: right; height:20px">' +
          '<div style="float: left;"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float: left; text-align: left; width: 10px">p</div>' +
          '<div style="float: right; text-align:right; width: 45px"><b>{point.y}</b></div>' +
          '</div>',
        },
        yAxis: 1
      },{
        // P-OFF
        index: 4,
        color: 'rgba(44, 160, 44, 0.9)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgba(44, 160, 44, 0.9)'
        },
        tooltip: {
          pointFormat: '<div style="float: right; height:20px">' +
          '<div style="float: left;"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float: left; text-align: left; width: 10px">p</div>' +
          '<div style="float: right; text-align: right; width: 45px"><b>{point.y}</b></div>' +
          '</div>',
        },
        yAxis: 1
      },{
        // CO2-ON
        index: 0,
        color: 'rgba(150, 150, 150, 0.5)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgb(150, 150, 150)'
        },
        tooltip: {
          pointFormat: '<div style="float: right; height:20px">' +
          '<div style="float: left;"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float: left; text-align: left; width: 35px">xCO<sub>2</sub></div>' +
          '<div style="float: right; text-align: right; width: 45px"><b>{point.y}</b></div>' +
          '</div>',
        },
        yAxis: 2
      },{
        // CO2-OFF
        index: 3,
        color: 'rgba(50, 50, 50, 0.5)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgb(50, 50, 50)'
        },
        tooltip: {
          pointFormat: '<div style="float: right; height:22px">' +
          '<div style="float: left;"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float: left; text-align: left; width: 35px">xCO<sub>2</sub>&nbsp;</div>' +
          '<div style="float: right; text-align: right; width: 45px"><b>{point.y}</b></div>' +
          '</div>',
        },
        yAxis: 2
      }],

      data : {
        csv : data
      },

      navigator : {
        enabled : false
      },

      tooltip: {
        valueDecimals: 2,
        headerFormat:'<p style="text-align:right; margin-bottom:0px; margin-top:0px">{point.key}</p>' +
        '<div style="overflow:auto; width:285px">',
        footerFormat: '</div>'
      }

    }); //end CO2_Zero container

  }); //end $.get fn for CO2zero

  //-------------------------------------------------//

  // Chart for Air Diagnostics [T-on, T-off, p-on, p-off, CO2-on, CO2off]

  $.get('csv/CO2air.csv', function (data) {

    $('#container_CO2_Air').highcharts('StockChart', {

      title: {
        text: 'Air'
      },
      xAxis: {
        events: {
          afterSetExtremes: syncExtremes
        },
        min: currTime - 86400*1000,
        max: currTime
      },

      yAxis: [{
        // T
        labels: {
          style: {
            "color":"rgb(11, 99, 160)"
          }
        },
        title: {
          text: 'Temperature [Â°C]',
          style: {
            "color":"rgb(11, 99, 160)"
          }
        },
        lineColor: 'rgb(11, 99, 160)',
        tickColor: 'rgb(11, 99, 160)'
      },{
        // p
        opposite: true,
        labels: {
          style: {
            "color":"rgb(44, 160, 44)"
          }
        },
        title: {
          text: 'Pressure [kPa]',
          style: {
            "color":"rgb(44, 160, 44)"
          }
        },
        lineColor: 'rgb(44, 160, 44)',
        tickColor: 'rgb(44, 160, 44)'
      },{
        // xCO2
        opposite: true,
        labels: {
          style: {
            "color":"rgb(50, 50, 50)"
          }
        },
        title: {
          text: 'xCO2 [ppm]',
          style: {
            "color":"rgb(50, 50, 50)"
          }
        },
        lineColor: 'rgb(50, 50, 50)',
        tickColor: 'rgb(50, 50, 50)'
      }],

      series: [{
        // T-ON
        index: 2,
        color: 'rgba(11, 99, 160, 0.5)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgba(11, 99, 160, 0.5)'
        },
        tooltip: {
          pointFormat: '<div style="float: right; height:20px">' +
          '<div style="float: left; text-align: right;"><u>ON:</u><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float: left; text-align: left; width: 12px">T</div>' +
          '<div style="float: right; text-align: right; width: 35px"><b>{point.y}</b></div>' +
          '</div>',
        }
      },{
        // T-OFF
        index: 5,
        color: 'rgba(11, 99, 160, 0.9)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgba(11, 99, 160, 0.9)'
        },
        tooltip: {
          pointFormat: '<div style="float: right; height:20px">' +
          '<div style="float: left; text-align: right"><u>OFF:</u><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float: left; text-align: left; width: 12px">T</div>' +
          '<div style="float: right; text-align:right; width: 35px"><b>{point.y}</b></div>' +
          '</div>',
        }
      },{
        // P-ON
        index: 1,
        color: 'rgba(44, 160, 44, 0.5)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgba(44, 160, 44, 0.5)'
        },
        tooltip: {
          pointFormat: '<div style="float: right; height:20px">' +
          '<div style="float: left;"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float: left; text-align: left; width: 10px">p</div>' +
          '<div style="float: right; text-align:right; width: 45px"><b>{point.y}</b></div>' +
          '</div>',
        },
        yAxis: 1
      },{
        // P-OFF
        index: 4,
        color: 'rgba(44, 160, 44, 0.9)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgba(44, 160, 44, 0.9)'
        },
        tooltip: {
          pointFormat: '<div style="float: right; height:20px">' +
          '<div style="float: left;"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float: left; text-align: left; width: 10px">p</div>' +
          '<div style="float: right; text-align: right; width: 45px"><b>{point.y}</b></div>' +
          '</div>',
        },
        yAxis: 1
      },{
        // CO2-ON
        index: 0,
        color: 'rgba(150, 150, 150, 0.5)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgb(150, 150, 150)'
        },
        tooltip: {
          pointFormat: '<div style="float: right; height:20px">' +
          '<div style="float: left;"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float: left; text-align: left; width: 35px">xCO<sub>2</sub></div>' +
          '<div style="float: right; text-align: right; width: 45px"><b>{point.y}</b></div>' +
          '</div>',
        },
        yAxis: 2
      },{
        // CO2-OFF
        index: 3,
        color: 'rgba(50, 50, 50, 0.5)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgb(50, 50, 50)'
        },
        tooltip: {
          pointFormat: '<div style="float: right; height:22px">' +
          '<div style="float: left;"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float: left; text-align: left; width: 35px">xCO<sub>2</sub>&nbsp;</div>' +
          '<div style="float: right; text-align: right; width: 45px"><b>{point.y}</b></div>' +
          '</div>',
        },
        yAxis: 2
      }],

      data : {
        csv : data
      },

      navigator : {
        enabled : false
      },

      tooltip: {
        valueDecimals: 2,
        headerFormat:'<p style="text-align:right; margin-bottom:0px; margin-top:0px">{point.key}</p>' +
        '<div style="overflow:auto; width:285px">',
        footerFormat: '</div>'
      }

    }); //end CO2_Air container

  }); //end $.get fn for CO2air

  //-------------------------------------------------//

  // Chart for Standard Diagnostics [T-on, p-on, p-off, CO2-off]

  $.get('csv/CO2stnd.csv', function (data) {

    $('#container_CO2_Stnd').highcharts('StockChart', {

      title: {
        text: 'Standard'
      },
      xAxis: {
        events: {
          afterSetExtremes: syncExtremes
        },
        min: currTime - 86400*1000,
        max: currTime
      },

      yAxis: [{
        // T
        labels: {
          style: {
            "color":"rgb(11, 99, 160)"
          }
        },
        title: {
          text: 'Temperature [Â°C]',
          style: {
            "color":"rgb(11, 99, 160)"
          }
        },
        lineColor: 'rgb(11, 99, 160)',
        tickColor: 'rgb(11, 99, 160)'
      },{
        // p
        opposite: true,
        labels: {
          style: {
            "color":"rgb(44, 160, 44)"
          }
        },
        title: {
          text: 'Pressure [kPa]',
          style: {
            "color":"rgb(44, 160, 44)"
          }
        },
        lineColor: 'rgb(44, 160, 44)',
        tickColor: 'rgb(44, 160, 44)'
      },{
        // xCO2
        opposite: true,
        labels: {
          style: {
            "color":"rgb(50, 50, 50)"
          }
        },
        title: {
          text: 'xCO2 [ppm]',
          style: {
            "color":"rgb(50, 50, 50)"
          }
        },
        lineColor: 'rgb(50, 50, 50)',
        tickColor: 'rgb(50, 50, 50)'
      }],

      series: [{
        // T-ON
        index: 4,
        color: 'rgba(11, 99, 160, 0.5)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgba(11, 99, 160, 0.5)'
        },
        tooltip: {
          pointFormat: '<div style="float: right; height:20px">' +
          '<div style="float: left; text-align: right;"><u>ON:</u><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float: left; text-align: left; width: 12px">T</div>' +
          '<div style="float: right; text-align: right; width: 35px"><b>{point.y}</b></div>' +
          '</div>',
        },
        connectNulls: true
      },{
        // T-OFF
        index: 1,
        color: 'rgba(11, 99, 160, 0.9)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgba(11, 99, 160, 0.9)'
        },
        tooltip: {
          pointFormat: '<div style="float: right; height:20px">' +
          '<div style="float: left;"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float: left; text-align: left; width: 12px">T&nbsp;</div>' +
          '<div style="float: right; text-align: right; width: 35px"><b>{point.y}</b></div>' +
          '</div>',
        },
        connectNulls: true
      },{
        // P-ON
        index: 3,
        color: 'rgba(44, 160, 44, 0.5)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgba(44, 160, 44, 0.5)'
        },
        tooltip: {
          pointFormat: '<div style="float: right; height:20px">' +
          '<div style="float: left; text-align: right;"><u>ON:</u><span style="color:{point.color}">&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float: left; text-align: left; width: 12px">p</div>' +
          '<div style="float: right; text-align: right; width: 45px"><b>{point.y}</b></div>' +
          '</div>',
        },
        connectNulls: true,
        yAxis: 1
      },{
        // P-OFF
        index: 2,
        color: 'rgba(44, 160, 44, 0.9)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgba(44, 160, 44, 0.9)'
        },
        tooltip: {
          pointFormat: '<div style="float: right; height:20px">' +
          '<div style="float: left; text-align: right">&nbsp;&nbsp;&nbsp;<u>OFF:</u><span style="color:{point.color}">&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float: left; text-align: left; width: 12px">p</div>' +
          '<div style="float: right; text-align:right; width: 45px"><b>{point.y}</b></div>' +
          '</div>',
        },
        connectNulls: true,
        yAxis: 1
      },{
        // CO2-ON
        index: 5,
        color: 'rgba(150, 150, 150, 0.5)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgb(150, 150, 150)'
        },
        tooltip: {
          pointFormat: '<div style="float: right; height:20px">' +
          '<div style="float: left;"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float: left; text-align: left; width: 35px">xCO<sub>2</sub></div>' +
          '<div style="float: right; text-align: right; width: 45px"><b>{point.y}</b></div>' +
          '</div>',
        },
        connectNulls: true,
        yAxis: 2
      },{
        // CO2-OFF
        index: 0,
        color: 'rgba(50, 50, 50, 0.5)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgb(50, 50, 50)'
        },
        tooltip: {
          pointFormat: '<div style="float: right; height:22px">' +
          '<div style="float: left;"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float: left; text-align: left; width: 35px">xCO<sub>2</sub>&nbsp;</div>' +
          '<div style="float: right; text-align: right; width: 45px"><b>{point.y}</b></div>' +
          '</div>',
        },
        connectNulls: true,
        yAxis: 2
      }],

      data : {
        csv : data
      },

      navigator : {
        enabled : false
      },

      tooltip: {
        valueDecimals: 2,
        headerFormat:'<p style="text-align:right; margin-bottom:0px; margin-top:0px">{point.key}</p>' +
        '<div style="overflow:auto; width:400px">',
        footerFormat: '</div>'
      }

    }); //end CO2_Stnd container

  }); //end $.get fn for CO2stnd


  //-------------------------------------------------//

  // Chart for CO2 Humidity [equilOff, airOff, stndOff]

  $.get('csv/CO2hum.csv', function (data) {

    $('#container_CO2humidity').highcharts('StockChart', {

      title: {
        text: 'Humidity'
      },
      xAxis: {
        events: {
          afterSetExtremes: syncExtremes
        },
        min: currTime - 86400*1000,
        max: currTime
      },
      yAxis: [{
        // all
        labels: {
          y: 5,
          style: {
            "color":"rgb(11, 99, 160)"
          }
        },
        title: {
          text: 'Humidity [V]',
          style: {
            "color":"rgb(11, 99, 160)"
          }
        },
        lineColor: 'rgb(11, 99, 160)',
        tickColor: 'rgb(11, 99, 160)'
      }],

      data : {
        csv : data
      },

      series: [{
        // equilOff
        index: 2,
        color: 'rgba(11, 99, 160, 0.9)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgb(11, 99, 160)'
        },
        tooltip: {
          pointFormat: '<div style="float:right; height:20px">' +
          '<div style="float:left"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float:left">Equil&nbsp;</div>' +
          '<div style="float:right; text-align:right; width:35px"><b>{point.y}</b></div>' +
          '</div>'
        }
      },{
        // airOff
        index: 1,
        color: 'rgba(99, 99, 160, 0.9)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgb(99, 99, 160)'
        },
        tooltip: {
          pointFormat: '<div style="float:right; height:20px">' +
          '<div style="float:left"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float:left">Air&nbsp;</div>' +
          '<div style="float:right; text-align:right; width:35px"><b>{point.y}</b></div>' +
          '</div>'
        }
      },{
        // stndOff
        index: 0,
        connectNulls: true,
        color: 'rgba(11, 160, 160, 0.9)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgb(11, 160, 160)'
        },
        tooltip: {
          pointFormat: '<div style="float:right; height:20px">' +
          '<div style="float:left"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float:left">Stnd&nbsp;</div>' +
          '<div style="float:right; text-align:right; width:35px"><b>{point.y}</b></div>' +
          '</div>'
        }
      }],

      navigator : {
        enabled : false
      },

      tooltip: {
        valueDecimals: 3,
        headerFormat:'<p style="text-align:right; margin-bottom:0px; margin-top:0px">{point.key}</p>' +
        '<div style="overflow:auto; width:275px">',
        footerFormat: '</div>'
      }

    }); //end CO2Humidity container

  }); //end $.get fn for CO2 Humidity

  //-------------------------------------------------//

  // Chart for Charging System [solarPowerGenerated, batteryChargingPower, outputPowerGenerated, totalBatteryPower]

  $.get('csv/Power.csv', function (data) {

    $('#container_Power').highcharts('StockChart', {

      title: {
        text: 'Power'
      },
      xAxis: {
        events: {
          afterSetExtremes: syncExtremes
        },
        min: currTime - 86400*1000,
        max: currTime
      },

      yAxis: [{
        // solar-Gen and batt-Charge
        opposite: true,
        labels: {
          y: 5,
          align: 'left', //?
          style: {
            "color":"rgb(11, 99, 160)"
          }
        },
        title: {
          text: 'Power [W]',
          style: {
            "color":"rgb(11, 99, 160)"
          }
        },
        lineColor: 'rgb(11, 99, 160)',
        tickColor: 'rgb(11, 99, 160)',
        max: 150
      },{
        // outputPowerGenerated
        opposite: true,
        labels: {
          y: 5,
          align: 'left', //?
          style: {
            "color":"rgb(255, 127, 14)"
          }
        },
        title: {
          text: 'Power [W]',
          style: {
            "color":"rgb(255, 127, 14)"
          }
        },
        lineColor: 'rgb(255, 127, 14)',
        tickColor: 'rgb(255, 127, 14)',
        max: 75
      },{
        // totalBatteryPower
        labels: {
          y: 5,
          style: {
            "color":"rgb(50, 50, 50)"
          }
        },
        title: {
          text: 'Energy [Wh]',
          style: {
            "color":"rgb(50, 50, 50)"
          }
        },
        lineColor: 'rgb(50, 50, 50)',
        tickColor: 'rgb(50, 50, 50)',
        min: 0,
        max: 1000
      }],

      data : {
        csv : data
      },

      series: [{
        // solar-Gen
        connectNulls: true,
        color: 'rgba(11, 99, 160, 0.9)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgb(11, 99, 160)'
        },
        tooltip: {
          pointFormat: '<div style="float:right; height:20px">' +
          '<div style="float:left"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float:left">Solar&nbsp;</div>' +
          '<div style="float:right; text-align:right; width:55px"><b>{point.y}</b></div>' +
          '</div>'
        },
        yAxis: 0
      },{
        // batt-Charge
        connectNulls: true,
        color: 'rgba(99, 99, 160, 0.9)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgb(99, 99, 160)'
        },
        tooltip: {
          pointFormat: '<div style="float:right; height:20px">' +
          '<div style="float:left"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float:left">Batt&nbsp;</div>' +
          '<div style="float:right; text-align:right; width:55px"><b>{point.y}</b></div>' +
          '</div>'
        },
        yAxis: 0
      },{
        // output-Gen
        connectNulls: true,
        color: 'rgba(255, 127, 14, 0.5)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgb(255, 127, 14)'
        },
        tooltip: {
          pointFormat: '<div style="float:right; height:20px">' +
          '<div style="float:left"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float:left">Output&nbsp;</div>' +
          '<div style="float:right; text-align:right; width:45px"><b>{point.y}</b></div>' +
          '</div>'
        },
        yAxis: 1
      },{
        // total energy
        connectNulls: true,
        color: 'rgba(50, 50, 50, 0.9)',
        lineWidth: 1,
        marker : {
          enabled : true,
          radius : 2,
          symbol: "circle",
          fillColor: 'rgb(50, 50, 50)'
        },
        tooltip: {
          pointFormat: '<div style="float:right; height:20px">' +
          '<div style="float:left"><span style="color:{point.color}">&nbsp;&nbsp;&nbsp;\u25CF</span></div>' +
          '<div style="float:left">Total Energy&nbsp;</div>' +
          '<div style="float:right; text-align:right; width:55px"><b>{point.y}</b></div>' +
          '</div>'
        },
        yAxis: 2
      }],

      navigator : {
        enabled : false
      },

      tooltip: {
        valueDecimals: 3,
        headerFormat:'<p style="text-align:right; margin-bottom:0px; margin-top:0px">{point.key}</p>' +
        '<div style="overflow:auto; width:350px">',
        footerFormat: '</div>'
      }

    }); //end Power container

  }); //end $.get fn for Power

  //-------------------------------------------------//

  // Chart for pCO2 vs pH

  $.get('csv/CO2SYS_2100.csv', function(data1) {

    $.get('csv/CO2SYS_2400.csv', function(data2) {

      $.get('csv/CO2_vs_pH.csv', function(data3) {

        var series1 = parseCSV(data1)
        var series2 = parseCSV(data2)
        var series3 = parseCSV(data3)

        Highcharts.chart('container_CO2_vs_pH', {
          chart: {
            marginLeft: 70,
            spacingLeft: 50,
            marginRight: 10,
            marginBottom: 50,
            zoomType: 'xy',
            resetZoomButton: {
              position: {
                verticalAlign: 'bottom',
                y: -45
              }
            }
          },
          title: {
            useHTML: true,
            text: 'pCO<sub>2</sub> vs pH',
            x: 25,
            y: 25
          },
          subtitle: {
            text: '[Last 24 hr]',
            align: 'left',
            x: 30,
            y: 55
          },
          exporting: {
            enabled: false
          },
          xAxis: {
            min: 100,
            max: 1000,
            tickInterval: 200,
            labels: {
              style: {
                "color":"rgb(50, 50, 50)"
              }
            },
            title: {
              useHTML: true,
              text: 'pCO<sub>2</sub> [ppm]',
              style: {
                "color":"rgb(50, 50, 50)"
              },
              x: 25
            },
            lineColor: 'rgb(50, 50, 50)',
            tickColor: 'rgb(50, 50, 50)'
          },
          yAxis: {
            gridLineWidth: 1,
            gridLineDashStyle: 'ShortDot',
            labels: {
              style: {
                "color":"rgb(50, 50, 50)"
              }
            },
            title: {
              text: 'pH [sea water scale]',
              style: {
                "color":"rgb(50, 50, 50)"
              }
            },
            lineColor: 'rgb(50, 50, 50)',
            tickColor: 'rgb(50, 50, 50)'
          },
          legend: {
            layout: 'vertical',
            align: 'right',
            verticalAlign: 'top',
            x: 0,
            y: 40,
            itemMarginTop: -5,
            itemMarginBottom: 5,
            title: {
              text: 'TA:',
              style: {
                'text-align': 'center'
              }
            }
          },

          series: [{
            name: '2100',
            type: 'line',
            data: series1,
            enableMouseTracking: false,
            color: 'rgba(50, 50, 50, 0.9)',
            lineWidth: 2,
            marker: {
              enabled: false
            },
            events: {
              legendItemClick: function() {
                return false;
              }
            }
          }, {
            name: '2400',
            type: 'line',
            data: series2,
            enableMouseTracking: false,
            color: 'rgba(200, 200, 200, 0.9)',
            lineWidth: 2,
            marker: {
              enabled: false
            },
            events: {
              legendItemClick: function() {
                return false;
              }
            }
          }, {
            name: 'data',
            type: 'scatter',
            dataGrouping: { // !
              enabled: false
            },
            data: series3,
            showInLegend: false,
            stickyTracking: false,
            marker: {
              enabled: true,
              fillColor: 'rgba(0, 0, 0, 0.5)',
              radius: 2,
              states: {
                hover: {
                  enabled: true,
                  fillColor: 'rgba(0, 0, 0, 0.5)'
                }
              }
            }
          }],

          tooltip: {
            headerFormat:'<div style="overflow:auto; width:180px">',
            pointFormat: '<div style="float:right; height:20px">' +
            '<div style="float:left;">pCO<sub>2</sub>&nbsp;</div>' +
            '<div style="float:left; text-align:left; width:50px"><b>{point.x}</b></div>' +
            '<div style="float:left;">pH&nbsp;</div>' +
            '<div style="float:left; text-align:left; width:40px"><b>{point.y}</b></div>' +
            '</div>',
            footerFormat: '</div>'
          }

        }) //container
      }) // file3
    }) //file2
  }) //file1

});