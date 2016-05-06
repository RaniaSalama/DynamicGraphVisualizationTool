package server;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class GraphReader {

  // The adjacency list of the graph.
  private HashMap<Node, HashMap<Node, Integer>> graph;
  // Mapping between the node id and the node object.
  private HashMap<String, Node> nodeMapping;

  /**
   * Constructor initializes the graph and nodeMapping objects.
   */
  public GraphReader() {
    graph = new HashMap<Node, HashMap<Node, Integer>>();
    nodeMapping = new HashMap<String, Node>();
  }

  /**
   * Get the graph.
   * 
   * @return the graph adjacency list.
   */
  public HashMap<Node, HashMap<Node, Integer>> getGraph() {
    return graph;
  }

  /**
   * Set the graph adjacency list.
   * 
   * @param graph to set the adjacency list.
   */
  public void setGraph(HashMap<Node, HashMap<Node, Integer>> graph) {
    this.graph = graph;
  }

  /**
   * Get the node mapping.
   * 
   * @return the node mapping hash map.
   */
  public HashMap<String, Node> getNodeMapping() {
    return nodeMapping;
  }

  /**
   * Set the node mapping.
   * 
   * @param nodeMapping to set the hash map to.
   */
  public void setNodeMapping(HashMap<String, Node> nodeMapping) {
    this.nodeMapping = nodeMapping;
  }
  
  /**
   * Load graph given the graph array.
   * 
   * @param graphArray graph array representing the graph.
   */
  public void loadGraphArray(double[][] graphArray) {
    for (int i = 0; i < graphArray.length; i++) {
      Node node1 = nodeMapping.get(graphArray[i][0]+"");
      if(node1 == null) {
        node1 = new Node(0.0, graphArray[i][0]+"");
        graph.put(node1, new HashMap<Node, Integer> ());
        nodeMapping.put(graphArray[i][0]+"", node1);
      }
      Node node2 = nodeMapping.get(graphArray[i][1]+"");
      if(node2 == null) {
        node2 = new Node(0.0, graphArray[i][1]+"");
        graph.put(node2, new HashMap<Node, Integer> ());
        nodeMapping.put(graphArray[i][1]+"", node2);
      }
      int edgeValue = (int) graphArray[i][2];
      graph.get(node1).put(node2, edgeValue);
      graph.get(node2).put(node1, edgeValue);
    }
  }

  /**
   * Read the file to load the graph adjacency list and the node mapping.
   * 
   * @param inputFile the graph file.
   * @throws IOException
   */
  public void readGraph(String inputFile) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(inputFile));
    String line = null;
    while ((line = reader.readLine()) != null) {
      // The line format is line for each node, as follows:
      // node_id,node_value,[neighbor_id:edge_value,..]
      String[] splits = line.trim().split("\\[");
      String nodeID = splits[0].split(",")[0];
      Node node = nodeMapping.get(nodeID);
      if (node == null) {
        // If the node mapping didn't contain this node ID before.
        node = new Node(0.0, nodeID);
        nodeMapping.put(nodeID, node);
      }
      String[] neighbors = splits[1].substring(0, splits[1].length() - 1).split(",");
      HashMap<Node, Integer> neighborNodes = graph.get(node);
      if (neighborNodes == null) {
        neighborNodes = new HashMap<Node, Integer>();
      }
      for (String neighbor : neighbors) {
        if (neighbor.trim().length() == 0){
          // Extra spaces.
          continue;
        }
        String[] neighborSplit = neighbor.split(":");
        Node neighborNode = nodeMapping.get(neighborSplit[0]);
        if (neighborNode == null) {
          // If the node mapping didn't contain this neighbor node ID before.
          neighborNode = new Node(0.0, neighborSplit[0]);
          nodeMapping.put(neighborSplit[0], neighborNode);
        }
        // Add the neighbor to node neighbor hash map.
        neighborNodes.put(neighborNode, Integer.parseInt(neighborSplit[1]));
      }
      // Add the node with its neighbors to the graph adjacency list.
      graph.put(node, neighborNodes);
    }
    reader.close();
  }

}
