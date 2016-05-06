package server;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;


public class GraphCalculator {

  // graph1 adjacency list of graph 1.
  private HashMap<Node, HashMap<Node, Integer>> graph1;
  // nodeMapping1 contains the mapping between the node ID and the node object of graph 1.
  private HashMap<String, Node> nodeMapping1;
  // graph2 adjacency list of graph 2.
  private HashMap<Node, HashMap<Node, Integer>> graph2;
  // nodeMapping2 contains the mapping between the node ID and the node object of graph 2.
  private HashMap<String, Node> nodeMapping2;
  // Min delta value of a node.
  private double minDelta;
  // Max delta value of a node.
  private double maxDelta;
  // Best evaluation measures found for each method, while changing the threshold.
  private HashMap<Integer, double[][]> bestMeasures;
  // The threshold that achieves the best evaluation measures for each method.
  private HashMap<Integer, double[]> thresholdsUsed;

  /**
   * Constructor initializes the values by setting the minDelta to a maximum value maxDelta to a
   * minimum value and by creating the bestMeasures and thresholdsUsed maps.
   * 
   * @throws IOException
   */
  public GraphCalculator() throws IOException {
    minDelta = Double.MAX_VALUE;
    maxDelta = Double.MIN_VALUE;
    bestMeasures = new HashMap<Integer, double[][]>();
    thresholdsUsed = new HashMap<Integer, double[]>();
  }

  /**
   * Get graph1 adjacency list.
   * 
   * @return graph1 adjacency list.
   */
  public HashMap<Node, HashMap<Node, Integer>> getGraph1() {
    return graph1;
  }

  /**
   * Set graph1 adjacency list.
   * 
   * @param graph1 adjacency list.
   */
  public void setGraph1(HashMap<Node, HashMap<Node, Integer>> graph1) {
    this.graph1 = graph1;
  }

  /**
   * Get graph2 adjacency list.
   * 
   * @return graph2 adjacency list.
   */
  public HashMap<Node, HashMap<Node, Integer>> getGraph2() {
    return graph2;
  }

  /**
   * Set graph2 adjacency list.
   * 
   * @param graph2 adjacency list.
   */
  public void setGraph2(HashMap<Node, HashMap<Node, Integer>> graph2) {
    this.graph2 = graph2;
  }

  /**
   * Get node mapping of graph1 node IDs and node objects.
   * 
   * @return graph1 node mapping.
   */
  public HashMap<String, Node> getNodeMapping1() {
    return nodeMapping1;
  }

  /**
   * Set node mapping of graph1.
   * 
   * @param nodeMapping1 graph1 node mapping.
   */
  public void setNodeMapping1(HashMap<String, Node> nodeMapping1) {
    this.nodeMapping1 = nodeMapping1;
  }

  /**
   * Get node mapping of graph2 node IDs and node objects.
   * 
   * @return graph2 node mapping.
   */
  public HashMap<String, Node> getNodeMapping2() {
    return nodeMapping2;
  }

  /**
   * Set node mapping of graph2.
   * 
   * @param nodeMapping2 graph2 node mapping.
   */
  public void setNodeMapping2(HashMap<String, Node> nodeMapping2) {
    this.nodeMapping2 = nodeMapping2;
  }

  /**
   * Get minDelta value.
   * 
   * @return minDelta value.
   */
  public double getMinDelta() {
    return minDelta;
  }

  /**
   * Set minDelta value.
   * 
   * @param minDelta value to set to.
   */
  public void setMinDelta(double minDelta) {
    this.minDelta = minDelta;
  }

  /**
   * Get maxDelta value.
   * 
   * @return maxDelta value.
   */
  public double getMaxDelta() {
    return maxDelta;
  }

