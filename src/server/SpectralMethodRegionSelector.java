package server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Queue;

// This class selects the highest distortion regions.
public class SpectralMethodRegionSelector {
	// Mapping between node ID and Node object.
	private HashMap<Integer, SpectralNode> nodes = null;
	// Store the graph in adjacency list format.
	private HashMap<SpectralNode, HashMap<SpectralNode, Integer>> graph = null;
	// Store list of nodes, populated in the constructor
	// and used in getRegions for sorting nodes
	// based on their distortion values.
	private ArrayList<SpectralNode> nodesList = null;
	// Average delta change of nodes in the region.
	private double regionChangeValue;
	// Six evaluation measures used taking into account the region size.
	private double[] evaluationMeasures;
	private ArrayList<Double[]> regionEvaluationMeasures;
	// Min delta change of a node.
	private double minDelta;
	// Max delta change of a node.
	private double maxDelta;

	/**
	 * RegionSelector constructor which loads the nodes and their distortion
	 * values and convert the graph double[][] to HashMap.
	 * 
	 * @param distortionValues
	 *            set the distortion values of the nodes.
	 * @param graphArray
	 *            graph data where each index corresponds to an edge.
	 */
	public SpectralMethodRegionSelector(double[] distortionValues,
			double[][] graphArray) {
		// Create nodes with the distortion values.
		nodes = new HashMap<Integer, SpectralNode>();
		nodesList = new ArrayList<SpectralNode>();
		int index = 1;
		for (double distoritionValue : distortionValues) {
			SpectralNode node = new SpectralNode(distoritionValue, index);
			nodes.put(index, node);
			nodesList.add(node);
			index++;
		}
		// Load the graph from the graphFile.
		graph = new HashMap<SpectralNode, HashMap<SpectralNode, Integer>>();
		loadGraphArray(graphArray);
		minDelta = Double.MAX_VALUE;
		maxDelta = Double.MIN_VALUE;
	}

	/**
	 * Load graph given the graph array.
	 * 
	 * @param graphArray
	 *            graph array representing the graph.
	 */
	public void loadGraphArray(double[][] graphArray) {
		for (int i = 1; i <= nodes.size(); i++) {
			graph.put(nodes.get(i), new HashMap<SpectralNode, Integer>());
		}
		for (int i = 0; i < graphArray.length; i++) {
			SpectralNode node1 = nodes.get((int) graphArray[i][0]);
			SpectralNode node2 = nodes.get((int) graphArray[i][1]);
			int edgeValue = (int) graphArray[i][2];
			graph.get(node1).put(node2, edgeValue);
			graph.get(node2).put(node1, edgeValue);
		}
	}

	/**
	 * Load graph given the graph file.
	 * 
	 * @param file
	 *            file storing graph data.
	 */
	public void loadGraph(String file) throws IOException {
		for (int i = 1; i <= nodes.size(); i++) {
			graph.put(nodes.get(i), new HashMap<SpectralNode, Integer>());
		}
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = null;
		while ((line = reader.readLine()) != null) {
			if (line.startsWith(",")) {
				line = line.substring(1);
			}
			String[] splits = line.split(","); // i,j,x
			SpectralNode node1 = nodes.get(Integer.parseInt(splits[0]));
			SpectralNode node2 = nodes.get(Integer.parseInt(splits[1]));
			int edgeValue = Integer.parseInt(splits[2]);
			graph.get(node1).put(node2, edgeValue);
			graph.get(node2).put(node1, edgeValue);
		}
		reader.close();
	}

