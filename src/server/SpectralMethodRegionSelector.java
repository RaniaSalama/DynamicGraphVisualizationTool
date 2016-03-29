package server;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

// This class selects the highest distortion regions.
public class SpectralMethodRegionSelector {
  // Mapping between node ID and Node object.
  private HashMap<Integer, Node> nodes = null;
  // Store the graph in adjacency list format.
  private HashMap<Node, HashMap<Node, Integer>> graph = null;
  // Store list of nodes, populated in the constructor
  // and used in getRegions for sorting nodes
  // based on their distortion values.
  private ArrayList<Node> nodesList = null;
  // Average delta change of nodes in the region.
  private double regionChangeValue;
  // Six evaluation measures used taking into account the region size.
  private double[] evaluationMeasures;
  // Min delta change of a node.
  private double minDelta;
  // Max delta change of a node.
  private double maxDelta;

  /**
   * RegionSelector constructor which loads the nodes and their distortion values and convert the
   * graph double[][] to HashMap.
   * 
   * @param distortionValues set the distortion values of the nodes.
   * @param graphArray graph data where each index corresponds to an edge.
   */
  public SpectralMethodRegionSelector(double[] distortionValues, double[][] graphArray) {
    // Create nodes with the distortion values.
    nodes = new HashMap<Integer, Node>();
    nodesList = new ArrayList<Node>();
    int index = 1;
    for (double distoritionValue : distortionValues) {
      Node node = new Node(distoritionValue, index);
      nodes.put(index, node);
      nodesList.add(node);
      index++;
    }
    // Load the graph from the graphFile.
    graph = new HashMap<Node, HashMap<Node, Integer>>();
    loadGraphArray(graphArray);
    minDelta = Double.MAX_VALUE;
    maxDelta = Double.MIN_VALUE;
  }

  /**
   * Load graph given the graph array.
   * 
   * @param graphArray graph array representing the graph.
   */
  public void loadGraphArray(double[][] graphArray) {
    for (int i = 1; i <= nodes.size(); i++) {
      graph.put(nodes.get(i), new HashMap<Node, Integer>());
    }
    for (int i = 0; i < graphArray.length; i++) {
      Node node1 = nodes.get((int) graphArray[i][0]);
      Node node2 = nodes.get((int) graphArray[i][1]);
      int edgeValue = (int) graphArray[i][2];
      graph.get(node1).put(node2, edgeValue);
      graph.get(node2).put(node1, edgeValue);
    }
  }

  /**
   * Load graph given the graph file.
   * 
   * @param file file storing graph data.
   */
  public void loadGraph(String file) throws IOException {
    for (int i = 1; i <= nodes.size(); i++) {
      graph.put(nodes.get(i), new HashMap<Node, Integer>());
    }
    BufferedReader reader = new BufferedReader(new FileReader(file));
    String line = null;
    while ((line = reader.readLine()) != null) {
      if (line.startsWith(",")) {
        line = line.substring(1);
      }
      String[] splits = line.split(","); // i,j,x
      Node node1 = nodes.get(Integer.parseInt(splits[0]));
      Node node2 = nodes.get(Integer.parseInt(splits[1]));
      int edgeValue = Integer.parseInt(splits[2]);
      graph.get(node1).put(node2, edgeValue);
      graph.get(node2).put(node1, edgeValue);
    }
    reader.close();
  }

  /**
   * Perform BFS starting from node n until reaching number of nodes equal to maxNodes or empty.
   * 
   * @param n start BFS from node n.
   * @param maxNodes max number of nodes in the BFS graph.
   * @return BFS graph.
   */
  public HashMap<Node, HashSet<Node>> BFS(Node n, int maxNodes) {
    HashMap<Node, HashSet<Node>> BFSGraph = new HashMap<Node, HashSet<Node>>();
    HashSet<Integer> found = new HashSet<Integer>();
    Queue<Node> queue = new LinkedList<Node>();
    queue.add(n);
    BFSGraph.put(n, new HashSet<Node>());
    found.add(n.getId());
    while (!queue.isEmpty()) {
      Node node = queue.poll();
      // Add node to BFSGraph
      BFSGraph.put(node, new HashSet<Node>());
      if (BFSGraph.size() == maxNodes) {
        break;
      }
      HashMap<Node, Integer> neighbors = graph.get(node);
      for (Node neighbor : neighbors.keySet()) {
        if (!found.contains(neighbor.getId())) {
          queue.add(neighbor);
          found.add(neighbor.getId());
        }
      }
    }
    // Copy edges of subgraph that belong to the subgraph.
    for (Node node : BFSGraph.keySet()) {
      HashSet<Node> BFSNeighbors = new HashSet<Node>();
      HashMap<Node, Integer> neighbors = graph.get(node);
      for (Node neighbor : neighbors.keySet()) {
        if (BFSGraph.containsKey(neighbor)) {
          BFSNeighbors.add(neighbor);
        }
      }
      BFSGraph.put(node, BFSNeighbors);
    }
    return BFSGraph;
  }