  /**
   * Set maxDelta value.
   * 
   * @param maxDelta value to set to.
   */
  public void setMaxDelta(double maxDelta) {
    this.maxDelta = maxDelta;
  }

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
   * Read two graph array and populate the graph 1 and graph 2.
   * 
   * @param graph1Array graph1 data array.
   * @param graph2Array graph2 data array.
   * @throws IOException
   */
  public void readGraphs(double[][] graph1Array, double[][] graph2Array) throws IOException {
    // Read the first graph.
    GraphReader reader1 = new GraphReader();
    reader1.loadGraphArray(graph1Array);
    graph1 = reader1.getGraph();
    nodeMapping1 = reader1.getNodeMapping();
    // Read the second graph.
    GraphReader reader2 = new GraphReader();
    reader2.loadGraphArray(graph2Array);
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
      if(node1NeighborsInGraph1 == null) {
    	  node1NeighborsInGraph1 = new HashMap<Node, Integer>();
      }
      Node node2 = nodeMapping2.get(node1.getId());
      HashMap<Node, Integer> node1NeighborsInGraph2 = graph2.get(node2);
      if(node1NeighborsInGraph2 == null) {
    	  node1NeighborsInGraph2 = new HashMap<Node, Integer>();
      }
      int delta = 0; // delta change of node 1.
      for (Node node1NeighborInGraph1 : node1NeighborsInGraph1.keySet()) {
        // Get the weight of this edge in graph 1.
        int edge1Weight = node1NeighborsInGraph1.get(node1NeighborInGraph1);
        Node node1NeighborInGraph2 = nodeMapping2.get(node1NeighborInGraph1.getId());
        if (node1NeighborsInGraph2 != null
            && node1NeighborsInGraph2.containsKey(node1NeighborInGraph2)) {
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
      if (node1NeighborsInGraph2 != null) {
        for (Node node1NeighborInGraph2 : node1NeighborsInGraph2.keySet()) {
          Node node1NeighborInGraph1 = nodeMapping1.get(node1NeighborInGraph2.getId());
          int edge2Weight = node1NeighborsInGraph2.get(node1NeighborInGraph2);
          if (!node1NeighborsInGraph1.containsKey(node1NeighborInGraph1)) {
            // If the edge only exist in graph 2 and not in graph 1, then
            // the edge weight in graph 1 is zero and thus delta is the edge weight in graph 2.
            delta += edge2Weight;
          }
        }
      }
      // Set the delta value as the distortion value of this node in graph 1 and graph 2.
      if (node1 != null) {
        node1.setDistortionValue(delta);
      }
      if (node2 != null) {
        node2.setDistortionValue(delta);
      }
      minDelta = Math.min(minDelta, node1.getDistortionValue());
      maxDelta = Math.max(maxDelta, node1.getDistortionValue());
    }
  }

  /**
   * Remove nodes with delta change less than the threshold.
   * 
   * @param step to remove the nodes based on.
   * @param numberOfNodes number of nodes in the graph.
   */
  public void removeNodesBelowThreshold(double step, int numberOfNodes) {
    HashSet<Node> graph1Nodes = new HashSet<Node>();
    // get graph1 nodes.
    graph1Nodes.addAll(graph1.keySet());
    Node[] nodes = new Node[graph1Nodes.size()];
    int index = 0;
    for (Node node : graph1Nodes) {
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
      HashMap<Node, Integer> node1Nbrs = graph1.get(node1);
      // Remove node1 from graph1.
      graph1.remove(node1);
      nodeMapping1.remove(node1.getId());
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
      if (neighbors == null) {
        continue;
      }
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
        if (neighbors == null) {
          continue;
        }
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
      HashMap<Node, Integer> neighborsMap = graph2.get(currentNode);
      if (neighborsMap == null) {
        continue;
      }
      Set<Node> neighbors = neighborsMap.keySet();
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
      if (neighbors == null) {
        continue;
      }
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
      System.out.println("Region size  = " + region.size());
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
   * Evaluate the thresholding performance for this specific threshold.
   * 
   * @param regions regions to evaluate.
   * @param methodID id of the method we are evaluating.
   * @param threshold used when we run the method.
   * @return
   */
  public double[][] evaluatetThreshoding(ArrayList<HashSet<Node>> regions, int methodID,
      double threshold) {
    // Six evaluation measures for each region.
    double[][] changeValues = new double[regions.size()][6];
    int index = 0;
    for (HashSet<Node> region : regions) { // Calculate evaluation measures for each region.
      double changeValue = 0.0; // Sum of change values of the nodes in the region.
      double edgesWithinRegionInGraph1 = 0.0; // Number of edges in the region in graph1.
      double edgesWithinRegionInGraph2 = 0.0; // Number of edges in the region in graph2.
      double nodesDegreeInGraph1 = 0.0; // Sum of node degrees of nodes in the region in graph1.
      double nodesDegreeInGraph2 = 0.0; // Sum of node degrees of nodes in the region in graph2.
      for (Node node : region) {
        HashMap<Node, Integer> node1Neighbors = graph1.get(nodeMapping1.get(node.getId()));
        HashMap<Node, Integer> node2Neighbors = graph2.get(nodeMapping2.get(node.getId()));
        nodesDegreeInGraph1 += node1Neighbors.size();
        nodesDegreeInGraph2 += node2Neighbors.size();
        for (Node node1Neighbor : node1Neighbors.keySet()) {
          if (region.contains(nodeMapping2.get(node1Neighbor.getId()))) {
            edgesWithinRegionInGraph1++;
          }
        }
        for (Node node2Neighbor : node2Neighbors.keySet()) {
          if (region.contains(node2Neighbor)) {
            edgesWithinRegionInGraph2++;
          }
        }
        changeValue += node.getDistortionValue();
      }
      // Calculate the evaluation measures and store them.
      changeValues[index][0] = changeValue / Math.max(1, edgesWithinRegionInGraph1);
      changeValues[index][1] = changeValue / Math.max(1, edgesWithinRegionInGraph2);
      changeValues[index][2] =
          changeValue
              / Math.min(Math.max(1, edgesWithinRegionInGraph1),
                  Math.max(1, edgesWithinRegionInGraph2));
      changeValues[index][3] = changeValue / Math.max(1, nodesDegreeInGraph1);
      changeValues[index][4] = changeValue / Math.max(1, nodesDegreeInGraph2);
      changeValues[index][5] =
          changeValue
              / Math.min(Math.max(1, nodesDegreeInGraph1), Math.max(1, nodesDegreeInGraph2));
      index++;
    }
    double[][] bestChangeValues = bestMeasures.get(methodID);
    if (bestChangeValues == null) {
      bestChangeValues = new double[regions.size()][6];
    }
    double[] bestThreshold = thresholdsUsed.get(methodID);
    if (bestThreshold == null) {
      bestThreshold = new double[6];
    }
    // Compare whether the current evaluation measures are better than the previous ones, using
    // different threshold.
    for (index = 0; index < bestChangeValues[0].length; index++) {
      double currentSum = sumArray(getColumn(changeValues, index));
      double bestSum = sumArray(getColumn(bestChangeValues, index));
      if (currentSum > bestSum) { // The current measures are better, store them.
        bestSum = currentSum;
        for (int i = 0; i < bestChangeValues.length; i++) {
          bestChangeValues[i][index] = changeValues[i][index];
        }
        bestThreshold[index] = threshold;
      }
    }
    // Store the new updated measures.
    bestMeasures.put(methodID, bestChangeValues);
    thresholdsUsed.put(methodID, bestThreshold);
    return changeValues;
  }

  /**
   * Get specific column in the matrix.
   * 
   * @param matrix to retrieve its ith column.
   * @param i ith column to retrieve.
   * @return ith column of the matrix.
   */
  private double[] getColumn(double[][] matrix, int i) {
    double[] columnI = new double[matrix.length];
    for (int j = 0; j < columnI.length; j++) {
      columnI[j] = matrix[j][i];
    }
    return columnI;
  }

  /**
   * Sum the array.
   * 
   * @param array to be summed.
   * @return the sum of the values of the array.
   */
  public double sumArray(double[] array) {
    double sum = 0;
    for (int i = 0; i < array.length; i++) {
      sum += array[i];
    }
    return sum;
  }

  /**
   * Print the best evaluation measures of the method returned by the threshoding technique.
   * 
   * @param methodID to print its evaluation measures.
   */
  public void printBestThresholdingValues(int methodID) {
    System.out.println(methodID);
    double[][] bestChangeValues = bestMeasures.get(methodID);
    double[] bestThresholds = thresholdsUsed.get(methodID);
    for (int i = 0; i < bestChangeValues[0].length; i++) {
      for (int index = 1; index <= bestChangeValues.length; index++) {
        System.out.println("R" + index + "=" + getString(bestChangeValues[index - 1][i]));
      }
      System.out.println("epson=" + getString(bestThresholds[i]));
      System.out.println("************************");
    }

  }

  /**
   * Calculates the distortion evaluation metric for the given regions.
   * 
   * @param regions to evaluate.
   * @return double[][] array of the same size as the region, containing the distortion measures for
   *         each region.
   */
  public double[][] evaluateEdges(ArrayList<HashSet<Node>> regions) {
    double[][] changeValues = new double[regions.size()][6];
    int index = 0;
    // Strings to print.
    String changeOverEdgesWithinRegionInGraph1Result = "";
    String changeOverEdgesWithinRegionInGraph2Result = "";
    String changeOverEdgesWithinRegionInMinGraphResult = "";
    String changeOverNodesDegreeInGraph1Result = "";
    String changeOverNodesDegreeInGraph2Result = "";
    String changeOverNodesDegreeInMinGraphResult = "";
    for (HashSet<Node> region : regions) {
      double changeValue = 0.0; // Sum of change values of the nodes in the region.
      double edgesWithinRegionInGraph1 = 0.0; // Number of edges in the region in graph1.
      double edgesWithinRegionInGraph2 = 0.0; // Number of edges in the region in graph2.
      double nodesDegreeInGraph1 = 0.0; // Sum of node degrees of nodes in the region in graph1.
      double nodesDegreeInGraph2 = 0.0; // Sum of node degrees of nodes in the region in graph2.
      for (Node node : region) { // for each node in the region.
        HashMap<Node, Integer> node1Neighbors = graph1.get(nodeMapping1.get(node.getId()));
        HashMap<Node, Integer> node2Neighbors = graph2.get(nodeMapping2.get(node.getId()));
        nodesDegreeInGraph1 += node1Neighbors.size();
        nodesDegreeInGraph2 += node2Neighbors.size();
        for (Node node1Neighbor : node1Neighbors.keySet()) {
          if (region.contains(nodeMapping2.get(node1Neighbor.getId()))) {
            edgesWithinRegionInGraph1++;
          }
        }
        for (Node node2Neighbor : node2Neighbors.keySet()) {
          if (region.contains(node2Neighbor)) {
            edgesWithinRegionInGraph2++;
          }
        }
        changeValue += node.getDistortionValue();
      }
      // Store change values.
      changeValues[index][0] = changeValue / Math.max(1, edgesWithinRegionInGraph1);
      changeValues[index][1] = changeValue / Math.max(1, edgesWithinRegionInGraph2);
      changeValues[index][2] =
          changeValue
              / Math.min(Math.max(1, edgesWithinRegionInGraph1),
                  Math.max(1, edgesWithinRegionInGraph2));
      changeValues[index][3] = changeValue / Math.max(1, nodesDegreeInGraph1);
      changeValues[index][4] = changeValue / Math.max(1, nodesDegreeInGraph2);
      changeValues[index][5] =
          changeValue
              / Math.min(Math.max(1, nodesDegreeInGraph1), Math.max(1, nodesDegreeInGraph2));
      // Add the result to the string formating that will be printed at the end.
      changeOverEdgesWithinRegionInGraph1Result =
          changeOverEdgesWithinRegionInGraph1Result + "\nR" + index + "="
              + getString(changeValues[index][0]);
      changeOverEdgesWithinRegionInGraph2Result =
          changeOverEdgesWithinRegionInGraph2Result + "\nR" + index + "="
              + getString(changeValues[index][1]);
      changeOverEdgesWithinRegionInMinGraphResult =
          changeOverEdgesWithinRegionInMinGraphResult + "\nR" + index + "="
              + getString(changeValues[index][2]);
      changeOverNodesDegreeInGraph1Result =
          changeOverNodesDegreeInGraph1Result + "\nR" + index + "="
              + getString(changeValues[index][3]);
      changeOverNodesDegreeInGraph2Result =
          changeOverNodesDegreeInGraph2Result + "\nR" + index + "="
              + getString(changeValues[index][4]);
      changeOverNodesDegreeInMinGraphResult =
          changeOverNodesDegreeInMinGraphResult + "\nR" + index + "="
              + getString(changeValues[index][5]);
      index++;
    }
    // Print the evaluation results.
    System.out.print("Within graph 1");
    System.out.println(changeOverEdgesWithinRegionInGraph1Result);
    System.out.print("Within graph 2");
    System.out.println(changeOverEdgesWithinRegionInGraph2Result);
    System.out.print("Within min graph 1,2");
    System.out.println(changeOverEdgesWithinRegionInMinGraphResult);
    System.out.print("Degree graph 1");
    System.out.println(changeOverNodesDegreeInGraph1Result);
    System.out.print("Degree graph 2");
    System.out.println(changeOverNodesDegreeInGraph2Result);
    System.out.print("Degree min graph 1,2");
    System.out.println(changeOverNodesDegreeInMinGraphResult);
    return changeValues;
  }

  public String getString(double value) {
    DecimalFormat df = new DecimalFormat("#.###");
    String changeValueString = df.format(value);
    return changeValueString;
  }

  /**
   * Print the regions for testing purposes.
   * 
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