	/**
	 * Perform BFS starting from node n until reaching number of nodes equal to
	 * maxNodes or empty.
	 * 
	 * @param n
	 *            start BFS from node n.
	 * @param maxNodes
	 *            max number of nodes in the BFS graph.
	 * @return BFS graph.
	 */
	public HashMap<SpectralNode, HashSet<SpectralNode>> BFS(SpectralNode n,
			int maxNodes) {
		HashMap<SpectralNode, HashSet<SpectralNode>> BFSGraph = new HashMap<SpectralNode, HashSet<SpectralNode>>();
		HashSet<Integer> found = new HashSet<Integer>();
		Queue<SpectralNode> queue = new LinkedList<SpectralNode>();
		queue.add(n);
		BFSGraph.put(n, new HashSet<SpectralNode>());
		found.add(n.getId());
		while (!queue.isEmpty()) {
			SpectralNode node = queue.poll();
			// Add node to BFSGraph
			BFSGraph.put(node, new HashSet<SpectralNode>());
			if (BFSGraph.size() == maxNodes) {
				break;
			}
			HashMap<SpectralNode, Integer> neighbors = graph.get(node);
			for (SpectralNode neighbor : neighbors.keySet()) {
				if (!found.contains(neighbor.getId())) {
					queue.add(neighbor);
					found.add(neighbor.getId());
				}
			}
		}
		// Copy edges of subgraph that belong to the subgraph.
		for (SpectralNode node : BFSGraph.keySet()) {
			HashSet<SpectralNode> BFSNeighbors = new HashSet<SpectralNode>();
			HashMap<SpectralNode, Integer> neighbors = graph.get(node);
			for (SpectralNode neighbor : neighbors.keySet()) {
				if (BFSGraph.containsKey(neighbor)) {
					BFSNeighbors.add(neighbor);
				}
			}
			BFSGraph.put(node, BFSNeighbors);
		}
		return BFSGraph;
	}

	/**
	 * Start biased BFS from node until the BFS graph number of nodes is equal
	 * to the parameter maxNodes. The biased BFS expands from each node by only
	 * considering the expansion from its top distorted biasedk neighbor nodes
	 * and neglect the other neighbors.
	 * 
	 * @param n
	 *            to start the BFS from.
	 * @param maxNodes
	 *            number of nodes in the BFS graph.
	 * @return BFS graph.
	 **/
	public HashMap<SpectralNode, HashSet<SpectralNode>> BFSBiased(
			SpectralNode n, int maxNodes) {
		HashMap<SpectralNode, HashSet<SpectralNode>> BFSGraph = new HashMap<SpectralNode, HashSet<SpectralNode>>();
		HashSet<Integer> found = new HashSet<Integer>();
		Queue<SpectralNode> queue = new LinkedList<SpectralNode>();
		queue.add(n);
		BFSGraph.put(n, new HashSet<SpectralNode>());
		found.add(n.getId());
		while (!queue.isEmpty()) {
			SpectralNode node = queue.poll();
			// Add node to BFSGraph
			BFSGraph.put(node, new HashSet<SpectralNode>());
			if (BFSGraph.size() == maxNodes) {
				break;
			}
			HashMap<SpectralNode, Integer> neighbors = graph.get(node);
			SpectralNode[] neighborNodes = new SpectralNode[neighbors.size()];
			int index = 0;
			for (SpectralNode neighbor : neighbors.keySet()) {
				neighborNodes[index++] = neighbor;
			}
			// Sort the neighbors of the nodes based on their distortion values
			// from highest to smallest.
			Arrays.sort(neighborNodes);
			int addedCount = 0; // Add only the top BIASEDK neighbors.
			for (int i = 0; i < neighborNodes.length; i++) {
				if (!found.contains(neighborNodes[i].getId())) {
					queue.add(neighborNodes[i]);
					found.add(neighborNodes[i].getId());
					addedCount++;
					if (addedCount == GraphServlet.getBiasedK()) {
						break;
					}
				}
			}
		}
		// Copy edges of subgraph that belong to the subgraph.
		for (SpectralNode node : BFSGraph.keySet()) {
			HashSet<SpectralNode> BFSNeighbors = new HashSet<SpectralNode>();
			HashMap<SpectralNode, Integer> neighbors = graph.get(node);
			for (SpectralNode neighbor : neighbors.keySet()) {
				if (BFSGraph.containsKey(neighbor)) {
					BFSNeighbors.add(neighbor);
				}
			}
			BFSGraph.put(node, BFSNeighbors);
		}
		return BFSGraph;
	}

