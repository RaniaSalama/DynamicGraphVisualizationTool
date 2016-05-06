package server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;


public class TopChangingVerticesCalculator {

  // GraphCalculator contains the graphs and common operation to do on them.
  private GraphCalculator graphCalculator;
  // Input file for graph1.
  private String inputFile1;
  // Input file for graph2.
  private String inputFile2;

  // Traversal methods.
  private enum TraversalMethods {
    BFS, BiasedBFS, BFSPriorityQueue
  };

  /**
   * Constructor.
   * 
   * @param inputFile1 input file for graph1.
   * @param inputFile2 input file for graph2.
   * @throws IOException
   */
  public TopChangingVerticesCalculator(String inputFile1, String inputFile2) throws IOException {
    this.inputFile1 = inputFile1;
    this.inputFile2 = inputFile2;
    graphCalculator = new GraphCalculator();
  }

  /**
   * Constructor.
   * 
   * @throws IOException
   */
  public TopChangingVerticesCalculator() throws IOException {
    graphCalculator = new GraphCalculator();
  }

  /**
   * Get the distortion regions as the top changing vertices.
   * 
   * @param regionNumber number of regions to return.
   * @return ArrayList of regions, where each region is represented by HashSet of nodes it contains.
   */
  public ArrayList<HashSet<Node>> getTopChangingVertcies(int regionNumber) {
    HashMap<Node, HashMap<Node, Integer>> graph2 = graphCalculator.getGraph2();
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
   * Get the most distorted regions by started BFS or its variations from vertices with high delta
   * values.
   * 
   * @param regionNumber number of regions to return.
   * @param nodesNumPerRegion number of nodes per region.
   * @param baisedk parameter for Biased BFS.
   * @param traversalMethod which method to use in constructing the regions, BFS, biased BFS or BFS
   *        with priority Queue.
   * @return ArrayList of regions, where each region is represented by HashSet of nodes it contains.
   */
  public ArrayList<HashSet<Node>> getTopChangingVertcies(int regionNumber, int nodesNumPerRegion,
      int baisedk, TraversalMethods traversalMethod) {
    HashMap<Node, HashMap<Node, Integer>> graph2 = graphCalculator.getGraph2();
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
      // Start from the nodes of high distortion value and do BFS or its variations to return the
      // ith region.
      HashSet<Node> regionI = null;
      switch (traversalMethod) {
        case BFS:
          regionI = graphCalculator.BFS(nodes[i], nodesNumPerRegion);
          break;
        case BiasedBFS:
          regionI = graphCalculator.BFSBiased(nodes[i], nodesNumPerRegion, baisedk);
          break;
        case BFSPriorityQueue:
          regionI = graphCalculator.BFSPriorityQueue(nodes[i], nodesNumPerRegion);
          break;
      }
      highestDistortionRegions.add(regionI);
    }
    return highestDistortionRegions;
  }


  /**
   * Run the top changing vertices methods.
   * 
   * @param regionNumber number of regions to return.
   * @param nodesNumPerRegion number of nodes per regions.
   * @param baisedk to use in biased BFS.
   * @throws IOException
   */
  public void run(int regionNumber, int nodesNumPerRegion, int baisedk) throws IOException {
    // Load the graphs.
    graphCalculator.readGraphs(inputFile1, inputFile2);
    // Calculate delta change for each node.
    graphCalculator.calculateDeltaGraph();
    System.out.println("Top Changing Vertcies BFS");
    ArrayList<HashSet<Node>> topChangingVertciesBFSRegions =
        getTopChangingVertcies(regionNumber, nodesNumPerRegion, 0, TraversalMethods.BFS);
    graphCalculator.evaluateEdges(topChangingVertciesBFSRegions);
    System.out.println("==============================================================");
    System.out.println("Top Changing Vertcies Biased BFS");
    ArrayList<HashSet<Node>> topChangingVertciesBiasedBFSRegions =
        getTopChangingVertcies(regionNumber, nodesNumPerRegion, baisedk, TraversalMethods.BiasedBFS);
    graphCalculator.evaluateEdges(topChangingVertciesBiasedBFSRegions);
    System.out.println("==============================================================");
    System.out.println("Top Changing Vertcies BFS with Priority Queue");
    ArrayList<HashSet<Node>> topChangingVertciesBFSPriorityQueueRegions =
        getTopChangingVertcies(regionNumber, nodesNumPerRegion, 0,
            TraversalMethods.BFSPriorityQueue);
    graphCalculator.evaluateEdges(topChangingVertciesBFSPriorityQueueRegions);
    System.out.println("==============================================================");
  }

