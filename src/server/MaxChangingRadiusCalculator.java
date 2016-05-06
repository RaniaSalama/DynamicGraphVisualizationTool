package server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class MaxChangingRadiusCalculator {

  // GraphCalculator contains the graphs and common operation to do on them.
  private GraphCalculator graphCalculator;
  // Input file for graph1.
  private String inputFile1;
  // Input file for graph2.
  private String inputFile2;

  public MaxChangingRadiusCalculator(String inputFile1, String inputFile2) throws IOException {
    this.inputFile1 = inputFile1;
    this.inputFile2 = inputFile2;
    graphCalculator = new GraphCalculator();
  }

  public MaxChangingRadiusCalculator() throws IOException {
    graphCalculator = new GraphCalculator();
  }

  /**
   * Get the most distorted regions by starting BFS until reaching a specific radius, then finally
   * choose the regions that has the top distortion values. The distortion measure is based on the
   * sum of the delta changes of the nodes in the region.
   * 
   * @param regionNumber number of regions to return.
   * @param nodesPerRegion at least nodes per region.
   * @return ArrayList of regions, where each region is represented by HashSet of nodes it contains.
   */
  public ArrayList<HashSet<Node>> getTopChangingRadius(int regionNumber, int nodesPerRegion) {
    HashMap<Node, HashMap<Node, Integer>> graph2 = graphCalculator.getGraph2();
    // If the region number is greater than the number of nodes, then
    // set the region number to the number of nodes in the graph.
    regionNumber = Math.min(regionNumber, graph2.size());
    // Add nodes of graph 2 to an array for sorting.
    Region[] regions = new Region[graph2.size()];
    int index = 0;
    for (Node node : graph2.keySet()) {
      // Start BFS from each node in graph 2 until reaching the desired radius.
      HashSet<Node> bfsNodes = null;
      int radius = 0;
      int regionSize = 0;
      for (; radius < graph2.size(); radius++) {
        bfsNodes = graphCalculator.BFSRadius(node, radius);
        if (bfsNodes.size() >= nodesPerRegion) {
          regionSize = bfsNodes.size();
          break;
        }
      }
      if (regionSize < nodesPerRegion) {
        regions[index++] = new Region(bfsNodes, 0, radius, regionSize);
        continue;
      }
      // Calculate the distortion value of the returned region.
      double distortionValues = 0;
      for (Node bfsNode : bfsNodes) {
        distortionValues += bfsNode.getDistortionValue();
      }
      distortionValues = distortionValues / bfsNodes.size();
      // Add this region to the regions array to sort latter on.
      regions[index++] = new Region(bfsNodes, distortionValues, radius, regionSize);
    }
    // Sort the regions based on their distortion values from the highest to the smallest.
    Arrays.sort(regions);
    ArrayList<HashSet<Node>> highestDistortionRegions = new ArrayList<HashSet<Node>>();
    for (int i = 0; i < regionNumber; i++) {
      if (regions[i].getNodes().size() < nodesPerRegion) {
        continue;
      }
      highestDistortionRegions.add(regions[i].getNodes());
    }
    return highestDistortionRegions;
  }

  /**
   * Get the most distorted regions by starting BFS until reaching a specific radius, then finally
   * choose the regions that has the top distortion values. The distortion measure is based on the
   * sum of the delta changes of the nodes in the region divided by the minimum number of edges in
   * the region in graph1 and graph2.
   * 
   * @param regionNumber number of regions to return.
   * @param nodesPerRegion at least nodes per region.
   * @return ArrayList of regions, where each region is represented by HashSet of nodes it contains.
   */
  public ArrayList<HashSet<Node>> getTopChangingRadiusWithRegionSize(int regionNumber,
      int nodesPerRegion) {
    // Get graph2 adjacency list.
    HashMap<Node, HashMap<Node, Integer>> graph2 = graphCalculator.getGraph2();
    // Get graph1 adjacency list.
    HashMap<Node, HashMap<Node, Integer>> graph1 = graphCalculator.getGraph1();
    // Get graph2 node mapping.
    HashMap<String, Node> nodeMapping2 = graphCalculator.getNodeMapping2();
    // Get graph1 node mapping.
    HashMap<String, Node> nodeMapping1 = graphCalculator.getNodeMapping1();
    // If the region number is greater than the number of nodes, then
    // set the region number to the number of nodes in the graph.
    regionNumber = Math.min(regionNumber, graph2.size());
    // Add nodes of graph 2 to an array for sorting.
    Region[] regions = new Region[graph2.size()];
    int index = 0;
    for (Node node : graph2.keySet()) {
      // Start BFS from each node in graph 2 until reaching the desired radius.
      HashSet<Node> bfsNodes = null;
      int radius = 0;
      int regionSize = 0;
      for (; radius < graph2.size(); radius++) {
        bfsNodes = graphCalculator.BFSRadius(node, radius);
        if (bfsNodes.size() >= nodesPerRegion) {
          regionSize = bfsNodes.size();
          break;
        }
      }
      if (regionSize < nodesPerRegion) {
        regions[index++] = new Region(bfsNodes, 0, radius, regionSize);
        continue;
      }
      // Calculate the distortion value of the returned region.
      double distortionValues = 0;
      double regionSizeGraph1 = 0;
      double regionSizeGraph2 = 0;
      for (Node bfsNode : bfsNodes) {
        distortionValues += bfsNode.getDistortionValue();
        HashMap<Node, Integer> bfsNodeNeighborsG1 = graph1.get(nodeMapping1.get(bfsNode.getId()));
        if (bfsNodeNeighborsG1 != null) {
          for (Node bfsNodeNeighborG1 : bfsNodeNeighborsG1.keySet()) {
            if (bfsNodes.contains(nodeMapping2.get(bfsNodeNeighborG1.getId()))) {
              regionSizeGraph1++;
            }
          }
        }
        HashMap<Node, Integer> bfsNodeNeighborsG2 = graph2.get(nodeMapping2.get(bfsNode.getId()));
        for (Node bfsNodeNeighborG2 : bfsNodeNeighborsG2.keySet()) {
          if (bfsNodes.contains(bfsNodeNeighborG2)) {
            regionSizeGraph2++;
          }
        }
      }
      distortionValues =
          distortionValues / Math.min(Math.max(1, regionSizeGraph1), Math.max(1, regionSizeGraph2));
      // Add this region to the regions array to sort latter on.
      regions[index++] = new Region(bfsNodes, distortionValues, radius, regionSize);
    }
    // Sort the regions based on their distortion values from the highest to the smallest.
    Arrays.sort(regions);
    ArrayList<HashSet<Node>> highestDistortionRegions = new ArrayList<HashSet<Node>>();
    for (int i = 0; i < graph2.size(); i++) {
      if (regions[i].getNodes().size() < nodesPerRegion) {
        continue;
      }
      highestDistortionRegions.add(regions[i].getNodes());
      if (highestDistortionRegions.size() == regionNumber) {
        break;
      }
    }
    return highestDistortionRegions;
  }


  /**
   * Run max changing radius variations.
   * 
   * @param regionNumber number of regions to return.
   * @param nodesNumPerRegion number of nodes per region.
   * @throws IOException
   */
  public void run(int regionNumber, int nodesNumPerRegion) throws IOException {
    // Read graph1 and graph2.
    graphCalculator.readGraphs(inputFile1, inputFile2);
    // Calculate delta change for each node.
    graphCalculator.calculateDeltaGraph();
    System.out.println("Max Changing Raduis");
    ArrayList<HashSet<Node>> maxChangingRadiusRegions =
        getTopChangingRadius(regionNumber, nodesNumPerRegion);
    graphCalculator.evaluateEdges(maxChangingRadiusRegions);
    System.out.println("========================================");
    System.out.println("Max Changing Radius While accounting for Region Size");
    ArrayList<HashSet<Node>> maxChangingRadiusWithRegionSizeRegions =
        getTopChangingRadiusWithRegionSize(regionNumber, nodesNumPerRegion);
    graphCalculator.evaluateEdges(maxChangingRadiusWithRegionSizeRegions);
  }

  /**
   * Run max changing radius variations while removing vertices below delta change threshold.
   * 
   * @param regionNumber number of regions to return.
   * @param nodesNumPerRegion number of nodes per region.
   * @param baisedk used in biased BFS.
   * @param runsNumber number of runs to do for choosing the best threshold.
   * @throws IOException
   */
  public void runWithThresholding(int regionNumber, int nodesNumPerRegion)
      throws IOException {
    // Read graph1 and graph2.
    graphCalculator.readGraphs(inputFile1, inputFile2);
    // Calculate delta change for each node.
    graphCalculator.calculateDeltaGraph();
    double step = 0.1;
    double maxStep = 10;
    int stepsNumber = 1;
    int numberOfNodes = graphCalculator.getGraph1().size();
    // Determine the threshold increase step size.
    while (stepsNumber < maxStep) {
      System.out.println("Iteration = " + stepsNumber);
      graphCalculator.removeNodesBelowThreshold(step, numberOfNodes);
      ArrayList<HashSet<Node>> maxChangingRadiusRegions =
          getTopChangingRadiusWithRegionSize(regionNumber, nodesNumPerRegion);
      graphCalculator.evaluatetThreshoding(maxChangingRadiusRegions, 4, step * stepsNumber);
      ArrayList<HashSet<Node>> maxChangingRadiusWithRegionSizeRegions =
          getTopChangingRadiusWithRegionSize(regionNumber, nodesNumPerRegion);
      graphCalculator.evaluatetThreshoding(maxChangingRadiusWithRegionSizeRegions, 5, step
          * stepsNumber);
      stepsNumber++;
    }
    // Print the best threshoding results of the two methods.
    System.out.println("Max Changing Raduis + Thresholding");
    graphCalculator.printBestThresholdingValues(4);
    System.out.println("Max Changing Radius While accounting for Region Size + Thresholding");
    graphCalculator.printBestThresholdingValues(5);
  }

  /**
   * Run the max changing radius methods with the browser tool
   * 
   * @param regionNumber number of regions to return.
   * @param nodesNumPerRegion number of nodes per regions.
   * @param graph1 data.
   * @param graph2 data.
   * @param region to select.
   * @param selectedTraversalMethod whether to use max changing radius with region size metric or not.
   * @return regions.
   * @throws IOException
   */
  public ArrayList<ArrayList<String>> runWithUI(int regionNumber, int nodesNumPerRegion,
      double[][] graph1, double[][] graph2, int selectedRegion, int selectedTraversalMethod)
      throws IOException {
    // Load the graphs.
    graphCalculator.readGraphs(graph1, graph2);
    // Calculate delta change for each node.
    graphCalculator.calculateDeltaGraph();
    ArrayList<HashSet<Node>> topChangingVertciesBFSRegions = null;
    if (selectedTraversalMethod == 0) {
      topChangingVertciesBFSRegions = getTopChangingRadius(regionNumber, nodesNumPerRegion);
    } else if (selectedTraversalMethod == 1) {
      topChangingVertciesBFSRegions =
          getTopChangingRadiusWithRegionSize(regionNumber, nodesNumPerRegion);
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
      System.out.println("Java -jar MaxChangingRaduis.jar graph1File graph2File regionsNumber nodesNumPerRegion baisedk");
      return;
    }
    String inputFile1 = args[0];
    String inputFile2 = args[1];
    int regionNumber = Integer.parseInt(args[2]);
    int nodesNumPerRegion = Integer.parseInt(args[2]);
    MaxChangingRadiusCalculator calculator =
        new MaxChangingRadiusCalculator(inputFile1, inputFile2);
    calculator.run(regionNumber, nodesNumPerRegion);
    System.out.println("==================Run with Thresholding======================");
    calculator.runWithThresholding(regionNumber, nodesNumPerRegion);
    //scanner.close();
  }

}
