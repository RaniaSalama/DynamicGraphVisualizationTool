package server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

public class RegionSelector {

	HashMap<Integer, Node> nodes; // mapping between node ID and Node object
	HashMap<Node, HashSet<Node>> graph; // store the graph in adjacent list
										// format
	ArrayList<Node> nodesList; // store list of nodes for sorting

	public RegionSelector(double[] distortionValues, double[][] graphArray) {
		// create nodes with the distortion values
		nodes = new HashMap<Integer, Node>();
		nodesList = new ArrayList<Node>();
		int index = 1;
		for (double distoritionValue : distortionValues) {
			Node node = new Node(distoritionValue, index);
			nodes.put(index, node);
			nodesList.add(node);
			index++;
		}

		// load the graph from the graphFile
		graph = new HashMap<Node, HashSet<Node>>();
		loadGraphArray(graphArray);

	}

	/**
	 * load graph given the graph array
	 * 
	 * @param graphArray
	 *            graph array representing the graph
	 */
	public void loadGraphArray(double[][] graphArray) {
		for (int i = 1; i <= nodes.size(); i++) {
			graph.put(nodes.get(i), new HashSet<Node>());
		}
		for (int i = 0; i < graphArray.length; i++) {
			Node node1 = nodes.get((int)graphArray[i][0]);
			Node node2 = nodes.get((int)graphArray[i][1]);
			graph.get(node1).add(node2);
			graph.get(node2).add(node1);
		}
	}