	/**
	 * Start priority queue BFS from node until the BFS graph number of nodes is
	 * equal to the parameter maxNodes. The priority queue BFS uses a priority
	 * queue instead of traditional queue, where the priority is the distortion
	 * value of the node and the higher this value is, the higher its priority
	 * will be.
	 * 
	 * @param n
	 *            to start the BFS from.
	 * @param maxNodes
	 *            number of nodes in the BFS graph.
	 * @return BFS graph.
	 */
	public HashMap<SpectralNode, HashSet<SpectralNode>> BFSPriorityQueue(
			SpectralNode n, int maxNodes) {
		HashMap<SpectralNode, HashSet<SpectralNode>> BFSGraph = new HashMap<SpectralNode, HashSet<SpectralNode>>();
		HashSet<Integer> found = new HashSet<Integer>();
		PriorityQueue<SpectralNode> queue = new PriorityQueue<SpectralNode>();
		queue.add(n);
		BFSGraph.put(n, new HashSet<SpectralNode>());
		found.add(n.getId());
		while (!queue.isEmpty()) {
			SpectralNode node = queue.poll();
			// Add node to BFSGraph
			BFSGraph.put(node, new HashSet<SpectralNode>());
			if (BFSGraph.size() == maxNodes) {
				break;
			}
			HashMap<SpectralNode, Integer> neighbors = graph.get(node);
			for (SpectralNode neighbor : neighbors.keySet()) {
				if (!found.contains(neighbor.getId())) {
					queue.add(neighbor);
					found.add(neighbor.getId());
				}
			}
		}
		// Copy edges of subgraph that belong to the subgraph.
		for (SpectralNode node : BFSGraph.keySet()) {
			HashSet<SpectralNode> BFSNeighbors = new HashSet<SpectralNode>();
			HashMap<SpectralNode, Integer> neighbors = graph.get(node);
			for (SpectralNode neighbor : neighbors.keySet()) {
				if (BFSGraph.containsKey(neighbor)) {
					BFSNeighbors.add(neighbor);
				}
			}
			BFSGraph.put(node, BFSNeighbors);
		}
		return BFSGraph;
	}

	/**
	 * Given a graph, convert it to String[] format, where each String cell
	 * represents an edge in the following format (edge_source,
	 * edge_destination).
	 * 
	 * @param graph
	 *            to be converted.
	 * @return String[] of edges.
	 */
	public String[] convertGraphToArray(HashMap<SpectralNode, HashSet<SpectralNode>> graph) {
		ArrayList<String> edges = new ArrayList<String>();
		for (SpectralNode node : graph.keySet()) {
			HashSet<SpectralNode> neighbors = graph.get(node);
			for (SpectralNode neighbor : neighbors) {
				edges.add(node.getId() + "," + neighbor.getId());
			}
		}
		String[] edgesArray = new String[edges.size()];
		int index = 0;
		for (String edge : edges) {
			edgesArray[index] = edge;
			index++;
		}
		return edgesArray;
	}

	/**
	 * Get the top-regionNum distortion regions.
	 * 
	 * @param regionNum
	 *            Number of distortion regions to return.
	 * @param maxNodes
	 *            Max number of nodes in each region.
	 * @param overlappingThreshold
	 *            max overlapping in number of nodes between regions.
	 * @param bfsSelection
	 *            0 means BFS, 1 means Biased BFS and 2 means BFS with priority
	 *            queue.
	 * @return HashMap<Integer, String[]> where the key is the region number and
	 *         the values are String array of edges in the following format
	 *         (edge.source, edge.destination).
	 */
	public HashMap<Integer, String[]> getRegions(int regionNum, int maxNodes,
			int bfsSelection) {
		HashMap<Integer, String[]> regions = new HashMap<Integer, String[]>();
		// Sort the nodes based on their distortion values descendingly.
		Collections.sort(nodesList);
		// Nodes selected in any region.
		int index = 1; // Index of current region.
		for (int i = 0; i < nodesList.size(); i++) {
			SpectralNode node = nodesList.get(i);
			// Start BFS from node i.
			HashMap<SpectralNode, HashSet<SpectralNode>> bfsGraph = null;
			if (bfsSelection == 0) {
				bfsGraph = BFS(nodes.get(node.getId()), maxNodes);
			} else if (bfsSelection == 1) {
				bfsGraph = BFSBiased(nodes.get(node.getId()), maxNodes);
			} else {
				bfsGraph = BFSPriorityQueue(nodes.get(node.getId()), maxNodes);
			}
			if (bfsGraph.size() != maxNodes) { // Ensure that each returned
												// region is exactly equal to
												// the
												// max nodes.
				continue;
			}
			// Convert BFSgraph into edges array format.
			String[] edges = convertGraphToArray(bfsGraph);
			regions.put(index, edges);
			index++;
			if (index > regionNum) { // Calculated all regions.
				break;
			}
		}
		return regions;
	}

