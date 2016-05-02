addpath('GCMex');
addpath('matlab_bgl');
G1 = load('Synthetic_0.txt')+1;
G2 = load('Synthetic_1.txt')+1;
G1(:,3) = 1;
G2(:,3) = 1;
k = 56;
r = 2;
measure_method = 'conformal-based';
[colors_nodes, distortion_values] = visualize_map(G1, G2, k, r, measure_method);
nv = max(max(max(G1(:,1)), max(G1(:,2))), max(max(G2(:,1)), max(G2(:,2))));
M = sparse(G1(:,1), G1(:,2), G1(:,3), nv, nv);
N = sparse(G2(:,1), G2(:,2), G2(:,3), nv, nv);
class = ones(1, nv);
label_cost = ones(r,r);
unary = distortion_values(:,1)';
for i=1:size(unary,1)
    unary(i,:) = (unary(i,:) - min(unary(i,:)))/ (max(unary(i,:)) - min(unary(i,:)));
end
unary(2,:) = 1 - unary(1,:);
[final_labels, energy, enery_after] = GCMex(class, single(unary), M, single(label_cost));

A1 = M;
A2 = N;
D1 = zeros(size(A1));
D1(A1 == 1 & A2==0) = 1;
D2 = zeros(size(A1));
D2(A1 == 0 & A2==1) = 1;
subplot(1,3,2);
X = plot_graph_wbgl(A1);
plot_graph_wcoordinates(D1,X,[0 1 0]); hold on;
plot_graph_wcoordinates(D2,X,[0.5 0 0]); hold on;

plot_function_2d(X, final_labels);
hold off;

