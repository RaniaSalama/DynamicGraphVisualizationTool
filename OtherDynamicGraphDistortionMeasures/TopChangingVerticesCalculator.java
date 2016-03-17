import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;


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
    ArrayList<HashSet<Node>> topChangingVertciesBFSRegions =
        getTopChangingVertcies(regionNumber, nodesNumPerRegion, 0, TraversalMethods.BFS);
    graphCalculator.evaluateEdges(topChangingVertciesBFSRegions);
    System.out.println("==============================================================");
    ArrayList<HashSet<Node>> topChangingVertciesBiasedBFSRegions =
        getTopChangingVertcies(regionNumber, nodesNumPerRegion, baisedk, TraversalMethods.BiasedBFS);
    graphCalculator.evaluateEdges(topChangingVertciesBiasedBFSRegions);
    System.out.println("==============================================================");
    ArrayList<HashSet<Node>> topChangingVertciesBFSPriorityQueueRegions =
        getTopChangingVertcies(regionNumber, nodesNumPerRegion, 0,
            TraversalMethods.BFSPriorityQueue);
    graphCalculator.evaluateEdges(topChangingVertciesBFSPriorityQueueRegions);
    System.out.println("==============================================================");
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
    TopChangingVerticesCalculator calculator =
        new TopChangingVerticesCalculator(inputFile1, inputFile2);
    calculator.run(regionNumber, nodesNumPerRegion, baisedk);
    scanner.close();
  }

}
