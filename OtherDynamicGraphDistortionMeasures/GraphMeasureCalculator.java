import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;


public class GraphMeasureCalculator {

  // graph1 adjacency list of graph 1.
  private HashMap<Node, HashMap<Node, Integer>> graph1;
  // nodeMapping1 contains the mapping between the node ID and the node object of graph 1.
  private HashMap<String, Node> nodeMapping1;

  // graph2 adjacency list of graph 2.
  private HashMap<Node, HashMap<Node, Integer>> graph2;
  // nodeMapping2 contains the mapping between the node ID and the node object of graph 2.
  private HashMap<String, Node> nodeMapping2;

  /**
   * Read two graph files and populate the graph 1 and graph 2.
   * 
   * @param inputFile1 graph 1 data file.
   * @param inputFile2 graph 2 data file.
   * @throws IOException
   */
  public void readGraphs(String inputFile1, String inputFile2) throws IOException {
    // Read the first graph.
    GraphReader reader1 = new GraphReader();
    reader1.readGraph(inputFile1);
    graph1 = reader1.getGraph();
    nodeMapping1 = reader1.getNodeMapping();
    // Read the second graph.
    GraphReader reader2 = new GraphReader();
    reader2.readGraph(inputFile2);
    graph2 = reader2.getGraph();
    nodeMapping2 = reader2.getNodeMapping();
  }

  /**
   * Method used for testing that the graph 1 was read correctly by printing its adjacency list.
   */
  public void printGraph1() {
    for (Node node : graph1.keySet()) {
      HashMap<Node, Integer> neighborNodes = graph1.get(node);
      for (Node neighborNode : neighborNodes.keySet()) {
        System.out.print(" " + node.getId() + ":" + node.getDistortionValue() + ","
            + neighborNode.getId() + ":" + neighborNode.getDistortionValue() + ","
            + neighborNodes.get(neighborNode));
      }
      System.out.println();
    }
    System.out.println("===============================================");
  }

  /**
   * Method used for testing that the graph 2 was read correctly by printing its adjacency list.
   */
  public void printGraph2() {
    for (Node node : graph2.keySet()) {
      HashMap<Node, Integer> neighborNodes = graph2.get(node);
      for (Node neighborNode : neighborNodes.keySet()) {
        if (neighborNode.getId().trim().length() == 0) {
          System.out.println(node.getId() + "******");
        }
        System.out.print(" " + node.getId() + ":" + node.getDistortionValue() + ","
            + neighborNode.getId() + ":" + neighborNode.getDistortionValue() + ","
            + neighborNodes.get(neighborNode));
      }
      System.out.println();
    }
    System.out.println("===============================================");
  }

  /**
   * For each vertex, calculate its delta change from graph 1 to graph 2 as the sum of difference of
   * its edges in graph 1 and its edges in graph 2.
   */
  public void calculateDeltaGraph() {
    for (Node node1 : graph1.keySet()) {
      HashMap<Node, Integer> node1NeighborsInGraph1 = graph1.get(node1);
      Node node2 = nodeMapping2.get(node1.getId());
      HashMap<Node, Integer> node1NeighborsInGraph2 = graph2.get(node2);
      int delta = 0; // delta change of node 1.
      for (Node node1NeighborInGraph1 : node1NeighborsInGraph1.keySet()) {
        // Get the weight of this edge in graph 1.
        int edge1Weight = node1NeighborsInGraph1.get(node1NeighborInGraph1);
        Node node1NeighborInGraph2 = nodeMapping2.get(node1NeighborInGraph1.getId());
        if (node1NeighborsInGraph2.containsKey(node1NeighborInGraph2)) {
          // If this edge exists in graph 2, get its weight in graph 2.
          int edge2Weight = node1NeighborsInGraph2.get(node1NeighborInGraph2);
          // The change in this case equal to the absolute difference between the edge weights.
          delta = delta + Math.abs(edge1Weight - edge2Weight);
        } else {
          // If graph 2 doesn't contain this edges, then its weight in graph 2 is zero
          // and then the delta is the edge weight in graph 1.
          delta += edge1Weight;
        }
      }
      for (Node node1NeighborInGraph2 : node1NeighborsInGraph2.keySet()) {
        Node node1NeighborInGraph1 = nodeMapping1.get(node1NeighborInGraph2.getId());
        int edge2Weight = node1NeighborsInGraph2.get(node1NeighborInGraph2);
        if (!node1NeighborsInGraph1.containsKey(node1NeighborInGraph1)) {
          // If the edge only exist in graph 2 and not in graph 1, then
          // the edge weight in graph 1 is zero and thus delta is the edge weight in graph 2.
          delta += edge2Weight;
        }
      }
      // Set the delta value as the distortion value of this node in graph 1 and graph 2.
      node1.setDistortionValue(delta);
      node2.setDistortionValue(delta);
    }
  }