	/**
	 * Get the top-regionNum distortion regions.
	 * 
	 * @param regionNum
	 *            Number of distortion regions to return.
	 * @param maxNodes
	 *            Max number of nodes in each region.
	 * @param overlappingThreshold
	 *            max overlapping in number of nodes between regions.
	 * @return HashMap<Integer, String[]> where the key is the region number and
	 *         the values are String array of edges in the following format
	 *         (edge.source, edge.destination).
	 */
	public HashMap<String, String[]> getRegionsExhastiveSearch(int regionNum,
			int maxNodes, HashMap<SpectralNode, HashMap<SpectralNode, Integer>> graph1,
			HashMap<SpectralNode, HashMap<SpectralNode, Integer>> graph2,
			HashMap<Integer, SpectralNode> node1Mapping,
			HashMap<Integer, SpectralNode> node2Mapping) {
		ArrayList<SpectralRegion> regionsList = new ArrayList<SpectralRegion>();
		// Nodes selected in any region.
		for (int i = 0; i < nodesList.size(); i++) {
			SpectralNode node = nodesList.get(i);
			// Start BFS from node i.
			HashMap<SpectralNode, HashSet<SpectralNode>> bfsGraph = BFSPriorityQueue(
					nodes.get(node.getId()), maxNodes);
			if (bfsGraph.size() != maxNodes) { // Ensure that each returned
												// region is exactly equal to
												// the
												// max nodes.
				continue;
			}
			double distortionValue = 0;
			double regionSizeGraph1 = 0;
			double regionSizeGraph2 = 0;
			for (SpectralNode bfsNode : bfsGraph.keySet()) {
				distortionValue += bfsNode.getDelta();
				HashMap<SpectralNode, Integer> bfsNodeNbrs1 = graph1.get(node1Mapping
						.get(bfsNode.getId()));
				for (SpectralNode nbr1 : bfsNodeNbrs1.keySet()) {
					if (bfsGraph.containsKey(node2Mapping.get(nbr1.getId()))) {
						regionSizeGraph1++;
					}
				}
				HashMap<SpectralNode, Integer> bfsNodeNbrs2 = graph2.get(node2Mapping
						.get(bfsNode.getId()));
				for (SpectralNode nbr2 : bfsNodeNbrs2.keySet()) {
					if (bfsGraph.containsKey(node2Mapping.get(nbr2.getId()))) {
						regionSizeGraph2++;
					}
				}
			}
			distortionValue = distortionValue
					/ (Math.min(Math.max(1, regionSizeGraph1),
							Math.max(1, regionSizeGraph2)));
			SpectralRegion region = new SpectralRegion(bfsGraph,
					distortionValue, 0, bfsGraph.size());
			regionsList.add(region);
		}
		// Sort the regions based on distortion values from the highest to the
		// smallest.
		Collections.sort(regionsList);
		HashMap<String, String[]> regions = new HashMap<String, String[]>();
		int index = 1; // Index of current region.
		for (SpectralRegion region : regionsList) {
			// Convert BFSgraph into edges array format.
			String[] edges = convertGraphToArray(region.getNodes());
			regions.put(index + " " + region.getDistortionValues(), edges);
			index++;
			if (index > regionNum) {
				break;
			}
		}
		return regions;
	}

