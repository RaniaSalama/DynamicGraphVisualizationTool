package server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.ArrayList;
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
 * Servlet implementation class GraphServlet to handle the user request to color
 * the graph with distortion values.
 */
@SuppressWarnings("serial")
public class GraphServlet extends HttpServlet {
	// graph1 parameter key name.
	private static final String GRAPH1_PARAMATER_KEY = "graph1file";
	// graph2 parameter key name.
	private static final String GRAPH2_PARAMATER_KEY = "graph2file";
	// Number of regions to calculate.
	private static int REGION_NUM = 10;
	// Max number of nodes per region.
	private static int MAX_NODES = 16;
	// Select the highest region from each one of the singular vectors.
	private static int REGION_SELECTOR = 1;
	// MATLAB file to run.
	static String MATLAB_FILE = "server/matlab/visualize_map.m";
	// Remove nodes with delta change below this threshold.
	private static final double DEFAULT_THRESHOLD = 0.0;
	// Biased k used in Biased BFS.
	private static int BIASEDK = 5;
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
	 * Servlet constructor initializes the MATLAB proxy and sets the MATLAB
	 * path.
	 * 
	 * @throws MatlabConnectionException
	 * @throws URISyntaxException.
	 */
	public GraphServlet() throws MatlabConnectionException, URISyntaxException {
		super();
		runMatlabCode();
	}

	/**
	 * Servlet constructor initializes the MATLAB proxy and sets the MATLAB
	 * path.
	 * 
	 * @throws MatlabConnectionException
	 * @throws URISyntaxException.
	 */
	public GraphServlet(String MATLABPath) throws MatlabConnectionException,
			URISyntaxException {
		super();
		GraphServlet.MATLAB_FILE = MATLABPath;
		runMatlabCodeFromCMD(MATLABPath);
	}

	/**
	 * runMatlabCode initializes the MATLAB proxy and sets the basepath to
	 * MATLAB code path.
	 * 
	 * @throws MatlabConnectionException.
	 * @throws URISyntaxException.
	 */
	public void runMatlabCodeFromCMD(String MATLABPath)
			throws MatlabConnectionException, URISyntaxException {
		if (proxy == null) { // Initialize MATLAB proxy.
			MatlabProxyFactoryOptions options = new MatlabProxyFactoryOptions.Builder()
					.setUsePreviouslyControlledSession(true).setHidden(true)
					.setMatlabLocation(null).build();
			proxy = new MatlabProxyFactory(options).getProxy();
		}
		System.out.println("MATLAB PATH = " + MATLABPath);
		// Set the basepath to the MATLAB code path.
		basePath = MATLABPath;
	}

	/**
	 * Get biasedk value.
	 * 
	 * @return biasedk value.
	 */
	public static double getBiasedK() {
		return BIASEDK;
	}

	/**
	 * runMatlabCode initializes the MATLAB proxy and sets the basepath to
	 * MATLAB code path.
	 * 
	 * @throws MatlabConnectionException.
	 * @throws URISyntaxException.
	 */
	public void runMatlabCode() throws MatlabConnectionException,
			URISyntaxException {
		if (proxy == null) { // Initialize MATLAB proxy.
			MatlabProxyFactoryOptions options = new MatlabProxyFactoryOptions.Builder()
					.setUsePreviouslyControlledSession(true).setHidden(true)
					.setMatlabLocation(null).build();
			proxy = new MatlabProxyFactory(options).getProxy();
		}
		String[] matlabFields = MATLAB_FILE.split("/");
		String matlabFilename = matlabFields[2];
		// Set the basepath to the MATLAB code path.
		basePath = new File(GraphServlet.class.getClassLoader()
				.getResource(MATLAB_FILE).toURI()).getAbsolutePath();
		basePath = basePath.substring(0, basePath.indexOf(matlabFilename));
	}