  /**
   * Get the distortion regions as the top changing vertices.
   * 
   * @param regionNumber number of regions to return.
   * @return ArrayList of regions, where each region is represented by HashSet of nodes it contains.
   */
  public ArrayList<HashSet<Node>> getTopChangingVertcies(int regionNumber) {
    // If the region number is greater than the number of nodes, then
    // set the region number to the number of nodes in the graph.
    regionNumber = Math.min(regionNumber, graph2.size());
    // Add graph 2 nodes to Node array.
    Node[] nodes = new Node[graph2.size()];
    int index = 0;
    for (Node node1 : graph2.keySet()) {
      nodes[index++] = node1;
    }
    // Sort the nodes based on their distortion values from the highest to the lowest.
    Arrays.sort(nodes);
    ArrayList<HashSet<Node>> highestDistortionRegions = new ArrayList<HashSet<Node>>();
    // Loop from the highest distorted node to the lowest.
    for (int i = 0; i < regionNumber; i++) {
      // Add node i as the region i.
      HashSet<Node> regionI = new HashSet<Node>();
      regionI.add(nodes[i]);
      highestDistortionRegions.add(regionI);
    }
    return highestDistortionRegions;
  }

  /**
   * Start traditional BFS from node until the number of nodes in the BFS graph is equal to the
   * nodesNumPerRegion.
   * 
   * @param node to start the BFS from.
   * @param nodesNumPerRegion number of nodes in the BFS graph.
   * @return HashSet of nodes in the BFS graph.
   */
  public HashSet<Node> BFS(Node node, int nodesNumPerRegion) {
    // found to keep track of nodes that are examined in the BFS so far.
    HashSet<Node> found = new HashSet<Node>();
    found.add(node);
    Queue<Node> queue = new LinkedList<Node>();
    queue.add(node);
    HashSet<Node> bfsNodes = new HashSet<Node>();
    while (!queue.isEmpty()) {
      Node currentNode = queue.poll();
      bfsNodes.add(currentNode);
      if (bfsNodes.size() == nodesNumPerRegion) {
        // The number of nodes in the current BFS graph is equal to the
        // nodesNumPerRegion.
        break;
      }
      HashMap<Node, Integer> neighbors = graph2.get(currentNode);
      for (Node neighbor : neighbors.keySet()) {
        // for each node connected to the current Node.
        if (!found.contains(neighbor)) {
          // Not visited yet.
          found.add(neighbor);
          queue.add(neighbor);
        }
      }
    }
    return bfsNodes;
  }

