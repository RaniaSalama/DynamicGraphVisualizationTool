/**
 * Original code for drawing graph is taken from http://bl.ocks.org/d3noob and
 * modified by Rania Ibrahim
 */

var GRAPH1_DIV_NAME = "#graph_1"; // graph 1 div name
var GRAPH2_DIV_NAME = "#graph_2"; // graph 2 div name
var DIV_WIDTH = 500; // graph 1 and graph 2 div width
var DIV_HEIGHT = 500; // graph 1 and graph 2 div height
// URL of the Java servlet
var GRAPH_SERVLET_URL = "http://localhost:8080/Dynamic_Graph_Visualization/GraphServlet?";

var graph1_data; // graph 1 content
var graph2_data; // graph 2 content
var svg1; // Drawing object for graph 1
var svg2; // Drawing object for graph 2
var nv; // Number of nodes in the graph
var k; // value of k parameter
var measure; // value of the distortion measure
var graph1_x; // x position of graph 1
var graph1_y; // y position of graph 1

/**
 * This function is called when the user chooses graph1's file It draws the
 * graph according to the specified file
 */
function readGraph1File(evt) {
	graph1_x = [];
	graph1_y = [];
	graph1_filename = evt.target.files[0];
	drawGraph(graph1_filename, svg1, GRAPH1_DIV_NAME);
}

/**
 * This function is called when the user chooses graph2's file It draws the
 * graph according to the specified file
 */
function readGraph2File(evt) {
	graph2_filename = evt.target.files[0];
	drawGraph(graph2_filename, svg2, GRAPH2_DIV_NAME);
}

function drawGraph(file, svg, div_name) {
	Math.seedrandom('abcde'); // fix graph position layout

	var links = [];
	if (svg) { // replace old graph with new one, delete original graph
		svg.selectAll("*").remove();
		svg.remove();
	}

	if (file) { // if file exist
		var reader = new FileReader();
		reader.onload = function(e) {
			var contents = e.target.result;
			var edges = contents.split('\n');
			var nodesIDs = {};
			var file_content = [];
			for (i = 0; i < edges.length; i++) {
				var edge = edges[i].split(',');
				var link = {};
				link['source'] = edge[0];
				link['target'] = edge[1];
				links.push(link);
				// Count number of nodes in the graph
				if (!nodesIDs[edge[0]])
					nodesIDs[edge[0]] = true;
				if (!nodesIDs[edge[1]])
					nodesIDs[edge[1]] = true;
				file_content.push(edges[i] + "-");
			}
			nv = Object.keys(nodesIDs).length; // number of nodes in the graph

			var nodes = {};
			// Compute the distinct nodes from the links.
			links.forEach(function(link) {
				link.source = nodes[link.source] || (nodes[link.source] = {
					name : link.source
				});
				link.target = nodes[link.target] || (nodes[link.target] = {
					name : link.target
				});
			});

			var force = d3.layout.force().nodes(d3.values(nodes)).links(links)
					.size([ DIV_WIDTH, DIV_HEIGHT ]).linkDistance(60).charge(
							-300).on("tick", tick).start();

			svg = d3.select(div_name).append("svg").attr("viewBox",
					"0 0 " + DIV_WIDTH + " " + DIV_HEIGHT);

			// Add links to svg
			var link = svg.selectAll(".link").data(force.links()).enter()
					.append("line").attr("class", "link");

			// Add nodes to svg
			var node = svg.selectAll(".node").data(force.nodes()).enter()
					.append("g").attr("class", "node").on("mouseover",
							mouseover).on("mouseout", mouseout)
					.call(force.drag);
			node.append("circle").attr("r", 10);
			node.append("text").attr("x", 12).attr("dy", ".35em").text(
					function(d) {
						return d.name;
					});

			function tick() {

				node.attr("transform", function(d) {
					// if the graph is graph1, then store its x and y positions
					if (div_name.localeCompare(GRAPH1_DIV_NAME) == 0) {
						graph1_x[d.name] = d.x;
						graph1_y[d.name] = d.y;
					} else {
						// set graph 2 nodes to same position as graph 1
						d.x = graph1_x[d.name];
						d.y = graph1_y[d.name];
					}
					return "translate(" + d.x + "," + d.y + ")";
				});

				link.attr("x1", function(d) {
					return d.source.x;
				}).attr("y1", function(d) {
					return d.source.y;
				}).attr("x2", function(d) {
					return d.target.x;
				}).attr("y2", function(d) {
					return d.target.y;
				});

			}

			function mouseover() {
				d3.select(this).select("circle").transition().duration(750)
						.attr("r", 16);
			}

			function mouseout() {
				d3.select(this).select("circle").transition().duration(750)
						.attr("r", 8);
			}

			// update svg of each graph
			if (div_name.localeCompare(GRAPH1_DIV_NAME) == 0) {
				svg1 = svg;
				graph1_data = file_content;
			} else {
				svg2 = svg;
				graph2_data = file_content;
			}

			// change value of k to be mx nv
			document.getElementById("k").max = nv;
			// set current values for k and measure
			k = document.getElementById("k").value;
			measure = document.getElementById("measure_list").value;
		};
		reader.readAsText(file);
	} else {
		alert("Failed to load file");
	}
}