	/**
	 * Get subgraphs of graph 1 that correspond to same subgraphs in graph 2.
	 * 
	 * @param graph2Results
	 *            region results of graph 2.
	 * @return corresponding subgraphs in graph 1.
	 */
	public HashMap<Integer, String[]> getMapping(
			HashMap<Integer, String[]> graph2Results,
			HashMap<SpectralNode, HashMap<SpectralNode, Integer>> graph1,
			HashMap<SpectralNode, HashMap<SpectralNode, Integer>> graph2,
			HashMap<Integer, SpectralNode> node2Mapping, int regionMax) {
		evaluationMeasures = new double[6];
		HashMap<Integer, String[]> graph1Results = new HashMap<Integer, String[]>();
		int regionCount = 1;
		for (int region : graph2Results.keySet()) {
			String[] edges = graph2Results.get(region);
			// Retrieve the nodes that appeared in this subgraph2.
			HashSet<SpectralNode> graph2Nodes = new HashSet<SpectralNode>();
			for (String edge : edges) {
				String[] edgeNodes = edge.split(",");
				SpectralNode node1 = nodes.get(Integer.parseInt(edgeNodes[0]));
				SpectralNode node2 = nodes.get(Integer.parseInt(edgeNodes[1]));
				graph2Nodes.add(node1);
				graph2Nodes.add(node2);
			}
			// Construct the corresponding subgraph 1.
			HashMap<SpectralNode, HashSet<SpectralNode>> subgraph1 = new HashMap<SpectralNode, HashSet<SpectralNode>>();
			double distortionValue = 0.0;
			double edgesWithinRegionInGraph1 = 0.0;
			double edgesWithinRegionInGraph2 = 0.0;
			double regionNodesDegreeInGraph1 = 0.0;
			double regionNodesDegreeInGraph2 = 0.0;
			for (SpectralNode node : graph2Nodes) {
				// Get the nodes that are connected to this node in graph2.
				HashMap<SpectralNode, Integer> graph1Nodes = graph1.get(nodes.get(node
						.getId()));
				regionNodesDegreeInGraph1 += graph1Nodes.size();
				HashMap<SpectralNode, Integer> graph2NodeNeibours = graph2
						.get(node2Mapping.get(node.getId()));
				regionNodesDegreeInGraph2 += graph2NodeNeibours.size();
				// Only keep nodes that appear in subgraph1.
				HashSet<SpectralNode> subNodes = new HashSet<SpectralNode>();
				for (SpectralNode graph1Node : graph1Nodes.keySet()) {
					if (graph2Nodes.contains(graph1Node)) {
						subNodes.add(graph1Node);
						edgesWithinRegionInGraph1++;
					}
				}
				subgraph1.put(node, subNodes);
				distortionValue = distortionValue + node.getDelta();
				for (SpectralNode graph2Node : graph2NodeNeibours.keySet()) {
					if (graph2Nodes.contains(nodes.get(graph2Node.getId()))) {
						edgesWithinRegionInGraph2++;
					}
				}
			}
			if (regionCount <= regionMax) { // We only take the highest region
											// in each one of the singular
											// vectors.
				regionChangeValue = distortionValue / graph2Nodes.size();
				evaluationMeasures[0] += distortionValue
						/ Math.max(1, edgesWithinRegionInGraph1);
				evaluationMeasures[1] += distortionValue
						/ Math.max(1, edgesWithinRegionInGraph2);
				evaluationMeasures[2] += distortionValue
						/ Math.min(Math.max(1, edgesWithinRegionInGraph1),
								Math.max(1, edgesWithinRegionInGraph2));
				evaluationMeasures[3] += distortionValue
						/ Math.max(1, regionNodesDegreeInGraph1);
				evaluationMeasures[4] += distortionValue
						/ Math.max(1, regionNodesDegreeInGraph2);
				evaluationMeasures[5] += distortionValue
						/ Math.min(Math.max(1, regionNodesDegreeInGraph1),
								Math.max(1, regionNodesDegreeInGraph2));
			} else {
				break;
			}
			regionCount++;
			// Convert to the array format.
			String[] subGraphArray = convertGraphToArray(subgraph1);
			graph1Results.put(region, subGraphArray);
		}
		return graph1Results;
	}