  /**
   * Start traditional BFS from node until the BFS graph radius is equal to the parameter radius.
   * 
   * @param node to start the BFS from.
   * @param radius desired radius of the BFS graph.
   * @return HashSet of nodes in the BFS graph.
   */
  public HashSet<Node> BFSRadius(Node node, int radius) {
    // found to keep track of nodes that are examined in the BFS so far.
    HashSet<Node> found = new HashSet<Node>();
    found.add(node);
    // queue will contain a pair of key and value, where key is the node and value is the current
    // radius of the node from the start node.
    Queue<HashMap<Node, Integer>> queue = new LinkedList<HashMap<Node, Integer>>();
    HashMap<Node, Integer> firstNode = new HashMap<Node, Integer>();
    // First node radius is zero.
    firstNode.put(node, 0);
    queue.add(firstNode);
    HashSet<Node> bfsNodes = new HashSet<Node>();
    while (!queue.isEmpty()) {
      // Get the current node pair.
      HashMap<Node, Integer> currentNodePair = queue.poll();
      for (Node currentNode : currentNodePair.keySet()) {
        // Get the current node radius.
        int currentRadius = currentNodePair.get(currentNode);
        bfsNodes.add(currentNode);
        if (currentRadius == radius) {
          // Don't put its neighbor as they are out of the radius range.
          continue;
        }
        HashMap<Node, Integer> neighbors = graph2.get(currentNode);
        for (Node neighbor : neighbors.keySet()) {
          // for each node connected to the current Node.
          if (!found.contains(neighbor)) {
            HashMap<Node, Integer> neighborPair = new HashMap<Node, Integer>();
            neighborPair.put(neighbor, currentRadius + 1);
            // Add the neighbor node to the queue and update its radius to its parent node radius +
            // 1.
            queue.add(neighborPair);
            found.add(neighbor);
          }
        }
      }
    }
    return bfsNodes;
  }

  /**
   * Start biased BFS from node until the BFS graph number of nodes is equal to the parameter
   * nodesNumPerRegion. The biased BFS expands from each node by only considering the expansion from
   * its top distorted biasedk neighbor nodes and neglect the other neighbors.
   * 
   * @param node to start the BFS from.
   * @param nodesNumPerRegion number of nodes in the BFS graph.
   * @param biasedk top distorted biasedk neighbor nodes to continue the expansion from.
   * @return HashSet of nodes in the BFS graph.
   */
  public HashSet<Node> BFSBiased(Node node, int nodesNumPerRegion, int biasedk) {
    // found to keep track of nodes that are examined in the BFS so far.
    HashSet<Node> found = new HashSet<Node>();
    found.add(node);
    Queue<Node> queue = new LinkedList<Node>();
    queue.add(node);
    HashSet<Node> bfsNodes = new HashSet<Node>();
    while (!queue.isEmpty()) {
      Node currentNode = queue.poll();
      bfsNodes.add(currentNode);
      if (bfsNodes.size() == nodesNumPerRegion) {
        // The number of nodes in the current BFS graph is equal to the
        // nodesNumPerRegion.
        break;
      }
      Set<Node> neighbors = graph2.get(currentNode).keySet();
      // Add the neighbors of the current node to an array for sorting.
      Node[] neighborNodes = new Node[neighbors.size()];
      int index = 0;
      for (Node neighbor : neighbors) {
        neighborNodes[index++] = neighbor;
      }
      // Sort the neighborNodes based on the distortion values from the highest to the lowest.
      Arrays.sort(neighborNodes);
      int addedCount = 0; // Keep track of the number of added neighbors until it reaches biasedk.
      for (int i = 0; i < neighborNodes.length; i++) {
        if (!found.contains(neighborNodes[i])) {
          queue.add(neighborNodes[i]);
          found.add(neighborNodes[i]);
          addedCount++;
          if (addedCount == biasedk) {
            // If the number of added neighbors equal to biasedk, then
            // stop adding the neighbors.
            break;
          }
        }
      }
    }
    return bfsNodes;
  }

