/**
 * Original code for drawing graph is taken from http://bl.ocks.org/d3noob and
 * modified by Rania Ibrahim.
 */

var GRAPH1_DIV_NAME = "#graph_1"; // Graph 1 div name.
var GRAPH2_DIV_NAME = "#graph_2"; // Graph 2 div name.
var DIV_WIDTH = 500; // Graph 1 and graph 2 div width.
var DIV_HEIGHT = 500; // Graph 1 and graph 2 div height.
// URL of the Java servlet.
var GRAPH_SERVLET_URL = "http://localhost:8080/Dynamic_Graph_Visualization_Juno/GraphServlet?";
var MAX_NUM_NODES = 50; // If the graph has nodes less than MAX_NUM_NODES, then
// draw the whole graph.
var MAX_K = 300; // Max value for the k parameter.
var graph1_data; // Graph 1 content.
var graph2_data; // Graph 2 content.
var svg1; // Drawing object for graph 1.
var svg2; // Drawing object for graph 2.
var num_nodes; // Number of nodes in the graph.
var k; // Value of k parameter.
var measure; // Value of the distortion measure.
var graph1_x = {}; // x position of graph 1.
var graph1_y = {}; // y position of graph 1.
var region; // Stores the number of region to return, 1 means return the most
// highest distortion region.
var colors; // Stores the colors of the nodes.
var prev_region; // Re-draw if the user selected a different region.
var firstDraw; // Whether it is first time to draw the graph.
var user_to_system_ids_mapping = {}; // Mapping between user's node IDs and
// system's node IDs.
var system_to_user_ids_mapping = {}; // Mapping between system's node IDs and
// user's node IDs.
/**
 * This function is called to initialize the graphs when a different graph file
 * is selected.
 */
function initializeGraphs(type) {
	if (type == 1) {// Initialize graph 1.
		graph1_x = {};
	} else {// Initialize graph 2.
		graph1_y = {};
	}
	colors = [];
	region = 1;
	prev_region = 1;
	firstDraw = 1;
}

/**
 * This function is called when the user chooses graph1's file. It draws the
 * graph according to the specified file.
 */
function readGraph1File(evt) {
	// When the user chooses a different graph, then clear graph 1 data and
	// drawing.
	initializeGraphs(1);
	// Load graph 1 data and draw graph 1 if its number of nodes is less than
	// MAX_NUM_NODES.
	loadGraphFile(evt, svg1, GRAPH1_DIV_NAME);
}

/**
 * This function is called when the user chooses graph2's file. It draws the
 * graph according to the specified file.
 */
function readGraph2File(evt) {
	// When the user chooses a different graph, then clear graph 2 data and
	// drawing.
	initializeGraphs(2);
	// Load graph 2 data and draw graph 2 if its number of nodes is less than
	// MAX_NUM_NODES.
	loadGraphFile(evt, svg2, GRAPH2_DIV_NAME);
}

/**
 * This function loads the graph file.
 */
