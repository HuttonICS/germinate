/*
 *  Copyright 2018 Information and Computational Sciences,
 *  The James Hutton Institute.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

function plotlyScatterPlot() {
	var colorBy = '',
		colors = ["#1f77b4", "#ff7f0e", "#2ca02c", "#d62728", "#9467bd", "#8c564b", "#e377c2", "#7f7f7f", "#bcbd22", "#17becf"],
		height = 700,
		width = 700,
		xCategory = 'x',
		yCategory = 'y',
		onPointClicked = null,
		onPointsSelected = null;

	function chart(selection) {
		selection.each(function (rows) {
			var categories = new Set();

			var unpacked = unpack(rows, colorBy);
			for (i = 0; i < unpacked.length; i++) {
				categories.add(unpacked[i])
			}

			var data = [];

			var cats = [];
			categories.forEach(function (c) {
				cats.push(c);
			});

			for (var i = 0; i < cats.length; i++) {
				var x = cats[i] ? unpackConditional(rows, xCategory, colorBy, cats[i]) : unpack(rows, xCategory);
				var y = cats[i] ? unpackConditional(rows, yCategory, colorBy, cats[i]) : unpack(rows, yCategory);
				var ids = cats[i] ? unpackConditional(rows, 'dbId', colorBy, cats[i]) : unpack(rows, 'dbId');
				ids = ids.map(function (i) {
					return i + "-" + uuidv4();
				});
				var names = cats[i] ? unpackConditional(rows, 'name', colorBy, cats[i]) : unpack(rows, 'name');
				data.push({
					x: x,
					y: y,
					marker: {
						color: colors[i % colors.length],
						size: 6,
						opacity: 0.7
					},
					mode: 'markers',
					name: cats[i] ? cats[i] : '',
					ids: ids,
					text: names,
					type: 'scattergl',
					unselected: {
						marker: {
							opacity: 0.2
						}
					},
					showlegend: cats[i] !== undefined
				});
				data.push({
					x: x,
					marker: {
						color: colors[i % colors.length]
					},
					opacity: 0.5,
					name: cats[i] ? cats[i] : '',
					type: 'histogram',
					yaxis: 'y2',
					showlegend: false
				});
				data.push({
					y: y,
					marker: {
						color: colors[i % colors.length]
					},
					opacity: 0.5,
					name: cats[i] ? cats[i] : '',
					type: 'histogram',
					xaxis: 'x2',
					showlegend: false
				})
			}

			var layout = {
				autosize: false,
				bargap: 0,
				height: height,
				width: width,
				hovermode: 'closest',
				dragmode: 'select',
				margin: {t: 65, autoexpand: true},
				xaxis: {
					domain: [0, 0.95],
					showgrid: false,
					showline: true,
					title: xCategory
				},
				xaxis2: {
					domain: [0.95, 1],
					showgrid: false,
					showticklabels: false,
					zeroline: false
				},
				yaxis: {
					domain: [0, 0.95],
					showgrid: false,
					showline: true,
					title: yCategory
				},
				yaxis2: {
					domain: [0.95, 1],
					showgrid: false,
					showticklabels: false,
					zeroline: false
				},
				legend: {
					orientation: 'h'
				},
				barmode: 'overlay'
			};

			var config = {
				modeBarButtonsToRemove: ['toImage'],
				displayModeBar: true,
				responsive: true,
				displaylogo: false
			};

			Plotly.plot(this, data, layout, config);

			var that = this;

			this.on('plotly_selected', function (eventData) {
				if (!eventData || (eventData.points.length < 1)) {
					Plotly.restyle(that, {selectedpoints: [null]});

					if (onPointsSelected)
						onPointsSelected(null);
				} else {
					if (onPointsSelected) {
						var mapped = eventData.points.map(function (p) {
							return p.id.split('-')[0];
						});

						onPointsSelected(mapped);
					}
				}
			});

			if (onPointClicked) {
				this.on('plotly_click', function (data) {
					if (data.points.length > 0) {
						onPointClicked(data.points[0]);
					}
				});
			}
		});
	}

	function unpack(rows, key) {
		return rows.map(function (row) {
			return row[key];
		});
	}

	function unpackConditional(rows, key, referenceColumn, referenceValue) {
		return rows.filter(function (row) {
			return row[referenceColumn] === referenceValue;
		}).map(function (row) {
			if (row[key] === '') {
				return NaN
			} else {
				var value = parseFloat(row[key])

				if (isNaN(value)) {
					return row[key];
				} else {
					return value;
				}
			}
		})
	}

	function uuidv4() {
		return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
			var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
			return v.toString(16);
		});
	}

	chart.colorBy = function (_) {
		if (!arguments.length) return colorBy;
		colorBy = _;
		return chart;
	};

	chart.width = function (_) {
		if (!arguments.length) return width;
		width = _;
		return chart;
	};

	chart.height = function (_) {
		if (!arguments.length) return height;
		height = _;
		return chart;
	};

	chart.xCategory = function (_) {
		if (!arguments.length) return xCategory;
		xCategory = _;
		return chart;
	};

	chart.yCategory = function (_) {
		if (!arguments.length) return yCategory;
		yCategory = _;
		return chart;
	};

	chart.colors = function (_) {
		if (!arguments.length) return colors;
		colors = _;
		return chart;
	};

	chart.onPointClicked = function (_) {
		if (!arguments.length) return onPointClicked;
		onPointClicked = _;
		return chart;
	};

	chart.onPointsSelected = function (_) {
		if (!arguments.length) return onPointsSelected;
		onPointsSelected = _;
		return chart;
	};

	return chart;
}