  /**
   * Start biased BFS from node until the BFS graph number of nodes is equal to the parameter
   * maxNodes. The biased BFS expands from each node by only considering the expansion from its top
   * distorted biasedk neighbor nodes and neglect the other neighbors.
   * 
   * @param n to start the BFS from.
   * @param maxNodes number of nodes in the BFS graph.
   * @return BFS graph.
   **/
  public HashMap<Node, HashSet<Node>> BFSBiased(Node n, int maxNodes) {
    HashMap<Node, HashSet<Node>> BFSGraph = new HashMap<Node, HashSet<Node>>();
    HashSet<Integer> found = new HashSet<Integer>();
    Queue<Node> queue = new LinkedList<Node>();
    queue.add(n);
    BFSGraph.put(n, new HashSet<Node>());
    found.add(n.getId());
    while (!queue.isEmpty()) {
      Node node = queue.poll();
      // Add node to BFSGraph
      BFSGraph.put(node, new HashSet<Node>());
      if (BFSGraph.size() == maxNodes) {
        break;
      }
      HashMap<Node, Integer> neighbors = graph.get(node);
      Node[] neighborNodes = new Node[neighbors.size()];
      int index = 0;
      for (Node neighbor : neighbors.keySet()) {
        neighborNodes[index++] = neighbor;
      }
      // Sort the neighbors of the nodes based on their distortion values from highest to smallest.
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
    for (Node node : BFSGraph.keySet()) {
      HashSet<Node> BFSNeighbors = new HashSet<Node>();
      HashMap<Node, Integer> neighbors = graph.get(node);
      for (Node neighbor : neighbors.keySet()) {
        if (BFSGraph.containsKey(neighbor)) {
          BFSNeighbors.add(neighbor);
        }
      }
      BFSGraph.put(node, BFSNeighbors);
    }
    return BFSGraph;
  }

  /**
   * Start priority queue BFS from node until the BFS graph number of nodes is equal to the
   * parameter maxNodes. The priority queue BFS uses a priority queue instead of traditional queue,
   * where the priority is the distortion value of the node and the higher this value is, the higher
   * its priority will be.
   * 
   * @param n to start the BFS from.
   * @param maxNodes number of nodes in the BFS graph.
   * @return BFS graph.
   */
  public HashMap<Node, HashSet<Node>> BFSPriorityQueue(Node n, int maxNodes) {
    HashMap<Node, HashSet<Node>> BFSGraph = new HashMap<Node, HashSet<Node>>();
    HashSet<Integer> found = new HashSet<Integer>();
    PriorityQueue<Node> queue = new PriorityQueue<Node>();
    queue.add(n);
    BFSGraph.put(n, new HashSet<Node>());
    found.add(n.getId());
    while (!queue.isEmpty()) {
      Node node = queue.poll();
      // Add node to BFSGraph
      BFSGraph.put(node, new HashSet<Node>());
      if (BFSGraph.size() == maxNodes) {
        break;
      }
      HashMap<Node, Integer> neighbors = graph.get(node);
      for (Node neighbor : neighbors.keySet()) {
        if (!found.contains(neighbor.getId())) {
          queue.add(neighbor);
          found.add(neighbor.getId());
        }
      }
    }
    // Copy edges of subgraph that belong to the subgraph.
    for (Node node : BFSGraph.keySet()) {
      HashSet<Node> BFSNeighbors = new HashSet<Node>();
      HashMap<Node, Integer> neighbors = graph.get(node);
      for (Node neighbor : neighbors.keySet()) {
        if (BFSGraph.containsKey(neighbor)) {
          BFSNeighbors.add(neighbor);
        }
      }
      BFSGraph.put(node, BFSNeighbors);
    }
    return BFSGraph;
  }

  /**
   * Given a graph, convert it to String[] format, where each String cell represents an edge in the
   * following format (edge_source, edge_destination).
   * 
   * @param graph to be converted.
   * @return String[] of edges.
   */
  public String[] convertGraphToArray(HashMap<Node, HashSet<Node>> graph) {
    ArrayList<String> edges = new ArrayList<String>();
    for (Node node : graph.keySet()) {
      HashSet<Node> neighbors = graph.get(node);
      for (Node neighbor : neighbors) {
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
   * @param regionNum Number of distortion regions to return.
   * @param maxNodes Max number of nodes in each region.
   * @param overlappingThreshold max overlapping in number of nodes between regions.
   * @return HashMap<Integer, String[]> where the key is the region number and the values are String
   *         array of edges in the following format (edge.source, edge.destination).
   */
  public HashMap<Integer, String[]> getRegions(int regionNum, int maxNodes) {
    HashMap<Integer, String[]> regions = new HashMap<Integer, String[]>();
    // Sort the nodes based on their distortion values descendingly.
    Collections.sort(nodesList);
    // Nodes selected in any region.
    int index = 1; // Index of current region.
    for (int i = 0; i < nodesList.size(); i++) {
      Node node = nodesList.get(i);
      // Start BFS from node i.
      HashMap<Node, HashSet<Node>> bfsGraph = BFSPriorityQueue(nodes.get(node.getId()), maxNodes);
      if (bfsGraph.size() != maxNodes) { // Ensure that each returned region is exactly equal to the
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
   * Get subgraphs of graph 1 that correspond to same subgraphs in graph 2.
   * 
   * @param graph2Results region results of graph 2.
   * @return corresponding subgraphs in graph 1.
   */
  public HashMap<Integer, String[]> getMapping(HashMap<Integer, String[]> graph2Results,
      HashMap<Node, HashMap<Node, Integer>> graph1, HashMap<Node, HashMap<Node, Integer>> graph2,
      HashMap<Integer, Node> node2Mapping) {
    HashMap<Integer, String[]> graph1Results = new HashMap<Integer, String[]>();
    for (int region : graph2Results.keySet()) {
      String[] edges = graph2Results.get(region);
      // Retrieve the nodes that appeared in this subgraph2.
      HashSet<Node> graph2Nodes = new HashSet<>();
      for (String edge : edges) {
        String[] edgeNodes = edge.split(",");
        Node node1 = nodes.get(Integer.parseInt(edgeNodes[0]));
        Node node2 = nodes.get(Integer.parseInt(edgeNodes[1]));
        graph2Nodes.add(node1);
        graph2Nodes.add(node2);
      }
      // Construct the corresponding subgraph 1.
      HashMap<Node, HashSet<Node>> subgraph1 = new HashMap<Node, HashSet<Node>>();
      double distortionValue = 0.0;
      double edgesWithinRegionInGraph1 = 0.0;
      double edgesWithinRegionInGraph2 = 0.0;
      double regionNodesDegreeInGraph1 = 0.0;
      double regionNodesDegreeInGraph2 = 0.0;
      for (Node node : graph2Nodes) {
        // Get the nodes that are connected to this node in graph2.
        HashMap<Node, Integer> graph1Nodes = graph1.get(nodes.get(node.getId()));
        regionNodesDegreeInGraph1 += graph1Nodes.size();
        HashMap<Node, Integer> graph2NodeNeibours = graph2.get(node2Mapping.get(node.getId()));
        regionNodesDegreeInGraph2 += graph2NodeNeibours.size();
        // Only keep nodes that appear in subgraph1.
        HashSet<Node> subNodes = new HashSet<>();
        for (Node graph1Node : graph1Nodes.keySet()) {
          if (graph2Nodes.contains(graph1Node)) {
            subNodes.add(graph1Node);
            edgesWithinRegionInGraph1++;
          }
        }
        subgraph1.put(node, subNodes);
        distortionValue = distortionValue + node.getDelta();
        for (Node graph2Node : graph2NodeNeibours.keySet()) {
          if (graph2Nodes.contains(nodes.get(graph2Node.getId()))) {
            edgesWithinRegionInGraph2++;
          }
        }
      }
      if (region == 1) { // We only take the highest region in each one of the singular vectors.
        regionChangeValue = distortionValue / graph2Nodes.size();
        evaluationMeasures = new double[6];
        evaluationMeasures[0] = distortionValue / Math.max(1, edgesWithinRegionInGraph1);
        evaluationMeasures[1] = distortionValue / Math.max(1, edgesWithinRegionInGraph2);
        evaluationMeasures[2] =
            distortionValue
                / Math.min(Math.max(1, edgesWithinRegionInGraph1),
                    Math.max(1, edgesWithinRegionInGraph2));
        evaluationMeasures[3] = distortionValue / Math.max(1, regionNodesDegreeInGraph1);
        evaluationMeasures[4] = distortionValue / Math.max(1, regionNodesDegreeInGraph2);
        evaluationMeasures[5] =
            distortionValue
                / Math.min(Math.max(1, regionNodesDegreeInGraph1),
                    Math.max(1, regionNodesDegreeInGraph2));
      }
      // Convert to the array format.
      String[] subGraphArray = convertGraphToArray(subgraph1);
      graph1Results.put(region, subGraphArray);
    }
    return graph1Results;
  }

  /**
   * For each node, calculate its delta value as the absolute difference between its edges in graph1
   * and graph2.
   * 
   * @param graph1 adjacency list.
   * @param nodeMapping1 mapping between node ids and node objects.
   * @param graph2 adjacency list.
   * @param nodeMapping2 mapping between node ids and node objects.
   */
  public void calculateDeltaGraph(HashMap<Node, HashMap<Node, Integer>> graph1,
      HashMap<Integer, Node> nodeMapping1, HashMap<Node, HashMap<Node, Integer>> graph2,
      HashMap<Integer, Node> nodeMapping2) {
    for (Node node1 : graph1.keySet()) { // For each node in graph1.
      // Get the node neighbors.
      HashMap<Node, Integer> node1NeighborsInGraph1 = graph1.get(node1);
      // Get the corresponding node in graph2.
      Node node2 = nodeMapping2.get(node1.getId());
      // Get the node neighbors in graph2.
      HashMap<Node, Integer> node1NeighborsInGraph2 = graph2.get(node2);
      // Calculate the node delta change.
      int delta = 0;
      for (Node node1NeighborInGraph1 : node1NeighborsInGraph1.keySet()) {
        int edge1Weight = node1NeighborsInGraph1.get(node1NeighborInGraph1);
        Node node1NeighborInGraph2 = nodeMapping2.get(node1NeighborInGraph1.getId());
        if (node1NeighborsInGraph2.containsKey(node1NeighborInGraph2)) {
          int edge2Weight = node1NeighborsInGraph2.get(node1NeighborInGraph2);
          delta = delta + Math.abs(edge1Weight - edge2Weight);
        } else {
          delta += edge1Weight;
        }
      }
      for (Node node1NeighborInGraph2 : node1NeighborsInGraph2.keySet()) {
        Node node1NeighborInGraph1 = nodeMapping1.get(node1NeighborInGraph2.getId());
        int edge2Weight = node1NeighborsInGraph2.get(node1NeighborInGraph2);
        if (!node1NeighborsInGraph1.containsKey(node1NeighborInGraph1)) {
          delta += edge2Weight;
        }
      }
      // Set the node delta change.
      node1.setDelta(delta);
      node2.setDelta(delta);
      minDelta = Math.min(node1.getDistortionValue(), minDelta);
      maxDelta = Math.max(node1.getDistortionValue(), maxDelta);
    }
  }

  /**
   * Remove nodes with delta change less than the threshold.
   * 
   * @param threshold to remove the nodes based on.
   */
  public void removeNodesBelowThreshold(double step, int numberOfNodes, 
      HashMap<Node, HashMap<Node, Integer>> graph1, HashMap<Integer, Node> nodeMapping1,
      ArrayList<Node> nodeList1, HashMap<Node, HashMap<Node, Integer>> graph2,
      HashMap<Integer, Node> nodeMapping2, ArrayList<Node> nodeList2) {
    HashSet<Node> graph1Nodes = new HashSet<Node>();
    // get graph1 nodes.
    graph1Nodes.addAll(graph1.keySet());
    Node[] nodes = new Node[graph1Nodes.size()];
    int index = 0;
    for(Node node : graph1Nodes) {
      nodes[index++] = node;
    }
    Arrays.sort(nodes, Collections.reverseOrder());
    int position = 0;
    for (Node node1 : nodes) {
      position++;
      if (position > step * numberOfNodes) {
        break;
      }
      // Get the mapping of node1 in graph2.
      Node node2 = nodeMapping2.get(node1.getId());
                                                    // nodes
                                                    // below
                                                    // the
                                                    // threshold.
        HashMap<Node, Integer> node1Nbrs = graph1.get(node1);
        // Remove node1 from graph1.
        graph1.remove(node1);
        nodeMapping1.remove(node1.getId());
        nodeList1.remove(node1);
        // Remove node1 from nodes pointing to it in graph1.
        for (Node node1Nbr : node1Nbrs.keySet()) {
          HashMap<Node, Integer> node1NbrNbrs = graph1.get(node1Nbr);
          if (node1NbrNbrs == null) {
            continue;
          }
          node1NbrNbrs.remove(node1);
          graph1.put(node1Nbr, node1NbrNbrs);
        }
        HashMap<Node, Integer> node2Nbrs = graph2.get(node2);
        // Remove node2 from graph2.
        graph2.remove(node2);
        nodeMapping2.remove(node2.getId());
        nodeList2.remove(node2);
        // Remove node2 from nodes pointing to it in graph2.
        for (Node node2Nbr : node2Nbrs.keySet()) {
          HashMap<Node, Integer> node2NbrNbrs = graph2.get(node2Nbr);
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
  public HashMap<Node, HashMap<Node, Integer>> getGraph() {
    return graph;
  }

  /**
   * Get the node mapping.
   * 
   * @return node mapping.
   */
  public HashMap<Integer, Node> getNodeMapping() {
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
  public ArrayList<Node> getNodesList() {
    return nodesList;
  }
}