	/**
	 * Get subgraphs of graph 1 that correspond to same subgraphs in graph 2.
	 * 
	 * @param graph2Results
	 *            region results of graph 2.
	 * @return corresponding subgraphs in graph 1.
	 */
	public HashMap<Double, String[]> getMappingExhastiveSearch(
			HashMap<Double, String[]> graph2Results,
			HashMap<SpectralNode, HashMap<SpectralNode, Integer>> graph1,
			HashMap<SpectralNode, HashMap<SpectralNode, Integer>> graph2,
			HashMap<Integer, SpectralNode> node2Mapping,
			HashMap<Integer, SpectralNode> node1Mapping, int regionMax,
			HashMap<Double, Double> grahResultMapping) {
		evaluationMeasures = new double[6];
		regionEvaluationMeasures = new ArrayList<Double[]>();
		HashMap<Double, String[]> graph1Results = new HashMap<Double, String[]>();
		// sort graph2results
		List<Entry<Double, Double>> SortedGrahResultMapping = entriesSortedByValues(grahResultMapping);
		int regionCount = 1;
		for (Entry<Double, Double> regionPair : SortedGrahResultMapping) {
			Double region = regionPair.getKey();
			String[] edges = graph2Results.get(region);
			// Retrieve the nodes that appeared in this subgraph2.
			HashSet<SpectralNode> graph2Nodes = new HashSet<SpectralNode>();
			for (String edge : edges) {
				String[] edgeNodes = edge.split(",");
				SpectralNode node1 = nodes.get(Integer.parseInt(edgeNodes[0]));
				SpectralNode node2 = nodes.get(Integer.parseInt(edgeNodes[1]));
				graph2Nodes.add(node1);
				graph2Nodes.add(node2);
			}
			// Construct the corresponding subgraph 1.
			HashMap<SpectralNode, HashSet<SpectralNode>> subgraph1 = new HashMap<SpectralNode, HashSet<SpectralNode>>();
			double distortionValue = 0.0;
			double edgesWithinRegionInGraph1 = 0.0;
			double edgesWithinRegionInGraph2 = 0.0;
			double regionNodesDegreeInGraph1 = 0.0;
			double regionNodesDegreeInGraph2 = 0.0;
			for (SpectralNode node : graph2Nodes) {
				// Get the nodes that are connected to this node in graph2.

				HashMap<SpectralNode, Integer> graph1Nodes = graph1.get(node1Mapping
						.get(node.getId()));
				regionNodesDegreeInGraph1 += graph1Nodes.size();
				HashMap<SpectralNode, Integer> graph2NodeNeibours = graph2
						.get(node2Mapping.get(node.getId()));
				regionNodesDegreeInGraph2 += graph2NodeNeibours.size();
				// Only keep nodes that appear in subgraph1.
				HashSet<SpectralNode> subNodes = new HashSet<SpectralNode>();
				for (SpectralNode graph1Node : graph1Nodes.keySet()) {
					if (graph2Nodes.contains(graph1Node)) {
						subNodes.add(graph1Node);
						edgesWithinRegionInGraph1++;
					}
				}
				subgraph1.put(node, subNodes);
				distortionValue = distortionValue + node.getDelta();
				for (SpectralNode graph2Node : graph2NodeNeibours.keySet()) {
					if (graph2Nodes.contains(nodes.get(graph2Node.getId()))) {
						edgesWithinRegionInGraph2++;
					}
				}
			}
			if (regionCount <= regionMax) { // We only take the highest region
											// in each one of the singular
											// vectors.
				regionChangeValue = distortionValue / graph2Nodes.size();
				Double[] measure = new Double[6];
				measure[0] = distortionValue
						/ Math.max(1, edgesWithinRegionInGraph1);
				measure[1] = distortionValue
						/ Math.max(1, edgesWithinRegionInGraph2);
				measure[2] = distortionValue
						/ Math.min(Math.max(1, edgesWithinRegionInGraph1),
								Math.max(1, edgesWithinRegionInGraph2));
				measure[3] = distortionValue
						/ Math.max(1, regionNodesDegreeInGraph1);
				measure[4] = distortionValue
						/ Math.max(1, regionNodesDegreeInGraph2);
				measure[5] = distortionValue
						/ Math.min(Math.max(1, regionNodesDegreeInGraph1),
								Math.max(1, regionNodesDegreeInGraph2));
				for (int i = 0; i < measure.length; i++) {
					evaluationMeasures[i] += measure[i];
				}
				regionEvaluationMeasures.add(measure);
			}
			regionCount++;
			if (regionCount > regionMax) {
				break;
			}
			// Convert to the array format.
			String[] subGraphArray = convertGraphToArray(subgraph1);
			graph1Results.put(region, subGraphArray);
		}
		return graph1Results;
	}

