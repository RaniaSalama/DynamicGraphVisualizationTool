package server;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;
import matlabcontrol.MatlabProxyFactoryOptions;
import matlabcontrol.extensions.MatlabNumericArray;
import matlabcontrol.extensions.MatlabTypeConverter;

/**
 * Servlet implementation class GraphServlet to handle the user request to color the graph with
 * distortion values.
 */
@SuppressWarnings("serial")
public class GraphServlet extends HttpServlet {
  // graph1 parameter key name.
  private static final String GRAPH1_PARAMATER_KEY = "graph1file";
  // graph2 parameter key name.
  private static final String GRAPH2_PARAMATER_KEY = "graph2file";
  // Number of regions to calculate.
  private static final int REGION_NUM = 10;
  // Max number of nodes per region.
  private static final int MAX_NODES = 30;
  // Max overlapping nodes between regions.
  private static final double MAX_OVERLAP = 0.7;
  private static final int REGION_SELECTOR = 1;
  // MATLAB file to run.
  private static final String MATLAB_FILE = "server/matlab/visualize_map.m";
  // basepath for storing the two graphs.
  private static String basePath = "";
  // proxy used to run MATLAB code.
  private static MatlabProxy proxy = null;
  // graph1 edges values.
  private static double[][] graph1 = null;
  // graph2 edges values.
  private static double[][] graph2 = null;
  // Store previous parameters to re-use the results.
  private static double[][] prevGraph1 = null;
  private static double[][] prevGraph2 = null;
  private static int prevK = 0;
  private static String prevMeasure = "";
  private static String[] prevNodesColors = null;
  private double[] prevNodesDistortionValues = null;
  // Number of nodes in the graph.
  private static int nodesNumber = 0;

  /**
   * Servlet constructor initializes the MATLAB proxy and sets the MATLAB path.
   * 
   * @throws MatlabConnectionException
   * @throws URISyntaxException.
   */
  public GraphServlet() throws MatlabConnectionException, URISyntaxException {
    super();
    runMatlabCode();
  }

  /**
   * runMatlabCode initializes the MATLAB proxy and sets the basepath to MATLAB code path.
   * 
   * @throws MatlabConnectionException.
   * @throws URISyntaxException.
   */
  public void runMatlabCode() throws MatlabConnectionException, URISyntaxException {
    if (proxy == null) { // Initialize MATLAB proxy.
      MatlabProxyFactoryOptions options =
          new MatlabProxyFactoryOptions.Builder().setUsePreviouslyControlledSession(true)
              .setHidden(true).setMatlabLocation(null).build();
      proxy = new MatlabProxyFactory(options).getProxy();
    }
    String[] matlabFields = MATLAB_FILE.split("/");
    String matlabFilename = matlabFields[2];
    // Set the basepath to the MATLAB code path.
    basePath =
        new File(GraphServlet.class.getClassLoader().getResource(MATLAB_FILE).toURI())
            .getAbsolutePath();
    basePath = basePath.substring(0, basePath.indexOf(matlabFilename));
  }

  /**
   * Given graph edges string format, load them into graphs matrix.
   * 
   * @param data is the graph data where each line represents an edge in the following format:
   *        node1,node2,edgeValue.
   */
  public static double[][] loadGraph(String data) {
    // '-' is the delimiter used when sending graph edges.
    nodesNumber = 0;
    String[] edges = data.split("-");
    double[][] graph = new double[edges.length][3];
    int index = 0;
    for (String edge : edges) {
      if (edge.startsWith(",")) {
        edge = edge.substring(1);
      }
      edge = edge.trim();
      if (edge.length() == 0) {
        continue;
      }
      String[] nodes = edge.split(",");
      graph[index][0] = Integer.parseInt(nodes[0]);
      graph[index][1] = Integer.parseInt(nodes[1]);
      graph[index][2] = Integer.parseInt(nodes[2]);
      // Set the number of nodes equal to the max node id,
      // as the nodes are numbered from 1 to nodesNumber.
      nodesNumber = (int) Math.max(nodesNumber, graph[index][0]);
      nodesNumber = (int) Math.max(nodesNumber, graph[index][1]);
      index++;
    }
    return graph;
  }