	/**
	 * Given graph edges string format, load them into graphs matrix.
	 * 
	 * @param data
	 *            is the graph data where each line represents an edge in the
	 *            following format: node1,node2,edgeValue.
	 */
	public static double[][] loadGraph(String data, PrintWriter out) {
		out.println(data);
		out.close();
		// '-' is the delimiter used when sending graph edges.
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
	 * Load graph given string data representing the graph.
	 * 
	 * @param data
	 *            representing the graph.
	 * @return double[][] graph where each index has three values, which are
	 *         from node id, to node id and the weight of the edge.
	 */
	public static double[][] loadGraph(String data) {
		// '-' is the delimiter used when sending graph edges.
		String[] edges = data.split("-");
		double[][] graph = new double[edges.length][3];
		int index = 0;
		for (String edge : edges) {
			if (edge.startsWith(",")) { // Extra , in the start.
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
	 * @param graph1
	 *            graph1 edges.
	 * @param graph2
	 *            graph2 edges.
	 * @return true if the graphs are the same, otherwise return false.
	 */
	public boolean compareGraphs(double[][] graph1, double[][] graph2) {
		if (graph1 == null || graph2 == null) {
			return false;
		}
		for (int i = 0; i < graph1.length; i++) {
			if ((graph1[i][0] != graph2[i][0])
					|| (graph1[i][1] != graph2[i][1])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Run the spectral method.
	 * 
	 * @param matlabParameters
	 *            used to run the spectral method.
	 * @return ArrayList of nodes colors, graph1 region and graph2 region.
	 * @throws MatlabInvocationException
	 */
	public ArrayList<String[]> runSpectralMethod(String[] matlabParameters,
			double threshold) throws MatlabInvocationException {
		// Add code path to the MATLAB environment.
		proxy.eval("addpath('" + basePath + "')");
		// Store the graphs in the MATLAB format.
		MatlabTypeConverter processor = new MatlabTypeConverter(proxy);
		processor.setNumericArray("G1", new MatlabNumericArray(graph1, null));
		processor.setNumericArray("G2", new MatlabNumericArray(graph2, null));
		int k = Integer.parseInt(matlabParameters[2]);
		// Run the visualize_map code.
		proxy.eval("[nodes_colors, nodes_values] = visualize_map(G1,G2," + k
				+ "," + REGION_NUM + ",'" + matlabParameters[3] + "');");
		int selectedRegionNumber = Integer.parseInt(matlabParameters[4]);
		// MATLAB codes return nodes_colors and nodesDistortionValues
		// as 1D array by stacking the 2D matrix column wise.
		String[] nodesColors = (String[]) proxy.getVariable("nodes_colors");
		double[] nodesDistortionValues = (double[]) proxy
				.getVariable("nodes_values");
		prevNodesDistortionValues = nodesDistortionValues;
		ArrayList<SpectralMethodRegionSelector> regions = getRegionUI(
				nodesDistortionValues, selectedRegionNumber, threshold);
		SpectralMethodRegionSelector regionsGraph1 = regions.get(0);
		SpectralMethodRegionSelector regionsGraph2 = regions.get(1);
		// Select top-region_num from graph2 results.
		HashMap<Integer, String[]> graph2Results = regionsGraph2.getRegions(
				REGION_SELECTOR, MAX_NODES, 2);
		// Select from graph1 same nodes as graph2 but with their new edges
		// in graph1.
		HashMap<Integer, String[]> graph1Results = regionsGraph1.getMapping(
				graph2Results, regionsGraph1.getGraph(),
				regionsGraph2.getGraph(), regionsGraph2.getNodeMapping(),
				REGION_SELECTOR);
		// Select the region specified by the user.
		String[] graph1ResultsRegion = graph1Results.get(REGION_SELECTOR);
		String[] graph2ResultsRegion = graph2Results.get(REGION_SELECTOR);
		// Store the result in the graphsColors array list.
		ArrayList<String[]> graphsColors = new ArrayList<String[]>();
		graphsColors.add(nodesColors);
		graphsColors.add(graph1ResultsRegion);
		graphsColors.add(graph2ResultsRegion);
		return graphsColors;
	}

	/**
	 * Run the spectral method.
	 * 
	 * @param matlabParameters
	 *            used to run the spectral method.
	 * @return ArrayList of nodes colors, graph1 region and graph2 region.
	 * @throws MatlabInvocationException
	 */
	public ArrayList<String[]> runSpectralMethodEvalution(
			String[] matlabParameters, double threshold)
			throws MatlabInvocationException {
		// Add code path to the MATLAB environment.
		proxy.eval("addpath('" + basePath + "')");
		// Store the graphs in the MATLAB format.
		MatlabTypeConverter processor = new MatlabTypeConverter(proxy);
		processor.setNumericArray("G1", new MatlabNumericArray(graph1, null));
		processor.setNumericArray("G2", new MatlabNumericArray(graph2, null));
		int k = Integer.parseInt(matlabParameters[2]);
		// Run the visualize_map code.
		proxy.eval("[nodes_colors, nodes_values] = visualize_map(G1,G2," + k
				+ "," + REGION_NUM + ",'" + matlabParameters[3] + "');");
		int selectedRegionNumber = Integer.parseInt(matlabParameters[4]);
		// MATLAB codes return nodes_colors and nodesDistortionValues
		// as 1D array by stacking the 2D matrix column wise.
		String[] nodesColors = (String[]) proxy.getVariable("nodes_colors");
		double[] nodesDistortionValues = (double[]) proxy
				.getVariable("nodes_values");
		prevNodesDistortionValues = nodesDistortionValues;
		ArrayList<SpectralMethodRegionSelector> regions = getRegionNoThresholding(
				nodesDistortionValues, selectedRegionNumber, threshold);
		if (regions == null) {
			return null;
		}
		SpectralMethodRegionSelector regionsGraph1 = regions.get(0);
		SpectralMethodRegionSelector regionsGraph2 = regions.get(1);
		// Select top-region_num from graph2 results.
		HashMap<Integer, String[]> graph2Results = regionsGraph2.getRegions(
				REGION_SELECTOR, MAX_NODES, 2);
		// Select from graph1 same nodes as graph2 but with their new edges
		// in graph1.
		HashMap<Integer, String[]> graph1Results = regionsGraph1.getMapping(
				graph2Results, regionsGraph1.getGraph(),
				regionsGraph2.getGraph(), regionsGraph2.getNodeMapping(),
				REGION_SELECTOR);
		// Select the region specified by the user.
		String[] graph1ResultsRegion = graph1Results.get(REGION_SELECTOR);
		String[] graph2ResultsRegion = graph2Results.get(REGION_SELECTOR);
		// Store the result in the graphsColors array list.
		ArrayList<String[]> graphsColors = new ArrayList<String[]>();
		graphsColors.add(nodesColors);
		graphsColors.add(graph1ResultsRegion);
		graphsColors.add(graph2ResultsRegion);
		return graphsColors;
	}

	/**
	 * Run the spectral method.
	 * 
	 * @param matlabParameters
	 *            used to run the spectral method.
	 * @return ArrayList of nodes colors, graph1 region and graph2 region.
	 * @throws MatlabInvocationException
	 */
	public ArrayList<String[]> runSpectralMethodForThresholding(
			String[] matlabParameters, double threshold)
			throws MatlabInvocationException {
		// Add code path to the MATLAB environment.
		proxy.eval("addpath('" + basePath + "')");
		// Store the graphs in the MATLAB format.
		MatlabTypeConverter processor = new MatlabTypeConverter(proxy);
		processor.setNumericArray("G1", new MatlabNumericArray(graph1, null));
		processor.setNumericArray("G2", new MatlabNumericArray(graph2, null));
		int k = Integer.parseInt(matlabParameters[2]);
		// Run the visualize_map code.
		proxy.eval("[nodes_colors, nodes_values] = visualize_map(G1,G2," + k
				+ "," + REGION_NUM + ",'" + matlabParameters[3] + "');");
		// MATLAB codes return nodes_colors and nodesDistortionValues
		// as 1D array by stacking the 2D matrix column wise.
		String[] nodesColors = (String[]) proxy.getVariable("nodes_colors");
		double[] nodesDistortionValues = (double[]) proxy
				.getVariable("nodes_values");
		prevNodesDistortionValues = nodesDistortionValues;
		ArrayList<String[]> graphsColors = new ArrayList<String[]>();
		graphsColors.add(nodesColors);
		return graphsColors;
	}

	public double[][] copyGraph(double[][] graph) {
		double[][] newGraph = new double[graph.length][graph[0].length];
		for (int i = 0; i < newGraph.length; i++) {
			for (int j = 0; j < newGraph[i].length; j++) {
				newGraph[i][j] = graph[i][j];
			}
		}
		return newGraph;
	}

	/**
	 * Get region given the nodes distortion values and the selected region
	 * number.
	 * 
	 * @param nodesDistortionValues
	 *            distortion values of the nodes.
	 * @param selectedRegionNumber
	 *            selected region number.
	 * @return the selected region in graph1 and graph2.
	 */
	public ArrayList<SpectralMethodRegionSelector> getRegion(
			double[] nodesDistortionValues, int selectedRegionNumber,
			double step) {
		if (nodesDistortionValues.length == 0) {
			return null;
		}
		double[] nodesDistortionSelected = new double[nodesNumber];
		for (int i = 0; i < nodesNumber; i++) { // Store the distortion value of
												// the corresponding
												// singular vector.
			nodesDistortionSelected[i] = nodesDistortionValues[(selectedRegionNumber - 1)
					* nodesNumber + i];
		}
		// Load graph2.
		SpectralMethodRegionSelector regionsGraph2 = new SpectralMethodRegionSelector(
				nodesDistortionSelected, copyGraph(graph2));
		// Load graph1.
		SpectralMethodRegionSelector regionsGraph1 = new SpectralMethodRegionSelector(
				nodesDistortionSelected, copyGraph(graph1));
		regionsGraph2.calculateDeltaGraph(regionsGraph1.getGraph(),
				regionsGraph1.getNodeMapping(), regionsGraph2.getGraph(),
				regionsGraph2.getNodeMapping());
		regionsGraph2.removeNodesBelowThreshold(step, nodesNumber,
				regionsGraph1.getGraph(), regionsGraph1.getNodeMapping(),
				regionsGraph1.getNodesList(), regionsGraph2.getGraph(),
				regionsGraph2.getNodeMapping(), regionsGraph2.getNodesList());
		ArrayList<SpectralMethodRegionSelector> graphsRegions = new ArrayList<SpectralMethodRegionSelector>();
		graphsRegions.add(regionsGraph1);
		graphsRegions.add(regionsGraph2);
		return graphsRegions;
	}

	/**
	 * Get region given the nodes distortion values and the selected region
	 * number.
	 * 
	 * @param nodesDistortionValues
	 *            distortion values of the nodes.
	 * @param selectedRegionNumber
	 *            selected region number.
	 * @return the selected region in graph1 and graph2.
	 */
	public ArrayList<SpectralMethodRegionSelector> getRegionNoThresholding(
			double[] nodesDistortionValues, int selectedRegionNumber,
			double step) {
		if (nodesDistortionValues.length == 0) {
			return null;
		}
		double[] nodesDistortionSelected = new double[nodesNumber];
		for (int i = 0; i < nodesNumber; i++) { // Store the distortion value of
												// the corresponding
												// singular vector.
			nodesDistortionSelected[i] = nodesDistortionValues[(selectedRegionNumber - 1)
					* nodesNumber + i];
		}
		// Load graph2.
		SpectralMethodRegionSelector regionsGraph2 = new SpectralMethodRegionSelector(
				nodesDistortionSelected, copyGraph(graph2));
		// Load graph1.
		SpectralMethodRegionSelector regionsGraph1 = new SpectralMethodRegionSelector(
				nodesDistortionSelected, copyGraph(graph1));
		regionsGraph2.calculateDeltaGraph(regionsGraph1.getGraph(),
				regionsGraph1.getNodeMapping(), regionsGraph2.getGraph(),
				regionsGraph2.getNodeMapping());
		ArrayList<SpectralMethodRegionSelector> graphsRegions = new ArrayList<SpectralMethodRegionSelector>();
		graphsRegions.add(regionsGraph1);
		graphsRegions.add(regionsGraph2);
		return graphsRegions;
	}

	/**
	 * Get region given the nodes distortion values and the selected region
	 * number.
	 * 
	 * @param nodesDistortionValues
	 *            distortion values of the nodes.
	 * @param selectedRegionNumber
	 *            selected region number.
	 * @return the selected region in graph1 and graph2.
	 */
	public ArrayList<SpectralMethodRegionSelector> getRegionUI(
			double[] nodesDistortionValues, int selectedRegionNumber,
			double step) {
		if (nodesDistortionValues.length == 0) {
			return null;
		}
		double[] nodesDistortionSelected = new double[nodesNumber];
		for (int i = 0; i < nodesNumber; i++) { // Store the distortion value of
												// the corresponding
												// singular vector.
			nodesDistortionSelected[i] = nodesDistortionValues[(selectedRegionNumber - 1)
					* nodesNumber + i];
		}
		// Load graph2.
		SpectralMethodRegionSelector regionsGraph2 = new SpectralMethodRegionSelector(
				nodesDistortionSelected, copyGraph(graph2));
		// Load graph1.
		SpectralMethodRegionSelector regionsGraph1 = new SpectralMethodRegionSelector(
				nodesDistortionSelected, copyGraph(graph1));
		ArrayList<SpectralMethodRegionSelector> graphsRegions = new ArrayList<SpectralMethodRegionSelector>();
		graphsRegions.add(regionsGraph1);
		graphsRegions.add(regionsGraph2);
		return graphsRegions;
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response).
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		nodesNumber = 0;
		PrintWriter out = response.getWriter();
		String parameters = "";
		Enumeration<String> keys = request.getParameterNames();
		int index = 0;
		String[] toolParameters = new String[5];
		// Loop over each send parameter and add them to MATLAB parameter array.
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			// Get parameters in the request.
			parameters = request.getParameter(key);
			if (key.equalsIgnoreCase(GRAPH1_PARAMATER_KEY)) { // First graph.
				graph1 = loadGraph(parameters); // Load data to graph1.
			} else if (key.equalsIgnoreCase(GRAPH2_PARAMATER_KEY)) { // Second
																		// graph.
				graph2 = loadGraph(parameters); // Load data to graph2.
			} else { // Other parameters.
				toolParameters[index] = parameters;
			}
			index++;
		}
		String methodName = toolParameters[3];
		if (methodName.compareToIgnoreCase("Top-changing-vertices-BFS") == 0
				|| methodName
						.compareToIgnoreCase("Top-changing-vertices-BFSBiased") == 0
				|| methodName
						.compareToIgnoreCase("Top-changing-vertices-BFSPQ") == 0) {
			TopChangingVerticesCalculator calculator = new TopChangingVerticesCalculator();
			ArrayList<ArrayList<String>> regions = null;
			if (methodName.compareToIgnoreCase("Top-changing-vertices-BFS") == 0) {
				regions = calculator.runWithUI(REGION_NUM, MAX_NODES, graph1,
						graph2, Integer.parseInt(toolParameters[4]) - 1, 0, 0);
			} else if (methodName
					.compareToIgnoreCase("Top-changing-vertices-BFSBiased") == 0) {
				regions = calculator.runWithUI(REGION_NUM, MAX_NODES, graph1,
						graph2, Integer.parseInt(toolParameters[4]) - 1, 1,
						BIASEDK);
			} else if (methodName
					.compareToIgnoreCase("Top-changing-vertices-BFSPQ") == 0) {
				regions = calculator.runWithUI(REGION_NUM, MAX_NODES, graph1,
						graph2, Integer.parseInt(toolParameters[4]) - 1, 2, 0);
			}
			ArrayList<String> graph1ResultsRegion = regions.get(0);
			ArrayList<String> graph2ResultsRegion = regions.get(1);
			ArrayList<String> colors = regions.get(2);
			HashMap<Integer, String> colorMap = new HashMap<Integer, String>();
			for (String color : colors) {
				String[] splits = color.split(",");
				colorMap.put((int) Double.parseDouble(splits[0]), splits[1]);
			}
			for (int i = 1; i <= nodesNumber; i++) {
				// Get the nodes colors.
				out.print(colorMap.get(i) + ",");
			}
			// Write selected regions to the response.
			out.print("_");
			for (String graph1Edge : graph1ResultsRegion) {
				System.out.println(graph1Edge);
				out.print(graph1Edge + "-");
			}
			out.print("_");
			for (String graph2Edge : graph2ResultsRegion) {
				System.out.println(graph2Edge);
				out.print(graph2Edge + "-");
			}
		} else if (methodName.compareToIgnoreCase("Top-changing-regions-BFS") == 0
				|| methodName
						.compareToIgnoreCase("Top-changing-regions-BFSBiased") == 0
				|| methodName.compareToIgnoreCase("Top-changing-regions-BFSPQ") == 0) {
			TopChangingVerticesExhaustiveCalculator calculator = new TopChangingVerticesExhaustiveCalculator();
			ArrayList<ArrayList<String>> regions = null;
			if (methodName.compareToIgnoreCase("Top-changing-regions-BFS") == 0) {
				regions = calculator.runWithUI(REGION_NUM, MAX_NODES, graph1,
						graph2, Integer.parseInt(toolParameters[4]) - 1, 0, 0);
			} else if (methodName
					.compareToIgnoreCase("Top-changing-regions-BFSBiased") == 0) {
				regions = calculator.runWithUI(REGION_NUM, MAX_NODES, graph1,
						graph2, Integer.parseInt(toolParameters[4]) - 1, 1,
						BIASEDK);
			} else if (methodName
					.compareToIgnoreCase("Top-changing-regions-BFSPQ") == 0) {
				regions = calculator.runWithUI(REGION_NUM, MAX_NODES, graph1,
						graph2, Integer.parseInt(toolParameters[4]) - 1, 2, 0);
			}
			ArrayList<String> graph1ResultsRegion = regions.get(0);
			ArrayList<String> graph2ResultsRegion = regions.get(1);
			ArrayList<String> colors = regions.get(2);
			HashMap<Integer, String> colorMap = new HashMap<Integer, String>();
			for (String color : colors) {
				String[] splits = color.split(",");
				colorMap.put((int) Double.parseDouble(splits[0]), splits[1]);
			}
			for (int i = 1; i <= nodesNumber; i++) {
				// Get the nodes colors.
				out.print(colorMap.get(i) + ",");
			}
			// Write selected regions to the response.
			out.print("_");
			for (String graph1Edge : graph1ResultsRegion) {
				out.print(graph1Edge + "-");
			}
			out.print("_");
			for (String graph2Edge : graph2ResultsRegion) {
				out.print(graph2Edge + "-");
			}
		} else if (methodName.compareToIgnoreCase("Max-changin-radius") == 0
				|| methodName
						.compareToIgnoreCase("Max-changin-radius-regionSize") == 0) {
			MaxChangingRadiusCalculator calculator = new MaxChangingRadiusCalculator();
			ArrayList<ArrayList<String>> regions = null;
			if (methodName.compareToIgnoreCase("Max-changin-radius") == 0) {
				regions = calculator.runWithUI(REGION_NUM, MAX_NODES, graph1,
						graph2, Integer.parseInt(toolParameters[4]) - 1, 0);
			} else if (methodName
					.compareToIgnoreCase("Max-changin-radius-regionSize") == 0) {
				regions = calculator.runWithUI(REGION_NUM, MAX_NODES, graph1,
						graph2, Integer.parseInt(toolParameters[4]) - 1, 1);
			}
			ArrayList<String> graph1ResultsRegion = regions.get(0);
			ArrayList<String> graph2ResultsRegion = regions.get(1);
			ArrayList<String> colors = regions.get(2);
			HashMap<Integer, String> colorMap = new HashMap<Integer, String>();
			for (String color : colors) {
				String[] splits = color.split(",");
				colorMap.put((int) Double.parseDouble(splits[0]), splits[1]);
			}
			for (int i = 1; i <= nodesNumber; i++) {
				// Get the nodes colors.
				out.print(colorMap.get(i) + ",");
			}
			// Write selected regions to the response.
			out.print("_");
			for (String graph1Edge : graph1ResultsRegion) {
				out.print(graph1Edge + "-");
			}
			out.print("_");
			for (String graph2Edge : graph2ResultsRegion) {
				out.print(graph2Edge + "-");
			}
		} else if ((methodName.compareToIgnoreCase("area-based") == 0)
				|| (methodName.compareToIgnoreCase("conformal-based") == 0) || (methodName.compareToIgnoreCase("E1") == 0)) {
			try {

				if (compareGraphs(graph1, prevGraph1)
						&& compareGraphs(graph2, prevGraph2)
						&& prevK == Integer.parseInt(toolParameters[2])
						&& prevMeasure.equalsIgnoreCase(toolParameters[3])) {
					// If same graph with same parameters but different regions,
					// re-use previous results.
					int selectedRegionNumber = Integer
							.parseInt(toolParameters[4]);
					ArrayList<SpectralMethodRegionSelector> regions = getRegion(
							prevNodesDistortionValues, selectedRegionNumber,
							DEFAULT_THRESHOLD);
					SpectralMethodRegionSelector regionsGraph1 = regions.get(0);
					SpectralMethodRegionSelector regionsGraph2 = regions.get(1);
					// Select top-region_num from graph2 results.
					HashMap<Integer, String[]> graph2Results = regionsGraph2
							.getRegions(REGION_SELECTOR, MAX_NODES, 2);
					// Select from graph1 same nodes as graph2 but with their
					// new edges
					// in graph1.
					HashMap<Integer, String[]> graph1Results = regionsGraph1
							.getMapping(graph2Results,
									regionsGraph1.getGraph(),
									regionsGraph2.getGraph(),
									regionsGraph2.getNodeMapping(),
									REGION_SELECTOR);
					// Select the region specified by the user.
					String[] graph1ResultsRegion = graph1Results
							.get(REGION_SELECTOR);
					String[] graph2ResultsRegion = graph2Results
							.get(REGION_SELECTOR);
					for (int i = 0; i < nodesNumber; i++) {
						// Get the nodes colors.
						out.print(prevNodesColors[(selectedRegionNumber - 1)
								* nodesNumber + i]
								+ ",");
					}
					// Write selected regions to the response.
					out.print("_");
					for (int i = 0; i < graph1ResultsRegion.length; i++) {
						// Get the nodes colors.
						out.print(graph1ResultsRegion[i] + "-");
					}
					out.print("_");
					for (int i = 0; i < graph2ResultsRegion.length; i++) {
						out.print(graph2ResultsRegion[i] + "-"); // Get the
																	// nodes
						// colors.
					}
					return;
				}
				ArrayList<String[]> graphColors = runSpectralMethod(
						toolParameters, DEFAULT_THRESHOLD);
				String[] nodesColors = graphColors.get(0);
				if (nodesColors.length == 0) {
					System.out.println("Emptry color array!");
					System.exit(0);
				} else {
					int selectedRegionNumber = Integer
							.parseInt(toolParameters[4]);
					for (int i = 0; i < nodesNumber; i++) {
						// Get the nodes colors.
						out.print(nodesColors[(selectedRegionNumber - 1)
								* nodesNumber + i]
								+ ",");
					}
					// Write selected regions to the response.
					out.print("_");
					String[] graph1ResultsRegion = graphColors.get(1);
					for (int i = 0; i < graph1ResultsRegion.length; i++) {
						out.print(graph1ResultsRegion[i] + "-"); // Get the
																	// nodes
						// colors.
					}
					out.print("_");
					String[] graph2ResultsRegion = graphColors.get(2);
					for (int i = 0; i < graph2ResultsRegion.length; i++) {
						out.print(graph2ResultsRegion[i] + "-"); // Get the
																	// nodes
																	// colors.
					}
					// Store the current results for checking next time.
					prevGraph1 = graph1;
					prevGraph2 = graph2;
					prevK = Integer.parseInt(toolParameters[2]);
					prevMeasure = toolParameters[3];
					prevNodesColors = nodesColors;
				}
				// proxy.disconnect();
			} catch (MatlabInvocationException ex) {
				out.println(ex.getMessage());
			}
		}
		out.close();
	}

	/**
	 * Run spectral method using different values of k and report the one with
	 * maximum evaluation measure.
	 * 
	 * @param inputFile1
	 *            graph1 input file.
	 * @param inputFile2
	 *            graph2 input file.
	 * @throws IOException
	 * @throws MatlabConnectionException
	 * @throws MatlabInvocationException
	 * @throws URISyntaxException
	 */
	public void runEvaluation(String inputFile1, String inputFile2)
			throws IOException, MatlabConnectionException,
			MatlabInvocationException, URISyntaxException {
		double maxK = 0.0; // k value that has the maximum distortion value.
		double maxDistortionValue = -1;
		String maxResult = ""; // Max distortion value string format.
		BufferedReader reader1 = new BufferedReader(new FileReader(inputFile1));
		BufferedReader reader2 = new BufferedReader(new FileReader(inputFile2));
		graph1 = loadGraph(reader1.readLine());
		graph2 = loadGraph(reader2.readLine());
		runMatlabCode();
		for (int k = 12; k <= 500; k += 2) { // Loop over k values to choose the
												// best one.
			try {
				System.out.println("============" + k + "==============");
				String[] matlabParameters = new String[5];
				matlabParameters[2] = k + "";
				matlabParameters[3] = "conformal-based";
				matlabParameters[4] = 1 + "";
				// Run the spectral method with the matlabParameters settings.
				runSpectralMethod(matlabParameters, DEFAULT_THRESHOLD);
				double distortionSum = 0;
				String result = "";
				double[] nodesDistortionValues = (double[]) proxy
						.getVariable("nodes_values");
				for (int selectedRegionNumber = 1; selectedRegionNumber <= 10; selectedRegionNumber++) { // Get
																											// top
																											// 10
																											// regions.
					double[] nodesDistortionSelected = new double[nodesNumber];
					for (int i = 0; i < nodesNumber; i++) {
						nodesDistortionSelected[i] = nodesDistortionValues[(selectedRegionNumber - 1)
								* nodesNumber + i];
					}
					ArrayList<SpectralMethodRegionSelector> regions = getRegion(
							nodesDistortionValues, selectedRegionNumber,
							DEFAULT_THRESHOLD);
					SpectralMethodRegionSelector regionsGraph1 = regions.get(0);
					SpectralMethodRegionSelector regionsGraph2 = regions.get(1);
					HashMap<Integer, String[]> graph2Results = regionsGraph2
							.getRegions(REGION_SELECTOR, MAX_NODES, 2);
					// Select from graph1 same nodes as graph2 but with their
					// new edges
					// in graph1.
					regionsGraph1.getMapping(graph2Results,
							regionsGraph1.getGraph(), regionsGraph2.getGraph(),
							regionsGraph2.getNodeMapping(), REGION_SELECTOR);
					distortionSum += regionsGraph1.getRegionChangeValue();
					String valueString = regionsGraph1.getRegionChangeValue()
							+ "";
					String[] splits = valueString.split("\\.");
					valueString = splits[0]
							+ "."
							+ splits[1].substring(0,
									Math.min(3, splits[1].length()));
					result = result + "\nR" + selectedRegionNumber + "="
							+ valueString;
				}
				if (distortionSum > maxDistortionValue) { // If the current k
															// value has better
															// results.
					maxDistortionValue = distortionSum;
					maxResult = result;
					maxK = k;
				}
			} catch (MatlabInvocationException ex) {
				// If K exceeds the number of nodes in the graph, Matlab code
				// throws an exception.
				continue;
			}
		}
		// Print the best result.
		System.out.println("Final = " + maxK);
		System.out.println(maxResult);
		reader1.close();
		reader2.close();
	}

	/**
	 * Run spectral method using different values of k and report the one with
	 * maximum evaluation measures.
	 * 
	 * @param inputFile1
	 *            graph1 input file.
	 * @param inputFile2
	 *            graph2 input file.
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws MatlabInvocationException
	 * @throws MatlabConnectionException
	 */
	public void runEvaluationsWithRespectToRegionSize(String inputFile1,
			String inputFile2, String method) throws IOException,
			URISyntaxException, MatlabInvocationException,
			MatlabConnectionException {
		double[] maxEvaluationMeasures = new double[6]; // Six evaluation
		// measures.
		String[] maxEvaluationMeasuresString = new String[6]; // Evaluation
		// measures
		// string
		// format.
		int[] maxEvaluationMeasuresK = new int[6]; // Value of k at the maximum
		// evaluation measures.
		double[] maxEvaluationMeasuresThreshold = new double[6]; // Value of
		// threshold
		// at the
		// maximum
		// evaluation
		// measures.
		BufferedReader reader1 = new BufferedReader(new FileReader(inputFile1));
		BufferedReader reader2 = new BufferedReader(new FileReader(inputFile2));
		runMatlabCodeFromCMD(GraphServlet.MATLAB_FILE);
		graph1 = loadGraph(reader1.readLine());
		graph2 = loadGraph(reader2.readLine());
		Loop: for (int k = 12; k <= Math.min(400, nodesNumber); k += 2) { // Loop
			// over
			// k
			// values
			// to
			// choose the best one.
			// System.out.println(k);
			try {
				double[] evaluationMeasuresSum = new double[6];
				String[] currentEvaluationMeasuresString = new String[6];
				for (int i = 0; i < currentEvaluationMeasuresString.length; i++) {
					currentEvaluationMeasuresString[i] = "";
				}
				String[] matlabParameters = new String[5];
				matlabParameters[2] = k + "";
				matlabParameters[3] = method;
				matlabParameters[4] = 1 + "";
				runSpectralMethodForThresholding(matlabParameters, 0.0);
				double[] nodesDistortionValues = (double[]) proxy
						.getVariable("nodes_values");
				if (nodesDistortionValues.length == 0) {
					System.out.println("Returned distortions are empty! " + k);
					continue;
				}
				for (int selectedRegionNumber = 1; selectedRegionNumber <= 10; selectedRegionNumber++) {
					double[] nodesDistortionSelected = new double[nodesNumber];
					for (int i = 0; i < nodesNumber; i++) {
						if ((selectedRegionNumber - 1) * nodesNumber + i >= nodesDistortionValues.length) {
							continue Loop;
						}
						nodesDistortionSelected[i] = nodesDistortionValues[(selectedRegionNumber - 1)
								* nodesNumber + i];
					}
					ArrayList<SpectralMethodRegionSelector> regions = getRegion(
							nodesDistortionValues, selectedRegionNumber,
							0.0);
					SpectralMethodRegionSelector regionsGraph1 = regions.get(0);
					SpectralMethodRegionSelector regionsGraph2 = regions.get(1);
					HashMap<Integer, String[]> graph2Results = regionsGraph2
							.getRegions(REGION_SELECTOR, MAX_NODES,
									2);
					// Select from graph1 same nodes as graph2 but with
					// their new edges
					// in graph1.
					regionsGraph1.getMapping(graph2Results,
							regionsGraph1.getGraph(), regionsGraph2.getGraph(),
							regionsGraph2.getNodeMapping(), REGION_SELECTOR);
					double[] currentEvaluationMeasures = regionsGraph1
							.getEvaluationMeasures();
					if (currentEvaluationMeasures == null) {
						System.out.println("Null evaluation measure " + k);
						continue Loop;
					}
					for (int i = 0; i < evaluationMeasuresSum.length; i++) {
						evaluationMeasuresSum[i] += currentEvaluationMeasures[i];
						currentEvaluationMeasuresString[i] = currentEvaluationMeasuresString[i]
								+ "\nR"
								+ selectedRegionNumber
								+ "="
								+ getString(currentEvaluationMeasures[i]);
					}
				}
				for (int i = 0; i < evaluationMeasuresSum.length; i++) {
					// If the current evaluation measures are better, store
					// them.
					if (evaluationMeasuresSum[i] >= maxEvaluationMeasures[i]) {
						maxEvaluationMeasures[i] = evaluationMeasuresSum[i];
						maxEvaluationMeasuresK[i] = k;
						maxEvaluationMeasuresString[i] = currentEvaluationMeasuresString[i];
						maxEvaluationMeasuresThreshold[i] = 0.0;
					}
				}
			} catch (MatlabInvocationException ex) {
				// If K exceeds the number of nodes in the graph, Matlab
				// code throws an exception.
				System.out.println("Error in Matlab " + ex.getMessage());
				continue;
			}
		}
		// Print the best results.
		for (int i = 0; i < maxEvaluationMeasuresString.length; i++) {
			System.out.println("==========================="
					+ maxEvaluationMeasuresK[i] + ","
					+ maxEvaluationMeasuresThreshold[i]
					+ "======================");
			System.out.println(maxEvaluationMeasuresString[i]);
			System.out
					.println("==================================================");
		}
		reader1.close();
		reader2.close();
	}

	public void runEvaluationsWithRespectToRegionSizeWithThresholdingAllBFSs(
			String inputFile1, String inputFile2, double step, String method)
			throws IOException, URISyntaxException, MatlabInvocationException,
			MatlabConnectionException {
		runEvaluationsWithRespectToRegionSizeWithThresholding(inputFile1,
				inputFile2, step, method, 0);
		runEvaluationsWithRespectToRegionSizeWithThresholding(inputFile1,
				inputFile2, step, method, 1);
		runEvaluationsWithRespectToRegionSizeWithThresholding(inputFile1,
				inputFile2, step, method, 2);
	}

	/**
	 * Run spectral method using different values of k and report the one with
	 * maximum evaluation measures while changing the threshold.
	 * 
	 * @param inputFile1
	 *            graph1 input file.
	 * @param inputFile2
	 *            graph2 input file.
	 * @param runsNumber
	 *            number of runs to choose the best threshold.
	 * @param bfsSelection
	 *            0 means BFS, 1 means Biased BFS and 2 means BFS with priority
	 *            queue.
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws MatlabInvocationException
	 * @throws MatlabConnectionException
	 */
	public void runEvaluationsWithRespectToRegionSizeWithThresholding(
			String inputFile1, String inputFile2, double step, String method,
			int bfsSelection) throws IOException, URISyntaxException,
			MatlabInvocationException, MatlabConnectionException {
		double[] maxEvaluationMeasures = new double[6]; // Six evaluation
														// measures.
		String[] maxEvaluationMeasuresString = new String[6]; // Evaluation
																// measures
																// string
																// format.
		int[] maxEvaluationMeasuresK = new int[6]; // Value of k at the maximum
													// evaluation measures.
		double[] maxEvaluationMeasuresThreshold = new double[6]; // Value of
																	// threshold
																	// at the
																	// maximum
																	// evaluation
																	// measures.
		BufferedReader reader1 = new BufferedReader(new FileReader(inputFile1));
		BufferedReader reader2 = new BufferedReader(new FileReader(inputFile2));
		runMatlabCodeFromCMD(GraphServlet.MATLAB_FILE);
		double threshold = 0;
		double maxThreshold = 1;
		graph1 = loadGraph(reader1.readLine());
		graph2 = loadGraph(reader2.readLine());
		while (threshold < maxThreshold) {
			System.out.println(threshold);
			Loop: for (int k = 12; k <= Math.min(400, nodesNumber); k += 10) { // Loop
																				// over
																				// k
																				// values
																				// to
				// choose the best one.
				// System.out.println(k);
				try {
					double[] evaluationMeasuresSum = new double[6];
					String[] currentEvaluationMeasuresString = new String[6];
					for (int i = 0; i < currentEvaluationMeasuresString.length; i++) {
						currentEvaluationMeasuresString[i] = "";
					}
					String[] matlabParameters = new String[5];
					matlabParameters[2] = k + "";
					matlabParameters[3] = method;
					matlabParameters[4] = 1 + "";
					runSpectralMethodForThresholding(matlabParameters,
							threshold);
					double[] nodesDistortionValues = (double[]) proxy
							.getVariable("nodes_values");
					if (nodesDistortionValues.length == 0) {
						System.out.println("Returned distortions are empty! "
								+ k);
						continue;
					}
					for (int selectedRegionNumber = 1; selectedRegionNumber <= 10; selectedRegionNumber++) {
						double[] nodesDistortionSelected = new double[nodesNumber];
						for (int i = 0; i < nodesNumber; i++) {
							if ((selectedRegionNumber - 1) * nodesNumber + i >= nodesDistortionValues.length) {
								continue Loop;
							}
							nodesDistortionSelected[i] = nodesDistortionValues[(selectedRegionNumber - 1)
									* nodesNumber + i];
						}
						ArrayList<SpectralMethodRegionSelector> regions = getRegion(
								nodesDistortionValues, selectedRegionNumber,
								threshold);
						SpectralMethodRegionSelector regionsGraph1 = regions
								.get(0);
						SpectralMethodRegionSelector regionsGraph2 = regions
								.get(1);
						HashMap<Integer, String[]> graph2Results = regionsGraph2
								.getRegions(REGION_SELECTOR, MAX_NODES,
										bfsSelection);
						// Select from graph1 same nodes as graph2 but with
						// their new edges
						// in graph1.
						regionsGraph1
								.getMapping(graph2Results,
										regionsGraph1.getGraph(),
										regionsGraph2.getGraph(),
										regionsGraph2.getNodeMapping(),
										REGION_SELECTOR);
						double[] currentEvaluationMeasures = regionsGraph1
								.getEvaluationMeasures();
						if (currentEvaluationMeasures == null) {
							System.out.println("Null evaluation measure " + k);
							continue Loop;
						}
						for (int i = 0; i < evaluationMeasuresSum.length; i++) {
							evaluationMeasuresSum[i] += currentEvaluationMeasures[i];
							currentEvaluationMeasuresString[i] = currentEvaluationMeasuresString[i]
									+ "\nR"
									+ selectedRegionNumber
									+ "="
									+ getString(currentEvaluationMeasures[i]);
						}
					}
					for (int i = 0; i < evaluationMeasuresSum.length; i++) {
						// If the current evaluation measures are better, store
						// them.
						if (evaluationMeasuresSum[i] >= maxEvaluationMeasures[i]) {
							maxEvaluationMeasures[i] = evaluationMeasuresSum[i];
							maxEvaluationMeasuresK[i] = k;
							maxEvaluationMeasuresString[i] = currentEvaluationMeasuresString[i];
							maxEvaluationMeasuresThreshold[i] = threshold;
						}
					}
				} catch (MatlabInvocationException ex) {
					// If K exceeds the number of nodes in the graph, Matlab
					// code throws an exception.
					System.out.println("Error in Matlab " + ex.getMessage());
					continue;
				}
			}
			threshold += step;
		}
		// Print the best results.
		for (int i = 0; i < maxEvaluationMeasuresString.length; i++) {
			System.out.println("==========================="
					+ maxEvaluationMeasuresK[i] + ","
					+ maxEvaluationMeasuresThreshold[i]
					+ "======================");
			System.out.println(maxEvaluationMeasuresString[i]);
			System.out
					.println("==================================================");
		}
		reader1.close();
		reader2.close();
	}

	/**
	 * Run spectral method using different values of k and report the one with
	 * maximum evaluation measures while changing the threshold.
	 * 
	 * @param inputFile1
	 *            graph1 input file.
	 * @param inputFile2
	 *            graph2 input file.
	 * @param runsNumber
	 *            number of runs to choose the best threshold.
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws MatlabInvocationException
	 * @throws MatlabConnectionException
	 */
	public void runEvaluationsWithRespectToRegionSizeWithThresholdingExhaustiveSearch(
			String inputFile1, String inputFile2, double step, String method)
			throws IOException, URISyntaxException, MatlabInvocationException,
			MatlabConnectionException {
		double[] maxEvaluationMeasures = new double[6]; // Six evaluation
														// measures.
		String[] maxEvaluationMeasuresString = new String[6]; // Evaluation
																// measures
																// string
																// format.
		int[] maxEvaluationMeasuresK = new int[6]; // Value of k at the maximum
													// evaluation measures.
		double[] maxEvaluationMeasuresThreshold = new double[6]; // Value of
																	// threshold
																	// at the
																	// maximum
																	// evaluation
																	// measures.
		BufferedReader reader1 = new BufferedReader(new FileReader(inputFile1));
		BufferedReader reader2 = new BufferedReader(new FileReader(inputFile2));
		runMatlabCode();
		double threshold = 0;
		double maxThreshold = 1;
		graph1 = loadGraph(reader1.readLine());
		graph2 = loadGraph(reader2.readLine());
		SpectralMethodRegionSelector regionsGraph1Overall = null;
		SpectralMethodRegionSelector regionsGraph2Overall = null;
		while (threshold < maxThreshold) {
			System.out.println(threshold);
			Loop: for (int k = 12; k <= Math.min(400, nodesNumber); k += 10) { // Loop
																				// over
																				// k
																				// values
																				// to
				// choose the best one.
				System.out.println(k);
				try {
					double[] evaluationMeasuresSum = new double[6];
					String[] currentEvaluationMeasuresString = new String[6];
					for (int i = 0; i < currentEvaluationMeasuresString.length; i++) {
						currentEvaluationMeasuresString[i] = "";
					}
					String[] matlabParameters = new String[5];
					matlabParameters[2] = k + "";
					matlabParameters[3] = method;
					matlabParameters[4] = 1 + "";
					runSpectralMethodForThresholding(matlabParameters,
							threshold);
					double[] nodesDistortionValues = (double[]) proxy
							.getVariable("nodes_values");
					if (nodesDistortionValues.length == 0) {
						System.out.println("Returned distortions are empty! "
								+ k);
						continue;
					}
					HashMap<Double, String[]> graph2Results = new HashMap<Double, String[]>();
					HashMap<Double, Double> graph2ResultsMapping = new HashMap<Double, Double>();
					Double regionID = 1.0;
					for (int selectedRegionNumber = 1; selectedRegionNumber <= 10; selectedRegionNumber++) {
						double[] nodesDistortionSelected = new double[nodesNumber];
						for (int i = 0; i < nodesNumber; i++) {
							if ((selectedRegionNumber - 1) * nodesNumber + i >= nodesDistortionValues.length) {
								continue Loop;
							}
							nodesDistortionSelected[i] = nodesDistortionValues[(selectedRegionNumber - 1)
									* nodesNumber + i];
						}
						ArrayList<SpectralMethodRegionSelector> regions = getRegion(
								nodesDistortionValues, selectedRegionNumber,
								threshold);
						SpectralMethodRegionSelector regionsGraph1 = regions
								.get(0);
						SpectralMethodRegionSelector regionsGraph2 = regions
								.get(1);
						if (threshold == 0) {
							regionsGraph1Overall = regionsGraph1;
							regionsGraph2Overall = regionsGraph2;
						}
						HashMap<String, String[]> graph2ResultsTmp = regionsGraph2
								.getRegionsExhastiveSearch(REGION_SELECTOR,
										MAX_NODES, regionsGraph1.getGraph(),
										regionsGraph2.getGraph(),
										regionsGraph1.getNodeMapping(),
										regionsGraph2.getNodeMapping());
						for (String region : graph2ResultsTmp.keySet()) {
							String[] regionSplit = region.split(" ");
							graph2ResultsMapping.put(regionID,
									Double.parseDouble(regionSplit[1]));
							graph2Results.put(regionID++,
									graph2ResultsTmp.get(region));
						}
					}
					// Select from graph1 same nodes as graph2 but with their
					// new edges
					// in graph1.
					regionsGraph1Overall.getMappingExhastiveSearch(
							graph2Results, regionsGraph1Overall.getGraph(),
							regionsGraph2Overall.getGraph(),
							regionsGraph2Overall.getNodeMapping(),
							regionsGraph1Overall.getNodeMapping(),
							REGION_SELECTOR, graph2ResultsMapping);
					ArrayList<Double[]> evaluationMeasures = regionsGraph1Overall
							.getRegionMeasures();
					int selectedRegionNumber = 1;
					for (Double[] currentEvaluationMeasures : evaluationMeasures) {
						if (currentEvaluationMeasures == null) {
							System.out.println("Null evaluation measure " + k);
							continue Loop;
						}
						for (int i = 0; i < evaluationMeasuresSum.length; i++) {
							evaluationMeasuresSum[i] += currentEvaluationMeasures[i];
							currentEvaluationMeasuresString[i] = currentEvaluationMeasuresString[i]
									+ "\nR"
									+ selectedRegionNumber
									+ "="
									+ getString(currentEvaluationMeasures[i]);
						}
						selectedRegionNumber++;
					}
					for (int i = 0; i < evaluationMeasuresSum.length; i++) {
						// If the current evaluation measures are better, store
						// them.
						if (evaluationMeasuresSum[i] > maxEvaluationMeasures[i]) {
							maxEvaluationMeasures[i] = evaluationMeasuresSum[i];
							maxEvaluationMeasuresK[i] = k;
							maxEvaluationMeasuresString[i] = currentEvaluationMeasuresString[i];
							maxEvaluationMeasuresThreshold[i] = threshold;
						}
					}
				} catch (MatlabInvocationException ex) {
					// If K exceeds the number of nodes in the graph, Matlab
					// code throws an exception.
					System.out.println("Error in Matlab " + ex.getMessage());
					continue;
				}
			}
			threshold += step;
		}
		// Print the best results.
		for (int i = 0; i < maxEvaluationMeasuresString.length; i++) {
			System.out.println("==========================="
					+ maxEvaluationMeasuresK[i] + ","
					+ maxEvaluationMeasuresThreshold[i]
					+ "======================");
			System.out.println(maxEvaluationMeasuresString[i]);
			System.out
					.println("==================================================");
		}
		reader1.close();
		reader2.close();
	}

	/**
	 * Convert the double value into a String.
	 * 
	 * @param value
	 *            to convert to string.
	 * @return String representation of the value.
	 */
	public String getString(double value) {
		DecimalFormat df = new DecimalFormat("#.###");
		String changeValueString = df.format(value);
		return changeValueString;
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response).
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
	}

	/**
	 * 
	 * @param args
	 *            argument sent to the program.
	 * @throws MatlabInvocationException
	 * @throws MatlabConnectionException
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public static void main(String[] args) throws MatlabInvocationException,
			MatlabConnectionException, URISyntaxException, IOException {
		if (args.length < 5) {
			System.out
					.println("Java -jar spectralMethod.jar graph1File graph2File energyFunction regionsNumber nodesNumPerRegion MatlabPath");
			return;
		}
		// Scanner scanner = new Scanner(System.in);
		// Input file for graph1.
		String inputFile1 = args[0];
		// Input file for graph2.
		String inputFile2 = args[1];
		// Number of runs to choose the threshold.
		double step = 0.1;
		String method = args[2];
		GraphServlet.REGION_NUM = Integer.parseInt(args[3]);
		GraphServlet.MAX_NODES = Integer.parseInt(args[4]);
		// GraphServlet.BIASEDK = Integer.parseInt(args[5]);
		GraphServlet servlet = new GraphServlet(args[5]);
		System.out.println("Spectral Method with Priority Queue");
		servlet.runEvaluationsWithRespectToRegionSize(inputFile1, inputFile2,
				method);
		System.out.println("Run with Thresholding");
		servlet.runEvaluationsWithRespectToRegionSizeWithThresholdingAllBFSs(
				inputFile1, inputFile2, step, method);
		// scanner.close();
	}

}
