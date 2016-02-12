clear; clc; close all;
rng(1);
k = 56;
r = 1;
measure_method = 'area-based';
file_graph_1 = 'graph_1_56.csv';
file_graph_2 = 'graph_2_56.csv';
output_file = 'graph_weights.csv';
visualize_map(file_graph_1, file_graph_2, k, r, measure_method, output_file);