	/**
	 * load graph given the graph file
	 * 
	 * @param file
	 *            file storing graph data
	 */
	public void loadGraph(String file) {
		for (int i = 1; i <= nodes.size(); i++) {
			graph.put(nodes.get(i), new HashSet<Node>());
		}
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith(","))
					line = line.substring(1);
				String[] splits = line.split(","); // i,j,1
				Node node1 = nodes.get(Integer.parseInt(splits[0]));
				Node node2 = nodes.get(Integer.parseInt(splits[1]));
				graph.get(node1).add(node2);
				graph.get(node2).add(node1);
			}
			reader.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Perform BFS starting from node n until reaching number of nodes equal to
	 * maxNodes or empty
	 * 
	 * @param n
	 *            start BFS from node n
	 * @param maxNodes
	 *            max number of nodes in the BFS graph
	 * @return BFS graph
	 */
	public HashMap<Node, HashSet<Node>> BFS(Node n, int maxNodes) {
		HashMap<Node, HashSet<Node>> BFSGraph = new HashMap<Node, HashSet<Node>>();
		boolean[] found = new boolean[graph.size() + 1];
		Queue<Node> queue = new LinkedList<Node>();
		queue.add(n);
		BFSGraph.put(n, new HashSet<Node>());
		maxNodes--;
		found[n.getId()] = true;
		int nodesNum = 0;
		while (!queue.isEmpty() && nodesNum < maxNodes) {
			Node node = queue.poll();
			nodesNum++;
			// add node to BFSGraph
			BFSGraph.put(node, new HashSet<Node>());
			HashSet<Node> neighbors = graph.get(node);
			for (Node neighbor : neighbors) {
				if (!found[neighbor.getId()]) {
					queue.add(neighbor);
					found[neighbor.getId()] = true;
				}
			}
		}
		// copy edges of subgraph that belong to the subgraph
		for (Node node : BFSGraph.keySet()) {
			HashSet<Node> BFSNeighbors = new HashSet<Node>();
			HashSet<Node> neighbors = graph.get(node);
			for (Node neighbor : neighbors) {
				if (BFSGraph.containsKey(neighbor))
					BFSNeighbors.add(neighbor);
			}
			BFSGraph.put(node, BFSNeighbors);
		}

		return BFSGraph;
	}

	/**
	 * Given a graph, convert it to String[] format, where each String cell
	 * represents an edge in the following format (edge_source,
	 * edge_destination)
	 * 
	 * @param graph
	 *            to be converted
	 * @return String[] of edges
	 */
	public String[] convertGraphtoArr(HashMap<Node, HashSet<Node>> graph) {
		ArrayList<String> edges = new ArrayList<String>();
		for (Node node : graph.keySet()) {
			HashSet<Node> neighbors = graph.get(node);
			for (Node neighbor : neighbors) {
				edges.add(node.getId() + "," + neighbor.getId());
			}
		}
		String[] edgesArr = new String[edges.size()];
		int index = 0;
		for (String edge : edges) {
			edgesArr[index] = edge;
			index++;
		}
		return edgesArr;
	}

	/**
	 * Get the top-regionNum distortion regions
	 * 
	 * @param regionNum
	 *            Number of distortion regions to return
	 * @param maxNodes
	 *            Max number of nodes in each region
	 * @param overlappingThreshol
	 *            max overlapping in number of nodes between regions
	 * @return HashMap<Integer, String[]> where the key is the region number and
	 *         the values are String array of edges in the following format
	 *         (edge.source, edge.destination)
	 */
	public HashMap<Integer, String[]> getRegions(int regionNum, int maxNodes,
			double overlappingThreshol) {
		HashMap<Integer, String[]> regions = new HashMap<Integer, String[]>();
		Collections.sort(nodesList); // sort the nodes based on their distortion
										// values descendingly
		HashSet<Node> selectedNodes = new HashSet<Node>(); // nodes selected in
															// any region
		int index = 1; // index of current region
		for (int i = 0; i < nodesList.size(); i++) {
			Node node = nodesList.get(i);
			if (selectedNodes.contains(node)) // if this node already appeared
												// in a region
				continue;
			// start BFS from node i
			HashMap<Node, HashSet<Node>> bfsgraph = BFS(
					nodes.get(node.getId()), maxNodes);

			// calculate overlap between regions, to ensure returning different
			// regions
			double overlap = 0;
			for (Node bfsnode : bfsgraph.keySet()) {
				if (selectedNodes.contains(bfsnode))
					overlap++;
			}
			overlap = overlap / bfsgraph.size();
			if (overlap > overlappingThreshol)
				continue;
			// convert BFSgraph into edges array format
			String[] edges = convertGraphtoArr(bfsgraph);
			regions.put(index, edges);
			index++;
			selectedNodes.addAll(bfsgraph.keySet());
			if (index > regionNum) // calculated all regions
				break;
		}

		return regions;
	}

	/**
	 * Get subgraphs of graph 1 that correspond to same subgraphs in graph2
	 * 
	 * @param graph2Results
	 *            region results of graph 2
	 * @return corresponding subgraphs in graph 1
	 */
	public HashMap<Integer, String[]> getMapping(
			HashMap<Integer, String[]> graph2Results) {
		HashMap<Integer, String[]> graph1Results = new HashMap<Integer, String[]>();
		for (int region : graph2Results.keySet()) {
			String[] edges = graph2Results.get(region);
			// Retrieve the nodes that appeared in this subgraph2
			HashSet<Node> graph2Nodes = new HashSet<>();
			for (String edge : edges) {
				String[] edgeNodes = edge.split(",");
				graph2Nodes.add(nodes.get(Integer.parseInt(edgeNodes[0])));
				graph2Nodes.add(nodes.get(Integer.parseInt(edgeNodes[1])));
			}
			// Construct the corresponding subgraph 1
			HashMap<Node, HashSet<Node>> subgraph1 = new HashMap<Node, HashSet<Node>>();
			for (Node node : graph2Nodes) {
				// get the nodes that are connected to this node in graph2
				HashSet<Node> graph1Nodes = graph.get(nodes.get(node.getId()));
				// Only keep nodes that appear in subgraph1
				HashSet<Node> subNodes = new HashSet<>();
				for (Node graph1Node : graph1Nodes) {
					if (graph2Nodes.contains(graph1Node))
						subNodes.add(graph1Node);
				}
				subgraph1.put(node, subNodes);
			}
			// convert to the array format
			String[] subGraphArray = convertGraphtoArr(subgraph1);
			graph1Results.put(region, subGraphArray);
		}
		return graph1Results;
	}

}