  /**
   * Start priority queue BFS from node until the BFS graph number of nodes is equal to the
   * parameter nodesNumPerRegion. The priority queue BFS uses a priority queue instead of
   * traditional queue, where the priority is the distortion value of the node and the higher this
   * value is, the higher its priority will be.
   * 
   * @param node to start the BFS from.
   * @param nodesNumPerRegion number of nodes in the BFS graph.
   * @return HashSet of nodes in the BFS graph.
   */
  public HashSet<Node> BFSPriorityQueue(Node node, int nodesNumPerRegion) {
    // found to keep track of nodes that are examined in the BFS so far.
    HashSet<Node> found = new HashSet<Node>();
    found.add(node);
    PriorityQueue<Node> queue = new PriorityQueue<Node>();
    queue.add(node);
    HashSet<Node> bfsNodes = new HashSet<Node>();
    while (!queue.isEmpty()) {
      Node currentNode = queue.poll();
      bfsNodes.add(currentNode);
      if (bfsNodes.size() == nodesNumPerRegion) {
        // The number of nodes in the current BFS graph is equal to the
        // nodesNumPerRegion.
        break;
      }
      HashMap<Node, Integer> neighbors = graph2.get(currentNode);
      for (Node neighbor : neighbors.keySet()) {
        // for each node connected to the current Node.
        if (!found.contains(neighbor)) {
          found.add(neighbor);
          queue.add(neighbor);
        }
      }
    }
    return bfsNodes;
  }


  /**
   * Get the most distorted regions by started traditional BFS from vertices with high delta values.
   * 
   * @param regionNumber number of regions to return.
   * @param nodesNumPerRegion number of nodes per region.
   * @return ArrayList of regions, where each region is represented by HashSet of nodes it contains.
   */
  public ArrayList<HashSet<Node>> getTopChangingVertciesBFS(int regionNumber, int nodesNumPerRegion) {
    // If the region number is greater than the number of nodes, then
    // set the region number to the number of nodes in the graph.
    regionNumber = Math.min(regionNumber, graph2.size());
    // Add nodes of graph 2 to an array for sorting.
    Node[] nodes = new Node[graph2.size()];
    int index = 0;
    for (Node node : graph2.keySet()) {
      nodes[index++] = node;
    }
    // Sort the nodes based on their distortion values from the highest to lowest.
    Arrays.sort(nodes);
    ArrayList<HashSet<Node>> highestDistortionRegions = new ArrayList<HashSet<Node>>();
    for (int i = 0; i < regionNumber; i++) {
      // Start from the nodes of high distortion value and do BFS to return the ith region.
      HashSet<Node> regionI = BFS(nodes[i], nodesNumPerRegion);
      highestDistortionRegions.add(regionI);
    }
    return highestDistortionRegions;
  }


  /**
   * Get the most distorted regions by started Biased BFS from vertices with high delta values.
   * 
   * @param regionNumber number of regions to return.
   * @param nodesNumPerRegion number of nodes per region.
   * @param baisedk parameter for Biased BFS.
   * @return ArrayList of regions, where each region is represented by HashSet of nodes it contains.
   */
  public ArrayList<HashSet<Node>> getTopChangingVertciesBiasedBFS(int regionNumber,
      int nodesNumPerRegion, int baisedk) {
    // If the region number is greater than the number of nodes, then
    // set the region number to the number of nodes in the graph.
    regionNumber = Math.min(regionNumber, graph2.size());
    // Add nodes of graph 2 to an array for sorting.
    Node[] nodes = new Node[graph2.size()];
    int index = 0;
    for (Node node1 : graph2.keySet()) {
      nodes[index++] = node1;
    }
    // Sort the nodes based on their distortion values from the highest to lowest.
    Arrays.sort(nodes);
    ArrayList<HashSet<Node>> highestDistortionRegions = new ArrayList<HashSet<Node>>();
    for (int i = 0; i < regionNumber; i++) {
      // Start from the nodes of high distortion value and do Biased BFS to return the ith region.
      HashSet<Node> regionI = BFSBiased(nodes[i], nodesNumPerRegion, baisedk);
      highestDistortionRegions.add(regionI);
    }
    return highestDistortionRegions;
  }