function colorG1() {
	// call the servlet to calculate the distortion colors
	var url = GRAPH_SERVLET_URL + "graph1file=" + graph1_data + "&graph2file="
			+ graph2_data + "&k=" + k + "&measure=" + measure;
	var http;
	if (window.XMLHttpRequest) {// code for IE7+, Firefox, Chrome, Opera, Safari
		http = new XMLHttpRequest();
	} else {// code for IE6, IE5
		http = new ActiveXObject("Microsoft.XMLHTTP");
	}
	var colors = [];
	http.onreadystatechange = function() {
		if (http.readyState == 4 && http.status == 200) { // get the servlet results
			var colors_values = http.responseText;
			var colors_array = colors_values.split(',');
			for ( var i = 0; i < nv; i++) {
				colors[i + 1] = colors_array[i];
			}
			// color graph1 with the result
			svg1.selectAll(".node").append("circle").attr("r", 10).style(
					"fill", function(d1) {
						return colors[d1.name];
					});
		}
	};
	http.open("GET", url, true);
	http.send();

}

function colorG2() {
	// call the servlet to calculate the distortion colors
	var url = GRAPH_SERVLET_URL + "graph1file=" + graph1_data + "&graph2file="
			+ graph2_data + "&k=" + k + "&measure=" + measure;
	var http;
	if (window.XMLHttpRequest) {// code for IE7+, Firefox, Chrome, Opera, Safari
		http = new XMLHttpRequest();
	} else {// code for IE6, IE5
		http = new ActiveXObject("Microsoft.XMLHTTP");
	}
	var colors = [];
	http.onreadystatechange = function() {
		if (http.readyState == 4 && http.status == 200) { // get the servlet results
			var colors_values = http.responseText;
			var colors_array = colors_values.split(',');
			for ( var i = 0; i < nv; i++) {
				colors[i + 1] = colors_array[i];
			}
			svg2.selectAll(".node").append("circle").attr("r", 10).style(
					"fill", function(d1) {
						return colors[d1.name];
					});
		}
	};
	http.open("GET", url, true);
	http.send();

}

function changeK() {
	// when change k value, set the new value and re-color the graphs
	// based on the new k value
	k = document.getElementById("k").value;
	colorG1();
	colorG2();
}

function changeMeasure() {
	//when change the measure, store the new value and re-color the graphs
	// based on the new measure
	measure = document.getElementById("measure_list").value;
	colorG1();
	colorG2();
}

document.getElementById('file_graph_1').addEventListener('change',
		readGraph1File, false);
document.getElementById('file_graph_2').addEventListener('change',
		readGraph2File, false);

document.getElementById('run_G1').addEventListener('click', colorG1);
document.getElementById('run_G2').addEventListener('click', colorG2);
document.getElementById('k').addEventListener('change', changeK, false);
document.getElementById('measure_list').addEventListener('change',
		changeMeasure, false);