  /**
   * Compare whether the two graphs are the same or not.
   * 
   * @param graph1 graph1 edges.
   * @param graph2 graph2 edges.
   * @return true if the graphs are the same, otherwise return false.
   */
  public boolean compareGraphs(double[][] graph1, double[][] graph2) {
    if (graph1 == null || graph2 == null) {
      return false;
    }
    for (int i = 0; i < graph1.length; i++) {
      if ((graph1[i][0] != graph2[i][0]) || (graph1[i][1] != graph2[i][1])) {
        return false;
      }
    }
    return true;
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response).
   */
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    PrintWriter out = response.getWriter();
    String parameters = "";
    Enumeration<String> keys = request.getParameterNames();
    int index = 0;
    String[] matlabParameters = new String[5];
    // Loop over each send parameter and add them to MATLAB parameter array.
    while (keys.hasMoreElements()) {
      String key = (String) keys.nextElement();
      // Get parameters in the request.
      parameters = request.getParameter(key);
      if (key.equalsIgnoreCase(GRAPH1_PARAMATER_KEY)) { // First graph.
        graph1 = loadGraph(parameters); // Load data to graph1.
      } else if (key.equalsIgnoreCase(GRAPH2_PARAMATER_KEY)) { // Second graph.
        graph2 = loadGraph(parameters); // Load data to graph2.
      } else { // Other parameters.
        matlabParameters[index] = parameters;
      }
      index++;
    }
    try {
      if (compareGraphs(graph1, prevGraph1) && compareGraphs(graph2, prevGraph2)
          && prevK == Integer.parseInt(matlabParameters[2])
          && prevMeasure.equalsIgnoreCase(matlabParameters[3])) {
        // If same graph with same parameters but different regions,
        // re-use previous results.
        int selectedRegionNumber = Integer.parseInt(matlabParameters[4]);
        double[] nodesDistortionSelected = new double[nodesNumber];
        for (int i = 0; i < nodesNumber; i++) {
          nodesDistortionSelected[i] =
              prevNodesDistortionValues[(selectedRegionNumber - 1) * nodesNumber + i];
        }
        // Load graph2.
        RegionSelector regionsGraph2 = new RegionSelector(nodesDistortionSelected, graph2);
        // Select top-region_num from graph2 results.
        HashMap<Integer, String[]> graph2Results =
            regionsGraph2.getRegions(REGION_NUM, MAX_NODES, MAX_OVERLAP);
        // Load graph1.
        RegionSelector regionsGraph1 = new RegionSelector(nodesDistortionSelected, graph1);
        // Select from graph1 same nodes as graph2 but with their new edges
        // in graph1.
        HashMap<Integer, String[]> graph1Results =
            regionsGraph1.getMapping(graph2Results, regionsGraph1.getGraph());
        String[] graph1ResultsRegion = graph1Results.get(REGION_SELECTOR);
        String[] graph2ResultsRegion = graph2Results.get(REGION_SELECTOR);
        for (int i = 0; i < nodesNumber; i++) {
          // Get the nodes colors.
          out.print(prevNodesColors[(selectedRegionNumber - 1) * nodesNumber + i] + ",");
        }
        // Write selected regions to the response.
        out.print("_");
        for (int i = 0; i < graph1ResultsRegion.length; i++) {
          // Get the nodes colors.
          out.print(graph1ResultsRegion[i] + "-");
        }
        out.print("_");
        for (int i = 0; i < graph2ResultsRegion.length; i++) {
          out.print(graph2ResultsRegion[i] + "-"); // Get the nodes
          // colors.
        }
        return;
      }
      // Add code path to the MATLAB environment.
      proxy.eval("addpath('" + basePath + "')");
      // Store the graphs in the MATLAB format.
      MatlabTypeConverter processor = new MatlabTypeConverter(proxy);
      processor.setNumericArray("G1", new MatlabNumericArray(graph1, null));
      processor.setNumericArray("G2", new MatlabNumericArray(graph2, null));
      int k = Integer.parseInt(matlabParameters[2]);
      // Run the visualize_map code.
      proxy.eval("[nodes_colors, nodes_values] = visualize_map(G1,G2," + k + "," + REGION_NUM
          + ",'" + matlabParameters[3] + "');");
      int selectedRegionNumber = Integer.parseInt(matlabParameters[4]);
      // MATLAB codes return nodes_colors and nodesDistortionValues
      // as 1D array by stacking the 2D matrix column wise.
      String[] nodesColors = (String[]) proxy.getVariable("nodes_colors");
      for (int i = 0; i < nodesNumber; i++) {
        // Get the nodes colors.
        out.print(nodesColors[(selectedRegionNumber - 1) * nodesNumber + i] + ",");
      }
      double[] nodesDistortionValues = (double[]) proxy.getVariable("nodes_values");
      double[] nodesDistortionSelected = new double[nodesNumber];
      for (int i = 0; i < nodesNumber; i++) {
        nodesDistortionSelected[i] =
            nodesDistortionValues[(selectedRegionNumber - 1) * nodesNumber + i];
      }
      // Load graph2.
      RegionSelector regionsGraph2 = new RegionSelector(nodesDistortionSelected, graph2);
      // Select top-region_num from graph2 results.
      HashMap<Integer, String[]> graph2Results =
          regionsGraph2.getRegions(REGION_NUM, MAX_NODES, MAX_OVERLAP);
      // Load graph1.
      RegionSelector regionsGraph1 = new RegionSelector(nodesDistortionSelected, graph1);
      // Select from graph1 same nodes as graph2 but with their new edges
      // in graph1.
      HashMap<Integer, String[]> graph1Results =
          regionsGraph1.getMapping(graph2Results, regionsGraph1.getGraph());
      // Select the region specified by the user.
      String[] graph1ResultsRegion = graph1Results.get(REGION_SELECTOR);
      String[] graph2ResultsRegion = graph2Results.get(REGION_SELECTOR);
      // Write selected regions to the response.
      out.print("_");
      for (int i = 0; i < graph1ResultsRegion.length; i++) {
        out.print(graph1ResultsRegion[i] + "-"); // Get the nodes
        // colors.
      }
      out.print("_");
      for (int i = 0; i < graph2ResultsRegion.length; i++) {
        out.print(graph2ResultsRegion[i] + "-"); // Get the nodes colors.
      }
      // Store the current results for checking next time.
      prevGraph1 = graph1;
      prevGraph2 = graph2;
      prevK = Integer.parseInt(matlabParameters[2]);
      prevMeasure = matlabParameters[3];
      prevNodesColors = nodesColors;
      prevNodesDistortionValues = nodesDistortionValues;
      // proxy.disconnect();
    } catch (MatlabInvocationException ex) {
      out.println(ex.getMessage());
    }
    out.close();
  }

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response).
   */
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {}

}