function loadGraphFile(evt, svg, div_name) {
	var file = evt.target.files[0];
	if (file) { // If file exist.
		var reader = new FileReader();
		reader.onload = function(e) {
			var nodes = e.target.result.split('\n');
			var file_content = [];
			var links = []; // Links of the graph.
			num_nodes = 0; // Count number of nodes in the graph.
			// System node id starts from 1.
			var current_system_node_id = 1;
			if (div_name.localeCompare(GRAPH1_DIV_NAME) == 0) {
				// Only initialize the mapping for the first graph.
				// The second graph should follow the first graph mapping.
				user_to_system_ids_mapping = {};
				system_to_user_ids_mapping = {};
			}
			for ( var i = 0; i < nodes.length; i++) {
				var node = nodes[i].split('[');
				var node_attributes = node[0].split(',');
				var node_id = node_attributes[0];
				var trunk_amount = 2; // Always remove ]\r.
				if (i == nodes.length - 1)
					trunk_amount = 1; // Last line just remove ].
				var node_links = node[1].substring(0,
						node[1].length - trunk_amount).split(',');
				var system_node_id = user_to_system_ids_mapping[node_id];
				if (system_node_id == undefined) { // Not added to the mapping
					// yet.
					user_to_system_ids_mapping[node_id] = current_system_node_id;
					system_to_user_ids_mapping[current_system_node_id] = node_id;
					system_node_id = current_system_node_id;
					current_system_node_id++;
				}
				for ( var j = 0; j < node_links.length; j++) {
					if (node_links[j].length == 0)
						continue;
					var node_link = node_links[j].split(':');
					var link = {};
					link['source'] = node_id;
					link['target'] = node_link[0];
					links.push(link);
					var system_node_link_id = user_to_system_ids_mapping[node_link[0]];
					if (system_node_link_id == undefined) { // Not added to the
						// mapping yet.
						user_to_system_ids_mapping[node_link[0]] = current_system_node_id;
						system_to_user_ids_mapping[current_system_node_id] = node_link[0];
						system_node_link_id = current_system_node_id;
						current_system_node_id++;
					}
					file_content.push(system_node_id + ","
							+ system_node_link_id + "," + node_link[1] + "-");
				}
				num_nodes++;
			}
			if (num_nodes < MAX_NUM_NODES) {
				drawGraph(links, svg, div_name);
			}
			if (div_name.localeCompare(GRAPH1_DIV_NAME) == 0) {
				graph1_data = file_content;
			} else {
				graph2_data = file_content;
			}
			// Change max of k to be num_nodes.
			document.getElementById("k").max = Math.min(num_nodes, MAX_K);
			// Set current values for k and measure.
			//k = documentgetElementById("k").min;
			k = Math.min(12, num_nodes);
			measure = document.getElementById("measure_list").value;
		};
		reader.readAsText(file);
	} else {
		alert("Failed to load file");
	}
}

/**
 * Draw the graph given its links.
 */
