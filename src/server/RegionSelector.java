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
  // Mapping between SpectralNode ID and SpectralNode object.
  private HashMap<Integer, SpectralNode> SpectralNodes = null;
  // Store the graph in adjacency list format.
  private HashMap<SpectralNode, HashSet<SpectralNode>> graph = null;
  // Store list of SpectralNodes, populated in the constructor
  // and used in getRegions for sorting SpectralNodes
  // based on their distortion values.
  private ArrayList<SpectralNode> SpectralNodesList = null;

  /**
   * RegionSelector constructor which loads the SpectralNodes and their distortion values and convert the
   * graph double[][] to HashMap.
   * 
   * @param distortionValues set the distortion values of the SpectralNodes.
   * @param graphArray graph data where each index corresponds to an edge.
   */
  public RegionSelector(double[] distortionValues, double[][] graphArray) {
    // Create SpectralNodes with the distortion values.
    SpectralNodes = new HashMap<Integer, SpectralNode>();
    SpectralNodesList = new ArrayList<SpectralNode>();
    int index = 1;
    for (double distoritionValue : distortionValues) {
      SpectralNode SpectralNode = new SpectralNode(distoritionValue, index);
      SpectralNodes.put(index, SpectralNode);
      SpectralNodesList.add(SpectralNode);
      index++;
    }
    // Load the graph from the graphFile.
    graph = new HashMap<SpectralNode, HashSet<SpectralNode>>();
    loadGraphArray(graphArray);
  }

  /**
   * Load graph given the graph array.
   * 
   * @param graphArray graph array representing the graph.
   */
  public void loadGraphArray(double[][] graphArray) {
    for (int i = 1; i <= SpectralNodes.size(); i++) {
      graph.put(SpectralNodes.get(i), new HashSet<SpectralNode>());
    }
    for (int i = 0; i < graphArray.length; i++) {
      SpectralNode SpectralNode1 = SpectralNodes.get((int) graphArray[i][0]);
      SpectralNode SpectralNode2 = SpectralNodes.get((int) graphArray[i][1]);
      graph.get(SpectralNode1).add(SpectralNode2);
      graph.get(SpectralNode2).add(SpectralNode1);
    }
  }

  /**
   * Load graph given the graph file.
   * 
   * @param file file storing graph data.
   */
  public void loadGraph(String file) throws IOException {
    for (int i = 1; i <= SpectralNodes.size(); i++) {
      graph.put(SpectralNodes.get(i), new HashSet<SpectralNode>());
    }
    BufferedReader reader = new BufferedReader(new FileReader(file));
    String line = null;
    while ((line = reader.readLine()) != null) {
      if (line.startsWith(",")) {
        line = line.substring(1);
      }
      String[] splits = line.split(","); // i,j,1
      SpectralNode SpectralNode1 = SpectralNodes.get(Integer.parseInt(splits[0]));
      SpectralNode SpectralNode2 = SpectralNodes.get(Integer.parseInt(splits[1]));
      graph.get(SpectralNode1).add(SpectralNode2);
      graph.get(SpectralNode2).add(SpectralNode1);
    }
    reader.close();
  }

  /**
   * Perform BFS starting from SpectralNode n until reaching number of SpectralNodes equal to maxSpectralNodes or empty.
   * 
   * @param n start BFS from SpectralNode n.
   * @param maxSpectralNodes max number of SpectralNodes in the BFS graph.
   * @return BFS graph.
   */
  public HashMap<SpectralNode, HashSet<SpectralNode>> BFS(SpectralNode n, int maxSpectralNodes) {
    HashMap<SpectralNode, HashSet<SpectralNode>> BFSGraph = new HashMap<SpectralNode, HashSet<SpectralNode>>();
    boolean[] found = new boolean[graph.size() + 1];
    Queue<SpectralNode> queue = new LinkedList<SpectralNode>();
    queue.add(n);
    BFSGraph.put(n, new HashSet<SpectralNode>());
    maxSpectralNodes--;
    found[n.getId()] = true;
    int SpectralNodesNum = 0;
    while (!queue.isEmpty() && SpectralNodesNum < maxSpectralNodes) {
      SpectralNode SpectralNode = queue.poll();
      SpectralNodesNum++;
      // Add SpectralNode to BFSGraph
      BFSGraph.put(SpectralNode, new HashSet<SpectralNode>());
      HashSet<SpectralNode> neighbors = graph.get(SpectralNode);
      for (SpectralNode neighbor : neighbors) {
        if (!found[neighbor.getId()]) {
          queue.add(neighbor);
          found[neighbor.getId()] = true;
        }
      }
    }
    // Copy edges of subgraph that belong to the subgraph.
    for (SpectralNode SpectralNode : BFSGraph.keySet()) {
      HashSet<SpectralNode> BFSNeighbors = new HashSet<SpectralNode>();
      HashSet<SpectralNode> neighbors = graph.get(SpectralNode);
      for (SpectralNode neighbor : neighbors) {
        if (BFSGraph.containsKey(neighbor)) {
          BFSNeighbors.add(neighbor);
        }
      }
      BFSGraph.put(SpectralNode, BFSNeighbors);
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
  public String[] convertGraphToArray(HashMap<SpectralNode, HashSet<SpectralNode>> graph) {
    ArrayList<String> edges = new ArrayList<String>();
    for (SpectralNode SpectralNode : graph.keySet()) {
      HashSet<SpectralNode> neighbors = graph.get(SpectralNode);
      for (SpectralNode neighbor : neighbors) {
        edges.add(SpectralNode.getId() + "," + neighbor.getId());
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
   * @param maxSpectralNodes Max number of SpectralNodes in each region.
   * @param overlappingThreshold max overlapping in number of SpectralNodes between regions.
   * @return HashMap<Integer, String[]> where the key is the region number and the values are String
   *         array of edges in the following format (edge.source, edge.destination).
   */
  public HashMap<Integer, String[]> getRegions(int regionNum, int maxSpectralNodes,
      double overlappingThreshold) {
    HashMap<Integer, String[]> regions = new HashMap<Integer, String[]>();
    // Sort the SpectralNodes based on their distortion values descendingly.
    Collections.sort(SpectralNodesList);
    // SpectralNodes selected in any region.
    HashSet<SpectralNode> selectedSpectralNodes = new HashSet<SpectralNode>();
    int index = 1; // Index of current region.
    for (int i = 0; i < SpectralNodesList.size(); i++) {
      SpectralNode SpectralNode = SpectralNodesList.get(i);
      // If this SpectralNode already appeared in a region.
      if (selectedSpectralNodes.contains(SpectralNode)) {
        continue;
      }
      // Start BFS from SpectralNode i.
      HashMap<SpectralNode, HashSet<SpectralNode>> bfsGraph = BFS(SpectralNodes.get(SpectralNode.getId()), maxSpectralNodes);
      // Calculate overlap between regions, to ensure returning different
      // regions.
      double overlap = 0;
      for (SpectralNode bfsSpectralNode : bfsGraph.keySet()) {
        if (selectedSpectralNodes.contains(bfsSpectralNode)) {
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
      selectedSpectralNodes.addAll(bfsGraph.keySet());
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
      HashMap<SpectralNode, HashSet<SpectralNode>> graph1) {
    HashMap<Integer, String[]> graph1Results = new HashMap<Integer, String[]>();
    for (int region : graph2Results.keySet()) {
      String[] edges = graph2Results.get(region);
      // Retrieve the SpectralNodes that appeared in this subgraph2.
      HashSet<SpectralNode> graph2SpectralNodes = new HashSet<>();
      for (String edge : edges) {
        String[] edgeSpectralNodes = edge.split(",");
        graph2SpectralNodes.add(SpectralNodes.get(Integer.parseInt(edgeSpectralNodes[0])));
        graph2SpectralNodes.add(SpectralNodes.get(Integer.parseInt(edgeSpectralNodes[1])));
      }
      // Construct the corresponding subgraph 1.
      HashMap<SpectralNode, HashSet<SpectralNode>> subgraph1 = new HashMap<SpectralNode, HashSet<SpectralNode>>();
      for (SpectralNode SpectralNode : graph2SpectralNodes) {
        // Get the SpectralNodes that are connected to this SpectralNode in graph2.
        HashSet<SpectralNode> graph1SpectralNodes = graph1.get(SpectralNodes.get(SpectralNode.getId()));
        // Only keep SpectralNodes that appear in subgraph1.
        HashSet<SpectralNode> subSpectralNodes = new HashSet<>();
        for (SpectralNode graph1SpectralNode : graph1SpectralNodes) {
          if (graph2SpectralNodes.contains(graph1SpectralNode)) {
            subSpectralNodes.add(graph1SpectralNode);
          }
        }
        subgraph1.put(SpectralNode, subSpectralNodes);
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
  public HashMap<SpectralNode, HashSet<SpectralNode>> getGraph() {
    return graph;
  }

}