  /**
   * Get the most distorted regions by started BFS using priority queue from vertices with high
   * delta values.
   * 
   * @param regionNumber number of regions to return.
   * @param nodesNumPerRegion number of nodes per region.
   * @return ArrayList of regions, where each region is represented by HashSet of nodes it contains.
   */
  public ArrayList<HashSet<Node>> getTopChangingVertciesBFSPriorityQueue(int regionNumber,
      int nodesNumPerRegion) {
    // If the region number is greater than the number of nodes, then
    // set the region number to the number of nodes in the graph.
    regionNumber = Math.min(regionNumber, graph2.size());
    // Add nodes of graph 2 to an array for sorting.
    Node[] nodes = new Node[graph2.size()];
    int index = 0;
    for (Node node1 : graph2.keySet()) {
      nodes[index++] = node1;
    }
    // Sort the nodes based on their distortion values from the highest to lowest.
    Arrays.sort(nodes);
    ArrayList<HashSet<Node>> highestDistortionRegions = new ArrayList<HashSet<Node>>();
    for (int i = 0; i < regionNumber; i++) {
      // Start from the nodes of high distortion value and do BFS using priority queue to return the
      // ith region.
      HashSet<Node> regionI = BFSPriorityQueue(nodes[i], nodesNumPerRegion);
      highestDistortionRegions.add(regionI);
    }
    return highestDistortionRegions;
  }


  /**
   * Get the most distorted regions by started BFS until reaching a specific radius, then finally
   * choose the regions that has the top distortion values.
   * 
   * @param regionNumber number of regions to return.
   * @param radius desired radius of the region.
   * @return ArrayList of regions, where each region is represented by HashSet of nodes it contains.
   */
  public ArrayList<HashSet<Node>> getTopChangingRadius(int regionNumber, int radius) {
    // If the region number is greater than the number of nodes, then
    // set the region number to the number of nodes in the graph.
    regionNumber = Math.min(regionNumber, graph2.size());
    // Add nodes of graph 2 to an array for sorting.
    Region[] regions = new Region[graph2.size()];
    int index = 0;
    for (Node node : graph2.keySet()) {
      // Start BFS from each node in graph 2 until reaching the desired radius.
      HashSet<Node> bfsNodes = BFSRadius(node, radius);
      // Calculate the distortion value of the returned region.
      double distortionValues = 0;
      for (Node bfsNode : bfsNodes) {
        distortionValues += bfsNode.getDistortionValue();
      }
      distortionValues = distortionValues / bfsNodes.size();
      // Add this region to the regions array to sort latter on.
      regions[index++] = new Region(bfsNodes, distortionValues);
    }
    // Sort the regions based on their distortion values from the highest to the smallest.
    Arrays.sort(regions);
    ArrayList<HashSet<Node>> highestDistortionRegions = new ArrayList<HashSet<Node>>();
    for (int i = 0; i < regionNumber; i++) {
      highestDistortionRegions.add(regions[i].getNodes());
    }
    return highestDistortionRegions;
  }

  /**
   * Calculates the distortion evaluation metric for the given regions.
   * 
   * @param regions to evaluate.
   * @return double[] array of the same size as the region, containing the distortion metric for
   *         each region.
   */
  public double[] evaluate(ArrayList<HashSet<Node>> regions) {
    double[] changeValues = new double[regions.size()];
    int index = 0;
    for (HashSet<Node> region : regions) {
      double changeValue = 0.0;
      for (Node node : region) {
        changeValue += node.getDistortionValue();
      }
      changeValue = changeValue / region.size();
      changeValues[index++] = changeValue;
      String changeValueString = changeValue + "";
      String[] splits = changeValueString.split("\\.");
      changeValueString = splits[0] + "." + splits[1].substring(0, Math.min(3, splits[1].length()));
      System.out.println("R" + index + " = " + changeValueString);
    }
    return changeValues;
  }

  /**
   * Print the regions for testing purposes. 
   * @param regions to print.
   */
  public void printRegion(ArrayList<HashSet<Node>> regions) {
    int index = 0;
    for (HashSet<Node> region : regions) {
      System.out.println("Region " + (index++));
      for (Node node : region) {
        System.out.print(node.getId() + " ");
      }
      System.out.println();
    }
    System.out.println("====================================");
  }
}
