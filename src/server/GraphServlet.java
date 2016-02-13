package server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;
import matlabcontrol.MatlabProxyFactoryOptions;

/**
 * Servlet implementation class GraphServlet
 */
public class GraphServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	// whether the request parameter is file or not.
	private static final boolean[] isFile = new boolean[] { true, false, true,
			false };
	// basepath for storing the two graphs
	private static String basePath = "";
	private static String graph1File = "\\graph_1.csv";
	private static String graph2File = "\\graph_2.csv";
	// MATLAB fiel to run
	private static final String MATLAB_FILE = "server/matlab/visualize_map.m";
	// proxy used to run MATLAB code
	private static MatlabProxy proxy;

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

	// given graph edges, write the data to the desired output file
	public static void writeGraph(String data, String outputFile) {
		PrintWriter out = null;
		try { // '-' is the delimiter used when sending graph edges
			String[] edges = data.split("-");
			out = new PrintWriter(new FileWriter(outputFile));
			for (String edge : edges) {
				out.println(edge);
			}
		} catch (IOException ex) {

		} finally {
			out.close();
		}
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
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
			// modify the graphs paths to point to the basepath
			graph1File = basePath + graph1File;
			graph2File = basePath + graph2File;
		}
		PrintWriter out = response.getWriter();
		String parameters = "";
		@SuppressWarnings("unchecked")
		Enumeration<Object> keys = request.getParameterNames();
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
					writeGraph(parameters, graph1File); // write data to graph1
														// file
					matlabParameters[index] = graph1File;
				} else { // second graph
					writeGraph(parameters, graph2File); // write data to graph2
														// file
					matlabParameters[index] = graph2File;
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
			// add graphs path to the MATLAB environment
			proxy.eval("addpath('" + basePath + "')");
			Object[] args = new Object[5];
			args[0] = (Object) matlabParameters[0]; // graph 1 path
			args[1] = (Object) matlabParameters[2]; // graph 2 path
			args[2] = (Object) Integer.parseInt(matlabParameters[3]); // k
			args[3] = (Object) 1; // r fixed
			args[4] = (Object) matlabParameters[1]; // measure
			Object[] results = proxy.returningFeval("visualize_map", 1, args);
			String[] result_array = (String[]) results[0];
			for (int i = 0; i < result_array.length; i++) {
				out.print(result_array[i] + ","); // get the nodes colors
			}
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
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
	}

}