	static <K, V extends Comparable<? super V>> List<Entry<Double, Double>> entriesSortedByValues(
			Map<Double, Double> map) {

		List<Entry<Double, Double>> sortedEntries = new ArrayList<Entry<Double, Double>>(
				map.entrySet());

		Collections.sort(sortedEntries,
				new Comparator<Entry<Double, Double>>() {
					@Override
					public int compare(Entry<Double, Double> e1,
							Entry<Double, Double> e2) {
						return (e2.getValue().compareTo(e1.getValue()));
					}
				});

		return sortedEntries;
	}

	/**
	 * For each node, calculate its delta value as the absolute difference
	 * between its edges in graph1 and graph2.
	 * 
	 * @param graph1
	 *            adjacency list.
	 * @param nodeMapping1
	 *            mapping between node ids and node objects.
	 * @param graph2
	 *            adjacency list.
	 * @param nodeMapping2
	 *            mapping between node ids and node objects.
	 */
	public void calculateDeltaGraph(
			HashMap<SpectralNode, HashMap<SpectralNode, Integer>> graph1,
			HashMap<Integer, SpectralNode> nodeMapping1,
			HashMap<SpectralNode, HashMap<SpectralNode, Integer>> graph2,
			HashMap<Integer, SpectralNode> nodeMapping2) {
		int test = 0;
		for (SpectralNode node1 : graph1.keySet()) { // For each node in graph1.
			// Get the node neighbors.
			HashMap<SpectralNode, Integer> node1NeighborsInGraph1 = graph1.get(node1);
			// Get the corresponding node in graph2.
			SpectralNode node2 = nodeMapping2.get(node1.getId());
			// Get the node neighbors in graph2.
			HashMap<SpectralNode, Integer> node1NeighborsInGraph2 = graph2.get(node2);
			// Calculate the node delta change.
			int delta = 0;
			for (SpectralNode node1NeighborInGraph1 : node1NeighborsInGraph1.keySet()) {
				int edge1Weight = node1NeighborsInGraph1
						.get(node1NeighborInGraph1);
				SpectralNode node1NeighborInGraph2 = nodeMapping2
						.get(node1NeighborInGraph1.getId());
				if (node1NeighborsInGraph2.containsKey(node1NeighborInGraph2)) {
					int edge2Weight = node1NeighborsInGraph2
							.get(node1NeighborInGraph2);
					delta = delta + Math.abs(edge1Weight - edge2Weight);
				} else {
					delta += edge1Weight;
				}
			}
			for (SpectralNode node1NeighborInGraph2 : node1NeighborsInGraph2.keySet()) {
				SpectralNode node1NeighborInGraph1 = nodeMapping1
						.get(node1NeighborInGraph2.getId());
				int edge2Weight = node1NeighborsInGraph2
						.get(node1NeighborInGraph2);
				if (!node1NeighborsInGraph1.containsKey(node1NeighborInGraph1)) {
					delta += edge2Weight;
				}
			}
			// Set the node delta change.
			test = test + delta;
			node1.setDelta(delta);
			node2.setDelta(delta);
			minDelta = Math.min(node1.getDistortionValue(), minDelta);
			maxDelta = Math.max(node1.getDistortionValue(), maxDelta);
		}
	}

