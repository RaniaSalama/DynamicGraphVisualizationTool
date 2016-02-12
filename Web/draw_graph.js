/**
 * Original code for drawing graph is taken from http://bl.ocks.org/d3noob
 * and modified by Rania Ibrahim 
 */

var GRAPH1_DIV_NAME = "#graph_1";
var GRAPH2_DIV_NAME = "#graph_2";
var DIV_WIDTH = 500;
var DIV_HEIGHT = 500;
var GRAPH_COLORS_FILE = "data/graph_weights.csv";

var graph1_filename; // graph 1 file name
var graph2_filename; // graph 2 file name
var svg1; // Drawing object for graph 1
var svg2; // Drawing object for graph 2
var nv; // Number of nodes in the graph

/**
 * This function is called when the user chooses graph1's file
 * It draws the graph according to the specified file
 */
function readGraph1File(evt) {
    graph1_filename = evt.target.files[0];
    drawGraph(graph1_filename, svg1, GRAPH1_DIV_NAME);
}

/**
 * This function is called when the user chooses graph2's file
 * It draws the graph according to the specified file 
 */
function readGraph2File(evt) {
    graph2_filename = evt.target.files[0]; 
    drawGraph(graph2_filename, svg2, GRAPH2_DIV_NAME);
}

function drawGraph(file, svg, div_name) {
	var links = [];
	if(svg) { // replace old graph with new one, delete original graph
		svg.selectAll("*").remove();
		svg.remove();
	}
	
    if (file) { // if file exist
      var reader = new FileReader();
      reader.onload = function(e) { 
    	  var contents = e.target.result;
    	  var edges = contents.split('\n');
    	  var nodesIDs = {};
	      for (i = 0; i < edges.length; i++) {
	    	var edge = edges[i].split(',');
	    	var link = {};
	    	link['source'] = edge[0];
	    	link['target'] = edge[1];
	    	links.push(link);
	    	// Count number of nodes in the graph
	    	if(!nodesIDs[edge[0]]) 
	    		nodesIDs[edge[0]] = true;
	    	if(!nodesIDs[edge[1]]) 
	    		nodesIDs[edge[1]] = true;
	    	
    	  }
	      nv = Object.keys(nodesIDs).length; 
	      var nodes = {};
	      // Compute the distinct nodes from the links.
		   links.forEach(function(link) {
		     link.source = nodes[link.source] || (nodes[link.source] = {name: link.source});
		     link.target = nodes[link.target] || (nodes[link.target] = {name: link.target});
		   });
	

		   var force = d3.layout.force()
		       .nodes(d3.values(nodes))
		       .links(links)
		       .size([DIV_WIDTH, DIV_HEIGHT])
		       .linkDistance(60)
		       .charge(-300)
		       .on("tick", tick)
		       .start();

		   svg = d3.select(div_name).append("svg").attr("viewBox", "0 0 "+DIV_WIDTH+" "+DIV_HEIGHT);
		   
		   // Add links to svg
		   var link = svg.selectAll(".link")
		       .data(force.links())
		       .enter().append("line")
		       .attr("class", "link");
		   
		   // Add nodes to svg
		   var node = svg.selectAll(".node")
		       .data(force.nodes())
		       .enter().append("g")
		       .attr("class", "node")
		       .on("mouseover", mouseover)
		       .on("mouseout", mouseout)
		       .call(force.drag);
		   node.append("circle")
		       .attr("r", 10);
		   node.append("text")
		       .attr("x", 12)
		       .attr("dy", ".35em")
		       .text(function(d) { return d.name; });
	
		   
		   function tick() {
		     link
		         .attr("x1", function(d) { return d.source.x; })
		         .attr("y1", function(d) { return d.source.y; })
		         .attr("x2", function(d) { return d.target.x; })
		         .attr("y2", function(d) { return d.target.y; });
	
		     node
		         .attr("transform", function(d) { return "translate(" + d.x + "," + d.y + ")"; });
		   }
	
		   function mouseover() {
		     d3.select(this).select("circle").transition()
		         .duration(750)
		         .attr("r", 16);
		   }
	
		   function mouseout() {
		     d3.select(this).select("circle").transition()
		         .duration(750)
		         .attr("r", 8);
		   }
		   
		   // update svg of each graph
		   if(div_name.localeCompare(GRAPH1_DIV_NAME) == 0) {
			   svg1 = svg;
		   }
		   else {
			   svg2 = svg;
		   }
      };
      reader.readAsText(file);
    } else { 
      alert("Failed to load file");
    }
}


function colorG1(){
	var colors = []; // colors of nodes
	d3.csv(GRAPH_COLORS_FILE, function(data) {
		  data.forEach(function(d) {
				colors[d.node] = d.color;
				if(colors.length == nv+1) {
					 svg1.selectAll(".node").append("circle")
					 	 .attr("r", 10)
				         .style("fill", function(d1) { 
				        	 	return colors[d1.name]; 
				         });
				}
    });});	
}

function colorG2(){
	var colors = []; // colors of nodes
	d3.csv(GRAPH_COLORS_FILE, function(data) {
		  data.forEach(function(d) {
				colors[d.node] = d.color;
				if(colors.length == nv+1) {
					 svg2.selectAll(".node").append("circle")
				    .attr("r", 8)
				    .style("fill", function(d1) { 
				    	return colors[d1.name]; 
				    });
				}
		  });});
}


document.getElementById('file_graph_1').addEventListener('change', readGraph1File, false);
document.getElementById('file_graph_2').addEventListener('change', readGraph2File, false);

document.getElementById('run_G1').addEventListener("click", colorG1);
document.getElementById('run_G2').addEventListener("click", colorG2);

