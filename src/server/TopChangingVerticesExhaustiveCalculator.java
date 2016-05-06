package server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;


public class TopChangingVerticesExhaustiveCalculator {

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
  public TopChangingVerticesExhaustiveCalculator(String inputFile1, String inputFile2)
      throws IOException {
    this.inputFile1 = inputFile1;
    this.inputFile2 = inputFile2;
    graphCalculator = new GraphCalculator();
  }

  /**
   * Constructor.
   * 
   * @throws IOException
   */
  public TopChangingVerticesExhaustiveCalculator() throws IOException {
    graphCalculator = new GraphCalculator();
  }

  /**
   * Start from every vertex, do BFS or its variations, then sort regions according to their
   * distortion measure and finally return the top regions with the highest distortion measure. The
   * distortion measure is based on the sum of the delta changes of the nodes in the region divided
   * by the minimum number of edges in the region in graph1 and graph2.
   * 
   * @param regionNumber number of regions to return.
   * @param nodesNumPerRegion number of nodes per region.
   * @param biasedk to be used if biased BFS is chosen.
   * @param traversalMethod which method to use in constructing the regions, BFS, biased BFS or BFS
   *        with priority Queue.
   * @return ArrayList of regions, where each region is represented by HashSet of nodes it contains.
   */
  public ArrayList<HashSet<Node>> getTopChangingVertciesExhaustiveSearch(int regionNumber,
      int nodesNumPerRegion, int biasedk, TraversalMethods traversalMethod) {
    // Get graph2 adjacency list.
    HashMap<Node, HashMap<Node, Integer>> graph2 = graphCalculator.getGraph2();
    // Get graph1 adjacency list.
    HashMap<Node, HashMap<Node, Integer>> graph1 = graphCalculator.getGraph1();
    // Get graph2 node mapping.
    HashMap<String, Node> nodeMapping2 = graphCalculator.getNodeMapping2();
    // Get graph1 node mapping.
    HashMap<String, Node> nodeMapping1 = graphCalculator.getNodeMapping1();
    regionNumber = Math.min(regionNumber, graph2.size());
    Region[] regions = new Region[graph2.size()];
    int index = 0;
    for (Node node : graph2.keySet()) {
      HashSet<Node> region = null;
      switch (traversalMethod) {
        case BFS:
          region = graphCalculator.BFS(node, nodesNumPerRegion);
          break;
        case BiasedBFS:
          region = graphCalculator.BFSBiased(node, nodesNumPerRegion, biasedk);
          break;
        case BFSPriorityQueue:
          region = graphCalculator.BFSPriorityQueue(node, nodesNumPerRegion);
          break;
      }
      // Get region size in graph 1.
      double regionSizeGraph1 = 0;
      double regionSizeGraph2 = 0;
      double distortionValue = 0;
      for (Node regionNode : region) {
        distortionValue += regionNode.getDistortionValue();
        // Get graph 1 region size.
        HashMap<Node, Integer> regionNodeNeighborsG1 =
            graph1.get(nodeMapping1.get(regionNode.getId()));
        if (regionNodeNeighborsG1 != null) {
          for (Node regionNodeNeighborG1 : regionNodeNeighborsG1.keySet()) {
            if (region.contains(nodeMapping2.get(regionNodeNeighborG1.getId()))) {
              regionSizeGraph1++;
            }
          }
        }
        // Get graph 2 region size.
        HashMap<Node, Integer> regionNodeNeighborsG2 =
            graph2.get(nodeMapping2.get(regionNode.getId()));
        if (regionNodeNeighborsG2 != null) {
          for (Node regionNodeNeighborG2 : regionNodeNeighborsG2.keySet()) {
            if (region.contains(regionNodeNeighborG2)) {
              regionSizeGraph2++;
            }
          }
        }
      }
      // Store the region with its distortion value.
      regions[index++] =
          new Region(region, distortionValue
              / Math.min(Math.max(1, regionSizeGraph1), Math.max(1, regionSizeGraph2)), 0,
              region.size());
    }
    // Sort the regions based on distortion values from the highest to the smallest.
    Arrays.sort(regions);
    ArrayList<HashSet<Node>> highestDistortionRegions = new ArrayList<HashSet<Node>>();
    for (int i = 0; i < graph2.size(); i++) {
      if (regions[i].getRegionSize() != nodesNumPerRegion) { // Ignore regions with different sizes.
        continue;
      }
      highestDistortionRegions.add(regions[i].getNodes());
      if (highestDistortionRegions.size() == regionNumber) { // Reached the desired regions number
                                                             // to return.
        break;
      }
    }
    return highestDistortionRegions;
  }


  /**
   * Run the different variations of top changing vertices exhaustive search with BFS, Biased BFS
   * and BFS with priority queue.
   * 
   * @param regionNumber number of regions to return.
   * @param nodesNumPerRegion number of nodes per region.
   * @param baisedk used in biased BFS.
   * @throws IOException
   */
  public void run(int regionNumber, int nodesNumPerRegion, int baisedk) throws IOException {
    // Read graph1 and graph2 data.
    graphCalculator.readGraphs(inputFile1, inputFile2);
    // Calculate delta change for each node.
    graphCalculator.calculateDeltaGraph();
    System.out.println("Top Changing Vertcies Exhaustive Search BFS");
    ArrayList<HashSet<Node>> bfsRegions =
        getTopChangingVertciesExhaustiveSearch(regionNumber, nodesNumPerRegion, 0,
            TraversalMethods.BFS);
    graphCalculator.evaluateEdges(bfsRegions);
    System.out.println("========================================");
    System.out.println("Top Changing Vertcies Exhaustive Search Biased BFS");
    ArrayList<HashSet<Node>> biasedBFSRegions =
        getTopChangingVertciesExhaustiveSearch(regionNumber, nodesNumPerRegion, baisedk,
            TraversalMethods.BiasedBFS);
    graphCalculator.evaluateEdges(biasedBFSRegions);
    System.out.println("========================================");
    System.out.println("Top Changing Vertcies Exhaustive Search BFS with Priority Queue");
    ArrayList<HashSet<Node>> bfsPriorityQueueRegions =
        getTopChangingVertciesExhaustiveSearch(regionNumber, nodesNumPerRegion, 0,
            TraversalMethods.BFSPriorityQueue);
    graphCalculator.evaluateEdges(bfsPriorityQueueRegions);
    System.out.println("========================================");

  }

