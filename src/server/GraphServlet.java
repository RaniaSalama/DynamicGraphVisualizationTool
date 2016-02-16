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
 * Servlet implementation class GraphServlet
 */
public class GraphServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	// whether the request parameter is file or not.
	private static final boolean[] isFile = new boolean[] { true, true, false,
			false, false };
	// basepath for storing the two graphs
	private static String basePath = "";
	// MATLAB fiel to run
	private static final String MATLAB_FILE = "server/matlab/visualize_map.m";
	// proxy used to run MATLAB code
	private static MatlabProxy proxy;

	// Number of regions to calculate
	private static final int REGION_NUM = 10;
	// Max number of nodes per region
	private static final int MAX_NODES = 30;
	// Max overlapping nodes between regions
	private static final double MAX_OVERLAP = 0.7;

	// graph1 edges values
	private static double[][] graph1;
	// graph2 edges
	private static double[][] graph2;

	// store previous parameters to re-use the results
	private static double[][] prev_graph1;
	private static double[][] prev_graph2;
	private static int prev_k;
	private static String prev_measure;
	private static String[] prev_nodes_colors;

	private HashMap<Integer, String[]> graph1Results;
	private HashMap<Integer, String[]> graph2Results;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public GraphServlet() {
		super();
		if (proxy == null) { // Initialize MATLAB proxy
			MatlabProxyFactoryOptions options = new MatlabProxyFactoryOptions.Builder()
					.setUsePreviouslyControlledSession(true).setHidden(true)
					.setMatlabLocation(null).build();
			MatlabProxyFactory factory = new MatlabProxyFactory(options);
			try {
				proxy = factory.getProxy();
			} catch (MatlabConnectionException e) {
				e.printStackTrace();
			}
		}
	}

	// given graph edges string format, load them into graphs matrix
	public static void loadGraph(String data, int type) {
		// '-' is the delimiter used when sending graph edges
		String[] edges = data.split("-");
		if (type == 1)
			graph1 = new double[edges.length][2];
		else
			graph2 = new double[edges.length][2];
		int index = 0;
		for (String edge : edges) {
			if (edge.startsWith(","))
				edge = edge.substring(1);
			edge = edge.trim();
			if (edge.length() == 0)
				continue;
			String[] nodes = edge.split(",");
			if (type == 1) {
				graph1[index][0] = Integer.parseInt(nodes[0]);
				graph1[index][1] = Integer.parseInt(nodes[1]);
			} else {
				graph2[index][0] = Integer.parseInt(nodes[0]);
				graph2[index][1] = Integer.parseInt(nodes[1]);
			}
			index++;
		}

	}

	/**
	 * Compare whether the two graphs are similar or not
	 * @param graph1 graph1 edges
	 * @param graph2 graph2 edges
	 * @return true if the graphs are the same, otherwise return false
	 */
	public boolean compareGraphs(double[][] graph1, double[][] graph2) {
		if (graph1 == null || graph2 == null)
			return false;
		for (int i = 0; i < graph1.length; i++) {
			if ((graph1[i][0] != graph2[i][0])
					|| (graph1[i][1] != graph2[i][1]))
				return false;
		}
		return true;
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		if (basePath.length() == 0) { // if we didn't set the basepath yet
			try { // set the basepath to the same as MATLAB code path
				String[] matlabFields = MATLAB_FILE.split("/");
				String matlabFilename = matlabFields[2];
				basePath = new File(GraphServlet.class.getClassLoader()
						.getResource(MATLAB_FILE).toURI()).getAbsolutePath();
				basePath = basePath.substring(0,
						basePath.indexOf(matlabFilename));
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}
		PrintWriter out = response.getWriter();
		String parameters = "";
		Enumeration<String> keys = request.getParameterNames();
		int index = 0;
		boolean isFirstFile = true; // is first graph
		String[] matlabParameters = new String[5];
		// loop over each send parameter and add them to matlab parameter array
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			parameters = request.getParameter(key); // get parameters in the
													// request
			if (isFile[index]) {
				if (isFirstFile) { // first graph
					isFirstFile = false;
					loadGraph(parameters, 1); // write data to graph1
												// file
				} else { // second graph
					loadGraph(parameters, 2); // write data to graph2
												// file
				}
			} else {
				matlabParameters[index] = parameters;
			}
			index++;
		}
		try {
			if (proxy == null) { // initialize the MATLAB proxy
				MatlabProxyFactoryOptions options = new MatlabProxyFactoryOptions.Builder()
						.setUsePreviouslyControlledSession(true)
						.setHidden(true).setMatlabLocation(null).build();
				MatlabProxyFactory factory = new MatlabProxyFactory(options);
				proxy = factory.getProxy();
			}
			if (compareGraphs(graph1, prev_graph1)
					&& compareGraphs(graph2, prev_graph2)
					&& prev_k == Integer.parseInt(matlabParameters[2])
					&& prev_measure.equalsIgnoreCase(matlabParameters[3])) {
				// if same graph with same parameters but different regions
				// re-use previous results
				int regionSelector = Integer.parseInt(matlabParameters[4]);
				String[] graph1ResultsRegion = graph1Results
						.get(regionSelector);
				String[] graph2ResultsRegion = graph2Results
						.get(regionSelector);
				
				for (int i = 0; i < prev_nodes_colors.length; i++) {
					out.print(prev_nodes_colors[i] + ","); // get the nodes colors
				}
				// Write selected regions to the response
				out.print("_");
				for (int i = 0; i < graph1ResultsRegion.length; i++) {
					out.print(graph1ResultsRegion[i] + "-"); // get the nodes
																// colors
				}
				out.print("_");
				for (int i = 0; i < graph2ResultsRegion.length; i++) {
					out.print(graph2ResultsRegion[i] + "-"); // get the nodes
																// colors
				}
				return;
			}
			// add code path to the MATLAB environment
			proxy.eval("addpath('" + basePath + "')");
			// store the graphs in the MATLAB format
			MatlabTypeConverter processor = new MatlabTypeConverter(proxy);
			processor.setNumericArray("G1",
					new MatlabNumericArray(graph1, null));
			processor.setNumericArray("G2",
					new MatlabNumericArray(graph2, null));
			// run the visualize_map code
			proxy.eval("[nodes_colors, nodes_values] = visualize_map(G1,G2,"
					+ Integer.parseInt(matlabParameters[2]) + ",1,'"
					+ matlabParameters[3] + "');");

			String[] nodes_colors = (String[]) proxy
					.getVariable("nodes_colors");
			for (int i = 0; i < nodes_colors.length; i++) {
				out.print(nodes_colors[i] + ","); // get the nodes colors
			}
			double[] nodes_values = (double[]) proxy
					.getVariable("nodes_values");
			// Load graph2
			RegionSelector regionsGraph2 = new RegionSelector(nodes_values,
					graph2);
			// Select top-region_num from graph2 results
			graph2Results = regionsGraph2.getRegions(REGION_NUM, MAX_NODES,
					MAX_OVERLAP);

			// Load graph1
			RegionSelector regionsGraph1 = new RegionSelector(nodes_values,
					graph1);
			// Select from graph1 same nodes as graph2 but with their new edges
			// in graph1
			graph1Results = regionsGraph1.getMapping(graph2Results);

			// Select the region specified by the user
			int regionSelector = Integer.parseInt(matlabParameters[4]);
			String[] graph1ResultsRegion = graph1Results.get(regionSelector);
			String[] graph2ResultsRegion = graph2Results.get(regionSelector);
			// Write selected regions to the response
			out.print("_");
			for (int i = 0; i < graph1ResultsRegion.length; i++) {
				out.print(graph1ResultsRegion[i] + "-"); // get the nodes colors
			}
			out.print("_");
			for (int i = 0; i < graph2ResultsRegion.length; i++) {
				out.print(graph2ResultsRegion[i] + "-"); // get the nodes colors

			}
			// store the current results for checking next time
			prev_graph1 = graph1;
			prev_graph2 = graph2;
			prev_k = Integer.parseInt(matlabParameters[2]);
			prev_measure = matlabParameters[3];
			prev_nodes_colors = nodes_colors;
			// proxy.disconnect();
		} catch (MatlabInvocationException ex) {
			out.println(ex.getMessage());
		} catch (MatlabConnectionException ex) {
			out.println(ex.getMessage());
		}
		out.close();
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
	}

}
