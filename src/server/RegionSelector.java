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

// This class selects the highest distortion regions.
public class RegionSelector {
  // Mapping between node ID and Node object.
  private HashMap<Integer, Node> nodes = null;
  // Store the graph in adjacency list format.
  private HashMap<Node, HashSet<Node>> graph = null;
  // Store list of nodes, populated in the constructor
  // and used in getRegions for sorting nodes
  // based on their distortion values.
  private ArrayList<Node> nodesList = null;

  /**
   * RegionSelector constructor which loads the nodes and their distortion values and convert the
   * graph double[][] to HashMap.
   * 
   * @param distortionValues set the distortion values of the nodes.
   * @param graphArray graph data where each index corresponds to an edge.
   */
  public RegionSelector(double[] distortionValues, double[][] graphArray) {
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
    graph = new HashMap<Node, HashSet<Node>>();
    loadGraphArray(graphArray);
  }

  /**
   * Load graph given the graph array.
   * 
   * @param graphArray graph array representing the graph.
   */
  public void loadGraphArray(double[][] graphArray) {
    for (int i = 1; i <= nodes.size(); i++) {
      graph.put(nodes.get(i), new HashSet<Node>());
    }
    for (int i = 0; i < graphArray.length; i++) {
      Node node1 = nodes.get((int) graphArray[i][0]);
      Node node2 = nodes.get((int) graphArray[i][1]);
      graph.get(node1).add(node2);
      graph.get(node2).add(node1);
    }
  }

  /**
   * Load graph given the graph file.
   * 
   * @param file file storing graph data.
   */
  public void loadGraph(String file) throws IOException {
    for (int i = 1; i <= nodes.size(); i++) {
      graph.put(nodes.get(i), new HashSet<Node>());
    }
    BufferedReader reader = new BufferedReader(new FileReader(file));
    String line = null;
    while ((line = reader.readLine()) != null) {
      if (line.startsWith(",")) {
        line = line.substring(1);
      }
      String[] splits = line.split(","); // i,j,1
      Node node1 = nodes.get(Integer.parseInt(splits[0]));
      Node node2 = nodes.get(Integer.parseInt(splits[1]));
      graph.get(node1).add(node2);
      graph.get(node2).add(node1);
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
      // Add node to BFSGraph
      BFSGraph.put(node, new HashSet<Node>());
      HashSet<Node> neighbors = graph.get(node);
      for (Node neighbor : neighbors) {
        if (!found[neighbor.getId()]) {
          queue.add(neighbor);
          found[neighbor.getId()] = true;
        }
      }
    }
    // Copy edges of subgraph that belong to the subgraph.
    for (Node node : BFSGraph.keySet()) {
      HashSet<Node> BFSNeighbors = new HashSet<Node>();
      HashSet<Node> neighbors = graph.get(node);
      for (Node neighbor : neighbors) {
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
  public HashMap<Integer, String[]> getRegions(int regionNum, int maxNodes,
      double overlappingThreshold) {
    HashMap<Integer, String[]> regions = new HashMap<Integer, String[]>();
    // Sort the nodes based on their distortion values descendingly.
    Collections.sort(nodesList);
    // Nodes selected in any region.
    HashSet<Node> selectedNodes = new HashSet<Node>();
    int index = 1; // Index of current region.
    for (int i = 0; i < nodesList.size(); i++) {
      Node node = nodesList.get(i);
      // If this node already appeared in a region.
      if (selectedNodes.contains(node)) {
        continue;
      }
      // Start BFS from node i.
      HashMap<Node, HashSet<Node>> bfsGraph = BFS(nodes.get(node.getId()), maxNodes);
      // Calculate overlap between regions, to ensure returning different
      // regions.
      double overlap = 0;
      for (Node bfsnode : bfsGraph.keySet()) {
        if (selectedNodes.contains(bfsnode)) {
          overlap++;
        }
      }
      overlap = overlap / bfsGraph.size();
      if (overlap > overlappingThreshold) {
        continue;
      }
      // Convert BFSgraph into edges array format.
      String[] edges = convertGraphToArray(bfsGraph);
      if(edges.length == 0)
        continue;
      regions.put(index, edges);
      index++;
      selectedNodes.addAll(bfsGraph.keySet());
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
      HashMap<Node, HashSet<Node>> graph1) {
    HashMap<Integer, String[]> graph1Results = new HashMap<Integer, String[]>();
    for (int region : graph2Results.keySet()) {
      String[] edges = graph2Results.get(region);
      // Retrieve the nodes that appeared in this subgraph2.
      HashSet<Node> graph2Nodes = new HashSet<>();
      for (String edge : edges) {
        String[] edgeNodes = edge.split(",");
        graph2Nodes.add(nodes.get(Integer.parseInt(edgeNodes[0])));
        graph2Nodes.add(nodes.get(Integer.parseInt(edgeNodes[1])));
      }
      // Construct the corresponding subgraph 1.
      HashMap<Node, HashSet<Node>> subgraph1 = new HashMap<Node, HashSet<Node>>();
      for (Node node : graph2Nodes) {
        // Get the nodes that are connected to this node in graph2.
        HashSet<Node> graph1Nodes = graph1.get(nodes.get(node.getId()));
        // Only keep nodes that appear in subgraph1.
        HashSet<Node> subNodes = new HashSet<>();
        for (Node graph1Node : graph1Nodes) {
          if (graph2Nodes.contains(graph1Node)) {
            subNodes.add(graph1Node);
          }
        }
        subgraph1.put(node, subNodes);
      }
      // Convert to the array format.
      String[] subGraphArray = convertGraphToArray(subgraph1);
      graph1Results.put(region, subGraphArray);
    }
    return graph1Results;
  }

  /**
   * Get the graph of RegionSelector class.
   * 
   * @return graph of RegionSelector class.
   */
  public HashMap<Node, HashSet<Node>> getGraph() {
    return graph;
  }

}
