Open command line and Go to "jars" folder.
1- Top changing vertices and its variations:
	Top Changing Vertices + BFS:
		For each vertex, calculate the following change measure:
		change(v)= \sum_(u) abs(G1(u,v)-G2(u,v))
		Where G1 is the adjacency matrix of graph 1 and G2 is the adjacency matrix of graph 2. 
		Then sort the vertices according to their change measure from the highest to the lowest and then pick the top r changing vertices and for each one of these top vertex perform BFS from this vertex until you traverse x vertices. Finally, each top vertex and its BFS reaching vertices will be the regions returned by this approach.
	Top Changing Vertices + Biased BFS:
		Same as the previous approach, but instead of doing traditional BFS from each vertex, we do a biased BFS as follows:
		Instead of adding all the neighbors of node n to the BFS queue, node n sorts its neighbors according to their change measure from the highest to the lowest and only add the top k unvisited neighbors to the BFS queue (k was set to 5 in all of our experiments). 
	Top Changing Vertices + BFS using priority queue:
		Same as the first approach, but instead of using a traditional queue, we use a priority queue, where the priority of the node is equal to its change value. The higher this change value, the higher the priority of the node. 

	To run top changing vertices and its variations, type the following:
		Java -jar TopChangingVerticesCalculator.jar graph1File graph2File regionsNumber nodesNumPerRegion baisedk
		Where:
			- graph1File is the file containing graph1 in the standard format, where each line represents a node in the following format:
			node_id, node_value, [node_neighbor_1:edge_weight, node_neighbor_2:edge_weight,..]
			- graph2File is the file containing graph2 in the standard format, where each line represents a node in the following format:
			node_id, node_value, [node_neighbor_1:edge_weight, node_neighbor_2:edge_weight,..]
			- regionsNumber is the number of regions to detect
			- nodesNumPerRegion is the number of nodes per region
			- baisedk is a parameter used in Top Changing Vertices + Biased BFS		
	Output:
		For each one of the previously mentioned three variations, the jar output six measures, which are:
			- Change relative to edges within Graph1 Regions  
				= (\sum_(v in Region) change(v) /(max(1,number of edges in R in graph 1))
			- Change relative to edges Within Graph2 Regions
				= (\sum_(v in Region) change(v) /(max(1,number of edges in R in graph 2))
			- Change relative to min edges Graph1 and Graph2 Regions 
				= (\sum_(v in Region) change(v)/min(max(number of edges in R in graph 1,1),max( number of edges in R in graph 2,1)) 
			- Change relative to the degree of nodes in Graph 1 = 
				= (\sum_(v in Region) change(v) /(max(1,\sum_(v in Region)degreeG1(v)))
			- Change relative to the degree of nodes in Graph 2 = 
				= (\sum_(v in Region) change(v) /(max(1,\sum_(v in Region)degreeG2(v)))
			- Change relative to the min degree of nodes in Graph 1 and Graph 2 = 
				= (\sum_(v in Region) change(v) /(min(max(1,\sum_(v in Region)degreeG1(v)),max(1,\sum_(v in Region)degreeG2(v)))))
	Example:
		java -jar TopChangingVerticesCalculator.jar "data\Related Work Format\Food_graphs\Japan.txt" "data\Related Work Format\Food_graphs\North-African.txt" 10 16 5
	
2- Top changing vertices Exhaustive Search and its variations:
	Top Changing Vertices Exhaustive Search + BFS:
			For each vertex, calculate the following change measure:
			change(v)= \sum_(u \in V) abs(G1(u,v)-G2(u,v))
			Where G1 is the adjacency matrix of graph 1 and G2 is the adjacency matrix of graph 2. Then do BFS from every node, store the returned region. After that, sort the regions according to their change measure (The used measure is(\sum_(v in Region) change(v) /min(max(number of edges in R in graph 1,1),max( number of edges in R in graph 2,1))) from the highest to the lowest and then pick the top r changing regions that have size equal to the desired number of nodes per region.
	Top Changing Vertices Exhaustive Search  + Biased BFS:
		Same as the previous approach, but instead of doing traditional BFS from each vertex, we do a biased BFS as follows:
		Instead of adding all the neighbors of node n to the BFS queue, node n sorts its neighbors according to their change measure from the highest to the lowest and only add the top k unvisited neighbors to the BFS queue (k was set to 5 in all of our experiments). 
	Top Changing Vertices Exhaustive Search  + BFS using priority queue:
		Same as the first approach, but instead of using a traditional queue, we use a priority queue, where the priority of the node is equal to its change value. The higher this change value, the higher the priority of the node. 
	The same previous three variations but plus thresholding:
		Before running each one of the previous methods, we used thresholding, where we sort the vertices from the smallest distortion value to the highest distortion value and then remove the top epson% vertices from the graph and run the rest of the algorithm. We have tried to run epson from 0 to 1 with an increase step of 0.1 and report espon that yields the highest evaluation measures. 
	To run top changing vertices and its variations, type the following:
		Java -jar TopChangingVerticesExhaustiveSearchCalculator.jar graph1File graph2File regionsNumber nodesNumPerRegion baisedk
		Where:
			- graph1File is the file containing graph1 in the standard format, where each line represents a node in the following format:
			node_id, node_value, [node_neighbor_1:edge_weight, node_neighbor_2:edge_weight,..]
			- graph2File is the file containing graph2 in the standard format, where each line represents a node in the following format:
			node_id, node_value, [node_neighbor_1:edge_weight, node_neighbor_2:edge_weight,..]
			- regionsNumber is the number of regions to detect
			- nodesNumPerRegion is the number of nodes per region
			- baisedk is a parameter used in Top Changing Vertices + Biased BFS		
	Output:
		For each one of the previously mentioned three variations, the jar output six measures, which are:
			- Change relative to edges within Graph1 Regions  
				= (\sum_(v in Region) change(v) /(max(1,number of edges in R in graph 1))
			- Change relative to edges Within Graph2 Regions
				= (\sum_(v in Region) change(v) /(max(1,number of edges in R in graph 2))
			- Change relative to min edges Graph1 and Graph2 Regions 
				= (\sum_(v in Region) change(v)/min(max(number of edges in R in graph 1,1),max( number of edges in R in graph 2,1)) 
			- Change relative to the degree of nodes in Graph 1 = 
				= (\sum_(v in Region) change(v) /(max(1,\sum_(v in Region)degreeG1(v)))
			- Change relative to the degree of nodes in Graph 2 = 
				= (\sum_(v in Region) change(v) /(max(1,\sum_(v in Region)degreeG2(v)))
			- Change relative to the min degree of nodes in Graph 1 and Graph 2 = 
				= (\sum_(v in Region) change(v) /(min(max(1,\sum_(v in Region)degreeG1(v)),max(1,\sum_(v in Region)degreeG2(v)))))
	Example:
		java -jar TopChangingVerticesExhaustiveSearchCalculator.jar "data\Related Work Format\Food_graphs\Japan.txt" "data\Related Work Format\Food_graphs\North-African.txt" 10 16 5

3- Max Changing Radius + its variations:
	- Max Changing Radius:
		Each vertex performs a BFS until reaching a radius a of the BFS graph. Then the approach sorts these regions according to the sum of the vertices change in them from the highest to the lowest and report the top r regions. 
	- Max Changing Radius While accounting for Region Size:
		Each vertex performs a BFS until reaching a radius a of the BFS graph such that radius a is the smallest radius that return a region with at least number of vertices equal to x. Then the approach sorts these regions according to (\sum_(v in Region) change(v) /min(max(number of edges in R in graph 1,1),max( number of edges in R in graph 2,1)) from the highest to the lowest and report the top r regions. 
	- Max Changing Radius Methods + Thresholding:
		Before running each one of the previous methods, we used thresholding, where we sort the vertices from the smallest distortion value to the highest distortion value and then remove the top epson% vertices from the graph and run the rest of the algorithm. We have tried to run epson from 0 to 1 with an increase step of 0.1 and report espon that yields the highest evaluation measures.
		
	To run max changing raduis and its variations, type the following:
		Java -jar MaxChangingRaduis.jar graph1File graph2File regionsNumber nodesNumPerRegion baisedk
		Where:
			- graph1File is the file containing graph1 in the standard format, where each line represents a node in the following format:
			node_id, node_value, [node_neighbor_1:edge_weight, node_neighbor_2:edge_weight,..]
			- graph2File is the file containing graph2 in the standard format, where each line represents a node in the following format:
			node_id, node_value, [node_neighbor_1:edge_weight, node_neighbor_2:edge_weight,..]
			- regionsNumber is the number of regions to detect
			- nodesNumPerRegion is the number of nodes per region
			- baisedk is a parameter used in Top Changing Vertices + Biased BFS		
	Output:
		For each one of the previously mentioned three variations, the jar output six measures, which are:
			- Change relative to edges within Graph1 Regions  
				= (\sum_(v in Region) change(v) /(max(1,number of edges in R in graph 1))
			- Change relative to edges Within Graph2 Regions
				= (\sum_(v in Region) change(v) /(max(1,number of edges in R in graph 2))
			- Change relative to min edges Graph1 and Graph2 Regions 
				= (\sum_(v in Region) change(v)/min(max(number of edges in R in graph 1,1),max( number of edges in R in graph 2,1)) 
			- Change relative to the degree of nodes in Graph 1 = 
				= (\sum_(v in Region) change(v) /(max(1,\sum_(v in Region)degreeG1(v)))
			- Change relative to the degree of nodes in Graph 2 = 
				= (\sum_(v in Region) change(v) /(max(1,\sum_(v in Region)degreeG2(v)))
			- Change relative to the min degree of nodes in Graph 1 and Graph 2 = 
				= (\sum_(v in Region) change(v) /(min(max(1,\sum_(v in Region)degreeG1(v)),max(1,\sum_(v in Region)degreeG2(v)))))
	Example:
		java -jar MaxChangingRaduis.jar "data\Related Work Format\Food_graphs\Japan.txt" "data\Related Work Format\Food_graphs\North-African.txt" 10 16 5

4- Spectral Method: (MATLAB installation is needed)
	For the spectral method, we have compared the following variations to get the highly distorted regions:
	- Spectral method + BFS:
		Use spectral method to compute the distortion values for each vertex, then sort the vertices based on their distortion values from the highest to the lowest and then pick the top r changing vertices and for each one of these top vertex perform BFS from this vertex until you traverse x vertices. Finally, each top vertex and its BFS reaching vertices will be the regions returned by this approach.
	- Spectral method + Biased BFS:
		Same as the previous approach, but instead of doing traditional BFS from each vertex, we do a biased BFS as follows:
		Instead of adding all the neighbors of node n to the BFS queue, node n sorts its neighbors according to their distortion measure from the highest to the lowest and only add the top k unvisited neighbors to the BFS queue (k was set to 5 in all of our experiments).
	- Spectral method + BFS with priority queue:
		Same as the spectral method with BFS, but instead of using a traditional queue, we use a priority queue, where the priority of the node is equal to its distortion value. The higher this distortion value, the higher the priority of the node. 
	- Thresholding:
		Before running each one of the previous methods, we used thresholding, where we sort the vertices from the smallest distortion value to the highest distortion value and then remove the top epson% vertices from the graph and run the rest of the algorithm. We have tried to run epson from 0 to 1 with an increase step of 0.1 and report espon that yields the highest evaluation measures. 	
	
	To run spectral method and its variations, type the following:
		Java -jar spectralMethod.jar graph1File graph2File energyFunction regionsNumber nodesNumPerRegion MATLABFile
		Where:
			- graph1File is the file containing graph1 in the standard format, where edges are separated by '-' and each edge contains the id of the source node, id of the target node and weight of the edge separated by comma, for example:
				1,2,3-2,1,3
			In the previous example, we have only two nodes 1 and 2 and an edge between them with weight 3
			- graph2File is the file containing graph2 in the standard format, where edges are separated by '-' and each edge contains the id of the source node, id of the target node and weight of the edge separated by comma, for example:
				1,2,3-2,1,3
			In the previous example, we have only two nodes 1 and 2 and an edge between them with weight 3
			- energyFunction is the energy function to use for the spectral method, currently we support the following energy functions:
				- area-based energy function
				- conformal-based energy function
				- E1, which is calculated as follows:
					(\sum_((u,v) in E) (f(u) + f(v))^2 * (w1(u,v) - w2(u,v))^2) / (\sum_(u) f(u)^2 * d1(u))
					Where w1(u,v) is the weight of the edge between u and v in graph1 and w2(u,v) is the weight of the edge between u and v in graph2. Additionally, d1(u) is the degree of vertex u in graph 1. 
				- E2, which is calculated as follows:
					(\sum_((u,v) in E) (f(u) - f(v))^2 * (w1(u,v) - w2(u,v))^2)
					Where w1(u,v) is the weight of the edge between u and v in graph1 and w2(u,v) is the weight of the edge between u and v in graph2.
				- E3, which is calculated as follows:
					\sum_u f(u)^2 (d1(u) - d2(u))^2
					Where d1(u) is the degree of vertex u in graph 1 and d2(u) is the degree of vertex u in graph 2. 
				- E4, which is calculated as follows:
					(\sum_u f(u)^2 (d1(u) - d2(u))^2) / (\sum_(u) f(u)^2 * d1(u))
					Where d1(u) is the degree of vertex u in graph 1 and d2(u) is the degree of vertex u in graph 2. 
			- regionsNumber is the number of regions to detect
			- nodesNumPerRegion is the number of nodes per region
			- MATLABFile is the path to the matlab file "visualize_map.m", which is stored in "src\server\matlab"
	
	Example:
		Java -jar spectralMethod.jar "data\Spectral Method Format\graph_1_japan.txt" "data\Spectral Method Format\graph_2_north_africa.txt" "conformal-based" 10 16 "C:\Users\User\Documents\Dynamic Graphs\DynamicGraphVisualizationTool-master\DynamicGraphVisualizationTool-master\src\server\matlab"

5- Spectral Method from every vertex: (MATLAB installation is needed)
		Start from every vertex using the colors provided by the first singular vector and get the top changing 10 regions using it (the ones that have the highest distortion values, which is calculated as = (sum_u \delta(u) / 	(min(region_size_in_graph1, region_size_in_graph2))) and then start from every vertex using the colors provided by the second singular vector and get the top changing 10 regions. Finally, end up with 100 regions (10 from each one of the singular vector), then sort them and choose the top 10 changing regions as the finally selected ones. BFS with priority queue is used in this setting.
		
		To run spectral method, type the following:
			Java -jar spectralMethodWithThresholdingExhaustiveSearch.jar graph1File graph2File energyFunction regionsNumber nodesNumPerRegion MatlabFile
			Where:
				- graph1File is the file containing graph1 in the standard format, where edges are separated by '-' and each edge contains the id of the source node, id of the target node and weight of the edge separated by comma, for example:
				1,2,3-2,1,3
				In the previous example, we have only two nodes 1 and 2 and an edge between them with weight 3
				- graph2File is the file containing graph2 in the standard format, where edges are separated by '-' and each edge contains the id of the source node, id of the target node and weight of the edge separated by comma, for example:
				1,2,3-2,1,3
				In the previous example, we have only two nodes 1 and 2 and an edge between them with weight 3
				- energyFunction is the energy function to use for the spectral method, currently we support the following energy functions:
					- area-based energy function
					- conformal-based energy function
					- E1, which is calculated as follows:
						(\sum_((u,v) in E) (f(u) + f(v))^2 * (w1(u,v) - w2(u,v))^2) / (\sum_(u) f(u)^2 * d1(u))
						Where w1(u,v) is the weight of the edge between u and v in graph1 and w2(u,v) is the weight of the edge between u and v in graph2. Additionally, d1(u) is the degree of vertex u in graph 1. 
					- E2, which is calculated as follows:
						(\sum_((u,v) in E) (f(u) - f(v))^2 * (w1(u,v) - w2(u,v))^2)
						Where w1(u,v) is the weight of the edge between u and v in graph1 and w2(u,v) is the weight of the edge between u and v in graph2.
					- E3, which is calculated as follows:
						\sum_u f(u)^2 (d1(u) - d2(u))^2
						Where d1(u) is the degree of vertex u in graph 1 and d2(u) is the degree of vertex u in graph 2. 
					- E4, which is calculated as follows:
						(\sum_u f(u)^2 (d1(u) - d2(u))^2) / (\sum_(u) f(u)^2 * d1(u))
						Where d1(u) is the degree of vertex u in graph 1 and d2(u) is the degree of vertex u in graph 2. 
				- regionsNumber is the number of regions to detect
				- nodesNumPerRegion is the number of nodes per region
				- MATLABFile is the path to the matlab file "visualize_map.m", which is stored in "src\server\matlab"
		
		Example:
			Java -jar spectralMethodWithThresholdingExhaustiveSearch.jar "data\Spectral Method Format\graph_1_japan.txt" "data\Spectral Method Format\graph_2_north_africa.txt" "conformal-based" 10 16 "C:\Users\User\Documents\Dynamic Graphs\DynamicGraphVisualizationTool-master\DynamicGraphVisualizationTool-master\src\server\matlab"