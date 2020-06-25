// Generated by CoffeeScript 1.12.7
(function() {
  var callWithJQuery;

  callWithJQuery = function(pivotModule) {
    if (typeof exports === "object" && typeof module === "object") {
      return pivotModule(require("jquery"), require("plotly.js"));
    } else if (typeof define === "function" && define.amd) {
      return define(["jquery", "plotly.js"], pivotModule);
    } else {
      return pivotModule(jQuery, Plotly);
    }
  };

  callWithJQuery(function($, Plotly) {
    var CSSProps, computedStyle, getAxisGridColor, getAxisLineColor, getAxisZeroLineColor, getCSSPropertyValue, getFontColor, getPaperBGColor, getPlotBGColor, makePlotlyChart, makePlotlyScatterChart;
    computedStyle = null;
    CSSProps = {
      paper_bgcolor: null,
      plot_bgcolor: null,
      font_color: null,
      axis_grid_color: null,
      axis_line_color: null,
      axis_zeroline_color: null
    };
    getCSSPropertyValue = function(propertyName) {
      if (computedStyle === null) {
        computedStyle = getComputedStyle(document.documentElement);
      }
      return computedStyle.getPropertyValue(propertyName);
    };
    getPaperBGColor = function() {
      if (CSSProps.paper_bgcolor === null) {
        CSSProps.paper_bgcolor = getCSSPropertyValue('--background-color');
      }
      return CSSProps.paper_bgcolor;
    };
    getPlotBGColor = function() {
      if (CSSProps.plot_bgcolor === null) {
        CSSProps.plot_bgcolor = getCSSPropertyValue('--component-background-color');
      }
      return CSSProps.plot_bgcolor;
    };
    getFontColor = function() {
      if (CSSProps.font_color === null) {
        CSSProps.font_color = getCSSPropertyValue('--text-color');
      }
      return CSSProps.font_color;
    };
    getAxisGridColor = function() {
      if (CSSProps.axis_grid_color === null) {
        CSSProps.axis_grid_color = getCSSPropertyValue('--grid-separator-border-color');
      }
      return CSSProps.axis_grid_color;
    };
    getAxisLineColor = function() {
      if (CSSProps.axis_line_color === null) {
        CSSProps.axis_line_color = getCSSPropertyValue('--component-border-color');
      }
      return CSSProps.axis_line_color;
    };
    getAxisZeroLineColor = function() {
      if (CSSProps.axis_zeroline_color === null) {
        CSSProps.axis_zeroline_color = getCSSPropertyValue('--component-border-color');
      }
      return CSSProps.axis_zeroline_color;
    };
    makePlotlyChart = function(reverse, traceOptions, layoutOptions, transpose) {
      if (traceOptions == null) {
        traceOptions = {};
      }
      if (layoutOptions == null) {
        layoutOptions = {};
      }
      if (transpose == null) {
        transpose = false;
      }
      return function(pivotData, opts) {
        var colKeys, columns, d, data, datumKeys, defaults, fullAggName, groupByTitle, hAxisTitle, i, layout, result, rowKeys, rows, tKeys, titleText, traceKeys;
        defaults = {
          localeStrings: {
            vs: "vs",
            by: "by"
          },
          plotly: {},
          plotlyConfig: {
            responsive: true,
            locale: opts.locale
          }
        };
        opts = $.extend(true, {}, defaults, opts);
        rowKeys = pivotData.getRowKeys();
        colKeys = pivotData.getColKeys();
        if (reverse) {
          tKeys = rowKeys;
          rowKeys = colKeys;
          colKeys = tKeys;
        }
        traceKeys = transpose ? colKeys : rowKeys;
        if (traceKeys.length === 0) {
          traceKeys.push([]);
        }
        datumKeys = transpose ? rowKeys : colKeys;
        if (datumKeys.length === 0) {
          datumKeys.push([]);
        }
        fullAggName = pivotData.aggregatorName;
        if (pivotData.valAttrs.length) {
          fullAggName += "(" + (pivotData.valAttrs.join(", ")) + ")";
        }
        data = traceKeys.map(function(traceKey) {
          var datumKey, j, labels, len, trace, val, values;
          values = [];
          labels = [];
          for (j = 0, len = datumKeys.length; j < len; j++) {
            datumKey = datumKeys[j];
            val = parseFloat(pivotData.getAggregator(transpose ^ reverse ? datumKey : traceKey, transpose ^ reverse ? traceKey : datumKey).value());
            values.push(isFinite(val) ? val : null);
            labels.push(datumKey.join('-') || ' ');
          }
          trace = {
            name: traceKey.join('-') || fullAggName
          };
          if (traceOptions.type === "pie") {
            trace.values = values;
            trace.labels = labels.length > 1 ? labels : [fullAggName];
          } else {
            trace.x = transpose ? values : labels;
            trace.y = transpose ? labels : values;
          }
          return $.extend(trace, traceOptions);
        });
        if (transpose ^ reverse) {
          hAxisTitle = pivotData.rowAttrs.join("-");
          groupByTitle = pivotData.colAttrs.join("-");
        } else {
          hAxisTitle = pivotData.colAttrs.join("-");
          groupByTitle = pivotData.rowAttrs.join("-");
        }
        titleText = fullAggName;
        if (hAxisTitle !== "") {
          titleText += " " + opts.localeStrings.vs + " " + hAxisTitle;
        }
        if (groupByTitle !== "") {
          titleText += " " + opts.localeStrings.by + " " + groupByTitle;
        }
        layout = {
          title: titleText,
          hovermode: 'closest',
          autosize: true,
          paper_bgcolor: getPaperBGColor(),
          plot_bgcolor: getPlotBGColor(),
          font: {
            color: getFontColor()
          }
        };
        if (traceOptions.type === 'pie') {
          columns = Math.ceil(Math.sqrt(data.length));
          rows = Math.ceil(data.length / columns);
          layout.grid = {
            columns: columns,
            rows: rows
          };
          for (i in data) {
            d = data[i];
            d.domain = {
              row: Math.floor(i / columns),
              column: i - columns * Math.floor(i / columns)
            };
            if (data.length > 1) {
              d.title = d.name;
            }
          }
          if (data[0].labels.length === 1) {
            layout.showlegend = false;
          }
        } else {
          layout.xaxis = {
            title: transpose ? fullAggName : null,
            automargin: true,
            gridcolor: getAxisGridColor(),
            linecolor: getAxisLineColor(),
            zerolinecolor: getAxisZeroLineColor()
          };
          layout.yaxis = {
            title: transpose ? null : fullAggName,
            automargin: true,
            gridcolor: getAxisGridColor(),
            linecolor: getAxisLineColor(),
            zerolinecolor: getAxisZeroLineColor()
          };
        }
        result = $("<div>").appendTo($("body"));
        Plotly.newPlot(result[0], data, $.extend(layout, layoutOptions, opts.plotly), opts.plotlyConfig);
        return result.detach();
      };
    };
    makePlotlyScatterChart = function() {
      return function(pivotData, opts) {
        var colKey, colKeys, data, defaults, j, k, layout, len, len1, renderArea, result, rowKey, rowKeys, v;
        defaults = {
          localeStrings: {
            vs: "vs",
            by: "by"
          },
          plotly: {},
          plotlyConfig: {
            responsive: true,
            locale: opts.locale
          }
        };
        opts = $.extend(true, {}, defaults, opts);
        rowKeys = pivotData.getRowKeys();
        if (rowKeys.length === 0) {
          rowKeys.push([]);
        }
        colKeys = pivotData.getColKeys();
        if (colKeys.length === 0) {
          colKeys.push([]);
        }
        data = {
          x: [],
          y: [],
          text: [],
          type: 'scatter',
          mode: 'markers'
        };
        for (j = 0, len = rowKeys.length; j < len; j++) {
          rowKey = rowKeys[j];
          for (k = 0, len1 = colKeys.length; k < len1; k++) {
            colKey = colKeys[k];
            v = pivotData.getAggregator(rowKey, colKey).value();
            if (v != null) {
              data.x.push(colKey.join('-'));
              data.y.push(rowKey.join('-'));
              data.text.push(v);
            }
          }
        }
        layout = {
          title: pivotData.rowAttrs.join("-") + ' vs ' + pivotData.colAttrs.join("-"),
          hovermode: 'closest',
          xaxis: {
            title: pivotData.colAttrs.join('-'),
            automargin: true
          },
          yaxis: {
            title: pivotData.rowAttrs.join('-'),
            automargin: true
          },
          autosize: true,
          paper_bgcolor: getPaperBGColor(),
          plot_bgcolor: getPlotBGColor(),
          font: {
            color: getFontColor()
          }
        };
        renderArea = $("<div>", {
          style: "display:none;"
        }).appendTo($("body"));
        result = $("<div>").appendTo(renderArea);
        Plotly.newPlot(result[0], [data], $.extend(layout, opts.plotly), opts.plotlyConfig);
        result.detach();
        renderArea.remove();
        return result;
      };
    };
    $.pivotUtilities.plotly_renderers = {
      "BARCHART": makePlotlyChart(true, {
        type: 'bar'
      }, {
        barmode: 'group'
      }, false),
      "STACKED_BARCHART": makePlotlyChart(true, {
        type: 'bar'
      }, {
        barmode: 'relative'
      }, false),
      "LINECHART": makePlotlyChart(true, {}, {}, false),
      "AREACHART": makePlotlyChart(true, {
        stackgroup: 1
      }, {}, false),
      "SCATTERCHART": makePlotlyScatterChart(),
      "MULTIPLE_PIECHART": makePlotlyChart(false, {
        type: 'pie',
        scalegroup: 1,
        hoverinfo: 'label+value',
        textinfo: 'none'
      }, {}, false),
      "HORIZONTAL_BARCHART": makePlotlyChart(true, {
        type: 'bar',
        orientation: 'h'
      }, {
        barmode: 'group'
      }, true),
      "HORIZONTAL_STACKED_BARCHART": makePlotlyChart(true, {
        type: 'bar',
        orientation: 'h'
      }, {
        barmode: 'relative'
      }, true)
    };
    return $.pivotUtilities.colorThemeChanged = function(plot) {
      var relayout;
      computedStyle = null;
      CSSProps.paper_bgcolor = null;
      CSSProps.plot_bgcolor = null;
      CSSProps.font_color = null;
      CSSProps.axis_grid_color = null;
      CSSProps.axis_line_color = null;
      CSSProps.axis_zeroline_color = null;
      relayout = function() {
        var update;
        update = {
          paper_bgcolor: getPaperBGColor(),
          plot_bgcolor: getPlotBGColor(),
          font: {
            color: getFontColor()
          },
          xaxis: {
            gridcolor: getAxisGridColor(),
            linecolor: getAxisLineColor(),
            zerolinecolor: getAxisZeroLineColor()
          },
          yaxis: {
            gridcolor: getAxisGridColor(),
            linecolor: getAxisLineColor(),
            zerolinecolor: getAxisZeroLineColor()
          }
        };
        return Plotly.relayout(plot, update);
      };
      return setTimeout(relayout);
    };
  });

}).call(this);

//# sourceMappingURL=plotly_renderers.js.map