	/**
	 * Remove nodes with delta change less than the threshold.
	 * 
	 * @param threshold
	 *            to remove the nodes based on.
	 */
	public void removeNodesBelowThreshold(double step, int numberOfNodes,
			HashMap<SpectralNode, HashMap<SpectralNode, Integer>> graph1,
			HashMap<Integer, SpectralNode> nodeMapping1, ArrayList<SpectralNode> nodeList1,
			HashMap<SpectralNode, HashMap<SpectralNode, Integer>> graph2,
			HashMap<Integer, SpectralNode> nodeMapping2, ArrayList<SpectralNode> nodeList2) {
		HashSet<SpectralNode> graph1Nodes = new HashSet<SpectralNode>();
		// get graph1 nodes.
		graph1Nodes.addAll(graph1.keySet());
		SpectralNode[] nodes = new SpectralNode[graph1Nodes.size()];
		int index = 0;
		for (SpectralNode node : graph1Nodes) {
			nodes[index++] = node;
		}
		Arrays.sort(nodes, Collections.reverseOrder());
		int position = 0;
		for (SpectralNode node1 : nodes) {
			position++;
			if (position > step * numberOfNodes) {
				break;
			}
			// Get the mapping of node1 in graph2.
			SpectralNode node2 = nodeMapping2.get(node1.getId());
			// nodes
			// below
			// the
			// threshold.
			HashMap<SpectralNode, Integer> node1Nbrs = graph1.get(node1);
			// Remove node1 from graph1.
			graph1.remove(node1);
			nodeMapping1.remove(node1.getId());
			nodeList1.remove(node1);
			// Remove node1 from nodes pointing to it in graph1.
			for (SpectralNode node1Nbr : node1Nbrs.keySet()) {
				HashMap<SpectralNode, Integer> node1NbrNbrs = graph1.get(node1Nbr);
				if (node1NbrNbrs == null) {
					continue;
				}
				node1NbrNbrs.remove(node1);
				graph1.put(node1Nbr, node1NbrNbrs);
			}
			HashMap<SpectralNode, Integer> node2Nbrs = graph2.get(node2);
			// Remove node2 from graph2.
			graph2.remove(node2);
			nodeMapping2.remove(node2.getId());
			nodeList2.remove(node2);
			// Remove node2 from nodes pointing to it in graph2.
			for (SpectralNode node2Nbr : node2Nbrs.keySet()) {
				HashMap<SpectralNode, Integer> node2NbrNbrs = graph2.get(node2Nbr);
				if (node2NbrNbrs == null) {
					continue;
				}
				node2NbrNbrs.remove(node2);
				graph2.put(node2Nbr, node2NbrNbrs);
			}
		}
	}

	/**
	 * Get the graph of RegionSelector class.
	 * 
	 * @return graph of RegionSelector class.
	 */
	public HashMap<SpectralNode, HashMap<SpectralNode, Integer>> getGraph() {
		return graph;
	}

	/**
	 * Get the node mapping.
	 * 
	 * @return node mapping.
	 */
	public HashMap<Integer, SpectralNode> getNodeMapping() {
		return nodes;
	}

	/**
	 * Get the region change value.
	 * 
	 * @return region change value.
	 */
	public double getRegionChangeValue() {
		return regionChangeValue;
	}

	/**
	 * Get the evaluation measures.
	 * 
	 * @return the six evaluation measures.
	 */
	public double[] getEvaluationMeasures() {
		return evaluationMeasures;
	}

	/**
	 * Get the minimum delta change of nodes in the graph.
	 * 
	 * @return minimum delta change of nodes in the graph.
	 */
	public double getMinDelta() {
		return minDelta;
	}

	/**
	 * Get the maximum delta change of nodes in the graph.
	 * 
	 * @return maximum delta change of nodes in the graph.
	 */
	public double getMaxDelta() {
		return maxDelta;
	}

	/**
	 * Get node list.
	 * 
	 * @return node list.
	 */
	public ArrayList<SpectralNode> getNodesList() {
		return nodesList;
	}

	public ArrayList<Double[]> getRegionMeasures() {
		return regionEvaluationMeasures;
	}
}
