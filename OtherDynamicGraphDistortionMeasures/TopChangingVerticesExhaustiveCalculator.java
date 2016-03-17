import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;


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
    ArrayList<HashSet<Node>> bfsRegions =
        getTopChangingVertciesExhaustiveSearch(regionNumber, nodesNumPerRegion, 0,
            TraversalMethods.BFS);
    graphCalculator.evaluateEdges(bfsRegions);
    System.out.println("========================================");
    ArrayList<HashSet<Node>> biasedBFSRegions =
        getTopChangingVertciesExhaustiveSearch(regionNumber, nodesNumPerRegion, baisedk,
            TraversalMethods.BiasedBFS);
    graphCalculator.evaluateEdges(biasedBFSRegions);
    System.out.println("========================================");
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
  public void runWithThresholding(int regionNumber, int nodesNumPerRegion, int baisedk,
      int runsNumber) throws IOException {
    // Read graph1 and graph2 data.
    graphCalculator.readGraphs(inputFile1, inputFile2);
    // Calculate delta change for each node.
    graphCalculator.calculateDeltaGraph();
    double threshold = graphCalculator.getMinDelta();
    // Determine the threshold increase step size.
    double step = (graphCalculator.getMaxDelta() - graphCalculator.getMinDelta()) / runsNumber;
    while (threshold < graphCalculator.getMaxDelta()) {
      System.out.println("Threshold = " + threshold);
      graphCalculator.removeNodesBelowThreshold(threshold);
      ArrayList<HashSet<Node>> bfsRegions =
          getTopChangingVertciesExhaustiveSearch(regionNumber, nodesNumPerRegion, 0,
              TraversalMethods.BFS);
      graphCalculator.evaluatetThreshoding(bfsRegions, 1, threshold);
      ArrayList<HashSet<Node>> biasedBFSRegions =
          getTopChangingVertciesExhaustiveSearch(regionNumber, nodesNumPerRegion, baisedk,
              TraversalMethods.BiasedBFS);
      graphCalculator.evaluatetThreshoding(biasedBFSRegions, 2, threshold);
      ArrayList<HashSet<Node>> bfsPriorityQueueRegions =
          getTopChangingVertciesExhaustiveSearch(regionNumber, nodesNumPerRegion, 0,
              TraversalMethods.BFSPriorityQueue);
      graphCalculator.evaluatetThreshoding(bfsPriorityQueueRegions, 3, threshold);
      threshold += step;
    }
    // Print the best threshoding results of the three methods.
    graphCalculator.printBestThresholdingValues(1);
    graphCalculator.printBestThresholdingValues(2);
    graphCalculator.printBestThresholdingValues(3);

  }

  /**
   * @param args argument sent for the program.
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    Scanner scanner = new Scanner(System.in);
    String inputFile1 = scanner.nextLine();
    String inputFile2 = scanner.nextLine();
    int regionNumber = 10;
    int nodesNumPerRegion = 16;
    int baisedk = 5;
    int runsNumber = 100;
    TopChangingVerticesExhaustiveCalculator calculator =
        new TopChangingVerticesExhaustiveCalculator(inputFile1, inputFile2);
    calculator.run(regionNumber, nodesNumPerRegion, baisedk);
    System.out.println("==================Run with Thresholding======================");
    calculator.runWithThresholding(regionNumber, nodesNumPerRegion, baisedk, runsNumber);
    scanner.close();
  }

}