  /**
   * Run the top changing vertices methods with the browser tool
   * 
   * @param regionNumber number of regions to return.
   * @param nodesNumPerRegion number of nodes per regions.
   * @param graph1 data.
   * @param graph2 data.
   * @param region to select.
   * @param selectedTraversalMethod whether use BFS, Biased BFS or BFS with priority queue.
   * @param biasedk used in Biased BFS.
   * @return regions.
   * @throws IOException
   */
  public ArrayList<ArrayList<String>> runWithUI(int regionNumber, int nodesNumPerRegion,
      double[][] graph1, double[][] graph2, int selectedRegion, int selectedTraversalMethod,
      int biasedk) throws IOException {
    // Load the graphs.
    graphCalculator.readGraphs(graph1, graph2);
    // Calculate delta change for each node.
    graphCalculator.calculateDeltaGraph();
    ArrayList<HashSet<Node>> topChangingVertciesBFSRegions = null;
    if (selectedTraversalMethod == 0) {
      topChangingVertciesBFSRegions =
          getTopChangingVertcies(regionNumber, nodesNumPerRegion, 0, TraversalMethods.BFS);
    } else if (selectedTraversalMethod == 1) {
      topChangingVertciesBFSRegions =
          getTopChangingVertcies(regionNumber, nodesNumPerRegion, biasedk,
              TraversalMethods.BiasedBFS);
    } else if (selectedTraversalMethod == 2) {
      topChangingVertciesBFSRegions =
          getTopChangingVertcies(regionNumber, nodesNumPerRegion, 0,
              TraversalMethods.BFSPriorityQueue);
    }
    // Get graph1 region.
    HashSet<Node> selectedNodes = topChangingVertciesBFSRegions.get(selectedRegion);
    HashMap<Node, HashMap<Node, Integer>> graph1Map = graphCalculator.getGraph1();
    HashMap<String, Node> nodeMapping1 = graphCalculator.getNodeMapping1();
    HashMap<String, Node> nodeMapping2 = graphCalculator.getNodeMapping2();
    ArrayList<String> graph1Region = new ArrayList<String>();
    for (Node node : selectedNodes) {
      HashMap<Node, Integer> neighborNodes = graph1Map.get(nodeMapping1.get(node.getId()));
      if (neighborNodes == null) {
    	  neighborNodes = new HashMap<Node, Integer>();      }
      for (Node neighborNode : neighborNodes.keySet()) {
        if (selectedNodes.contains(nodeMapping2.get(neighborNode.getId()))) {
          graph1Region.add((int)Double.parseDouble(node.getId()) + "," + (int)Double.parseDouble(neighborNode.getId()));
        }
      }
    }
    // Get graph2 region.
    HashMap<Node, HashMap<Node, Integer>> graph2Map = graphCalculator.getGraph2();
    ArrayList<String> graph2Region = new ArrayList<String>();
    for (Node node : selectedNodes) {
      HashMap<Node, Integer> neighborNodes = graph2Map.get(node);
      if (neighborNodes == null) {
    	  neighborNodes = new HashMap<Node, Integer>();
      }
      for (Node neighborNode : neighborNodes.keySet()) {
        if (selectedNodes.contains(neighborNode)) {
          graph2Region.add((int)Double.parseDouble(node.getId()) + "," + (int) Double.parseDouble(neighborNode.getId()));
        }
      }
    }
    // Get node colors.
    double max = -1;
    double min = 100000;
    for (Node node : graph2Map.keySet()) {
      max = Math.max(max, node.getDistortionValue());
      min = Math.min(min, node.getDistortionValue());
    }
    ArrayList<String> nodeColors = new ArrayList<String>();
    for (Node node : graph2Map.keySet()) {
      int normalizedDistortionValue =
          (int) ((((node.getDistortionValue() - min) / (max - min))) * (Integer.MAX_VALUE - 100000));
      String hexColor = String.format("#%06X", (0xFFFFFF & normalizedDistortionValue));
      nodeColors.add(node.getId() + "," + hexColor);
    }
    ArrayList<ArrayList<String>> bothGraphRegions = new ArrayList<ArrayList<String>>();
    bothGraphRegions.add(graph1Region);
    bothGraphRegions.add(graph2Region);
    bothGraphRegions.add(nodeColors);
    return bothGraphRegions;
  }


  /**
   * @param args argument sent for the program.
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    if(args.length < 5) {
      System.out.println("Java -jar TopChangingVerticesCalculator.jar graph1File graph2File regionsNumber nodesNumPerRegion baisedk");
      return;
    }
    //Scanner scanner = new Scanner(System.in);
    String inputFile1 = args[0];
    String inputFile2 = args[1];
    int regionNumber = Integer.parseInt(args[2]); //10
    int nodesNumPerRegion = Integer.parseInt(args[3]); //16
    int baisedk = Integer.parseInt(args[4]); //5
    TopChangingVerticesCalculator calculator =
        new TopChangingVerticesCalculator(inputFile1, inputFile2);
    calculator.run(regionNumber, nodesNumPerRegion, baisedk);
    //scanner.close();
  }

}