  /**
   * Run the different variations of top changing vertices exhaustive search with BFS, Biased BFS
   * and BFS with priority queue, while removing vertices below delta change threshold.
   * 
   * @param regionNumber number of regions to return.
   * @param nodesNumPerRegion number of nodes per region.
   * @param baisedk used in biased BFS.
   * @param runsNumber number of runs to do for choosing the best threshold.
   * @throws IOException
   */
  public void runWithThresholding(int regionNumber, int nodesNumPerRegion, int baisedk) throws IOException {
    // Read graph1 and graph2 data.
    graphCalculator.readGraphs(inputFile1, inputFile2);
    // Calculate delta change for each node.
    graphCalculator.calculateDeltaGraph();
    double step = 0.1;
    double maxStep = 10;
    int stepsNumber = 0;
    int numberOfNodes = graphCalculator.getGraph1().size();
    while (stepsNumber < maxStep) {
      System.out.println("Iteration = " + stepsNumber);
      ArrayList<HashSet<Node>> bfsRegions =
          getTopChangingVertciesExhaustiveSearch(regionNumber, nodesNumPerRegion, 0,
              TraversalMethods.BFS);
      graphCalculator.evaluatetThreshoding(bfsRegions, 1, step * stepsNumber);
      ArrayList<HashSet<Node>> biasedBFSRegions =
          getTopChangingVertciesExhaustiveSearch(regionNumber, nodesNumPerRegion, baisedk,
              TraversalMethods.BiasedBFS);
      graphCalculator.evaluatetThreshoding(biasedBFSRegions, 2, step * stepsNumber);
      ArrayList<HashSet<Node>> bfsPriorityQueueRegions =
          getTopChangingVertciesExhaustiveSearch(regionNumber, nodesNumPerRegion, 0,
              TraversalMethods.BFSPriorityQueue);
      graphCalculator.evaluatetThreshoding(bfsPriorityQueueRegions, 3, step * stepsNumber);
      graphCalculator.removeNodesBelowThreshold(step, numberOfNodes);
      stepsNumber++;
    }
    // Print the best threshoding results of the three methods.
    System.out.println("Top Changing Vertcies Exhaustive Search BFS + Thresholding");
    graphCalculator.printBestThresholdingValues(1);
    System.out.println("Top Changing Vertcies Exhaustive Search Biased BFS + Thresholding");
    graphCalculator.printBestThresholdingValues(2);
    System.out.println("Top Changing Vertcies Exhaustive Search Prioirty Queue BFS + Thresholding");
    graphCalculator.printBestThresholdingValues(3);

  }

  /**
   * Run the top changing vertices exhaustive search methods with the browser tool
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
          getTopChangingVertciesExhaustiveSearch(regionNumber, nodesNumPerRegion, 0,
              TraversalMethods.BFS);
    } else if (selectedTraversalMethod == 1) {
      topChangingVertciesBFSRegions =
          getTopChangingVertciesExhaustiveSearch(regionNumber, nodesNumPerRegion, biasedk,
              TraversalMethods.BiasedBFS);
    } else if (selectedTraversalMethod == 2) {
      topChangingVertciesBFSRegions =
          getTopChangingVertciesExhaustiveSearch(regionNumber, nodesNumPerRegion, 0,
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
    	  neighborNodes = new HashMap<Node, Integer>();
      }
      for (Node neighborNode : neighborNodes.keySet()) {
        if (selectedNodes.contains(nodeMapping2.get(neighborNode.getId()))) {
          graph1Region.add((int) Double.parseDouble(node.getId()) + "," + (int) Double.parseDouble(neighborNode.getId()));
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
          graph2Region.add((int) Double.parseDouble(node.getId()) + "," + (int) Double.parseDouble(neighborNode.getId()));
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
    //Scanner scanner = new Scanner(System.in);
    if(args.length < 5) {
      System.out.println("Java -jar TopChangingVerticesExhaustiveSearchCalculator.jar graph1File graph2File regionsNumber nodesNumPerRegion baisedk");
      return;
    }
    String inputFile1 = args[0];
    String inputFile2 = args[1];
    int regionNumber = Integer.parseInt(args[2]);
    int nodesNumPerRegion = Integer.parseInt(args[3]);
    int baisedk = Integer.parseInt(args[4]);
    TopChangingVerticesExhaustiveCalculator calculator =
        new TopChangingVerticesExhaustiveCalculator(inputFile1, inputFile2);
    calculator.run(regionNumber, nodesNumPerRegion, baisedk);
    System.out.println("==================Run with Thresholding======================");
    calculator.runWithThresholding(regionNumber, nodesNumPerRegion, baisedk);
    //scanner.close();
  }

}