function drawGraph(links, svg, div_name) {
	if (svg) { // Remove old drawing.
		svg.selectAll("*").remove();
		svg.remove();
	}
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
	var force = d3.layout.force().nodes(d3.values(nodes)).links(links).size(
			[ DIV_WIDTH, DIV_HEIGHT ]).linkDistance(60).charge(-300).on("tick",
			tick).start();
	svg = d3.select(div_name).append("svg").attr("viewBox",
			"0 0 " + DIV_WIDTH + " " + DIV_HEIGHT);
	// Add links to svg.
	var link = svg.selectAll(".link").data(force.links()).enter()
			.append("line").attr("class", "link");
	// Add nodes to svg.
	var node = svg.selectAll(".node").data(force.nodes()).enter().append("g")
			.attr("class", "node").on("mouseover", mouseover).on("mouseout",
					mouseout).call(force.drag);
	node.append("circle").attr("r", 10);
	node.append("text").attr("x", 12).attr("dy", ".35em").text(function(d) {
		return d.name;
	});

	function tick() {
		node.attr("transform", function(d) {
			// If the graph is graph1, then store its x and y positions.
			if (div_name.localeCompare(GRAPH1_DIV_NAME) == 0) {
				graph1_x[d.name] = d.x;
				graph1_y[d.name] = d.y;
			} else {
				// Set graph 2 nodes to same position as graph 1.
				if (graph1_x[d.name] == undefined) { // There is a node in
					// graph 2 not in graph
					// 1.
					d.x = Math.random()*DIV_WIDTH;
					d.y = Math.random()*DIV_HEIGHT;
					graph1_x[d.name] = d.x;
					graph1_y[d.name] = d.y;
				} else {
					d.x = graph1_x[d.name];
					d.y = graph1_y[d.name];
				}
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
		d3.select(this).select("circle").transition().duration(750).attr("r",
				16);
	}

	function mouseout() {
		d3.select(this).select("circle").transition().duration(750)
				.attr("r", 8);
	}
	// Update svg of each graph.
	if (div_name.localeCompare(GRAPH1_DIV_NAME) == 0) {
		svg1 = svg;
	} else {
		svg2 = svg;
	}
	firstDraw = 0;
}

/**
 * This function is called when the user presses run to call the servlet and run
 * the Matlab code.
 */
function colorGraph() {
	// Call the servlet to calculate the distortion colors.
	var url = GRAPH_SERVLET_URL;
	var params = "graph1file=" + graph1_data + "&graph2file=" + graph2_data
			+ "&k=" + k + "&measure=" + measure + "&region=" + region;
	var http;
	if (window.XMLHttpRequest) {
		// Code for IE7+, Firefox, Chrome, Opera, Safari.
		http = new XMLHttpRequest();
	} else {// Code for IE6, IE5.
		http = new ActiveXObject("Microsoft.XMLHTTP");
	}
	http.open("POST", url, true);
	// Send the proper header information along with the request.
	http.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
	http.setRequestHeader("Content-length", params.length);
	http.setRequestHeader("Connection", "close");
	http.onreadystatechange = function() {
		// Get the servlet results.
		if (http.readyState == 4 && http.status == 200) {
			var response = http.responseText;
			parseResponse(response);
			// Color graph1 with the result.
			svg1.selectAll(".node").append("circle").attr("r", 10).style(
					"fill", function(d1) {
						// As the colors returned are based on the system IDs,
						// convert the user ID to the system ID and get its
						// color.
						return colors[user_to_system_ids_mapping[d1.name]];
					});
			// Color graph2 with the result.
			svg2.selectAll(".node").append("circle").attr("r", 10).style(
					"fill", function(d1) {
						// As the colors returned are based on the system IDs,
						// convert the user ID to the system ID and get its
						// color.
						
						return colors[user_to_system_ids_mapping[d1.name]];
					});
		}
	};
	http.send(params); // Send the parameters.
}

/**
 * Parse the servlet response.
 */
function parseResponse(response) {
	var distorition = response.split('_');
	// If the region changed or the first time to draw the graph
	// then re-draw the graph.
	if (region != prev_region || firstDraw) {
		prev_region = region;
		var graph1 = distorition[1].split('-');
		var links1 = [];
		for ( var i = 0; i < graph1.length; i++) {
			if (graph1[i].length == 0) {
				continue;
			}
			var edge = graph1[i].split(",");
			var link = {};
			// As the returned edges are based on the system ID,
			// convert them to user ID for visualization.
			link['source'] = system_to_user_ids_mapping[edge[0]];
			link['target'] = system_to_user_ids_mapping[edge[1]];
			links1.push(link);
		}
		drawGraph(links1, svg1, GRAPH1_DIV_NAME);
		var graph2 = distorition[2].split('-');
		var links2 = [];
		for ( var i = 0; i < graph2.length; i++) {
			if (graph2[i].length == 0) {
				continue;
			}
			var edge = graph2[i].split(",");
			var link = {};
			// As the returned edges are based on the system ID,
			// convert them to user ID for visualization.
			link['source'] = system_to_user_ids_mapping[edge[0]];
			link['target'] = system_to_user_ids_mapping[edge[1]];
			links2.push(link);

		}
		drawGraph(links2, svg2, GRAPH2_DIV_NAME);
	}
	// Change the colors array based on the servlet result.
	colors = [];
	var colors_array = distorition[0].split(',');
	for ( var i = 0; i < num_nodes; i++) {
		colors[i + 1] = colors_array[i];
	}
}

/**
 * When the user changes the value of k, call the function to re-color the two
 * graphs.
 */
function changeK() {
	// When change k value, set the new value and re-color the graphs
	// based on the new k value.
	k = document.getElementById("k").value;
	colorGraph();
}

/**
 * When the user changes the measure, call the function to re-color the two
 * graphs.
 */
function changeMeasure() {
	// When change the measure, store the new value and re-color the graphs
	// based on the new measure.
	measure = document.getElementById("measure_list").value;
	colorGraph();
}

/**
 * When the user choose a different area to view, call the function to re-draw
 * the new area.
 */
function changeArea(element) {
	firstDraw = 1; // Always re-draw when user choose the area.
	prev_region = region;
	region = this.id;
	colorGraph();
}

document.getElementById('file_graph_1').addEventListener('change',
		readGraph1File, false);
document.getElementById('file_graph_2').addEventListener('change',
		readGraph2File, false);
document.getElementById('run').addEventListener('click', colorGraph);
document.getElementById('k').addEventListener('change', changeK, false);
document.getElementById('measure_list').addEventListener('change',
		changeMeasure, false);
document.getElementById('1').addEventListener('click', changeArea);
document.getElementById('2').addEventListener('click', changeArea);
document.getElementById('3').addEventListener('click', changeArea);
document.getElementById('4').addEventListener('click', changeArea);
document.getElementById('5').addEventListener('click', changeArea);
document.getElementById('6').addEventListener('click', changeArea);
document.getElementById('7').addEventListener('click', changeArea);
document.getElementById('8').addEventListener('click', changeArea);
document.getElementById('9').addEventListener('click', changeArea);
document.getElementById('10').addEventListener('click', changeArea);