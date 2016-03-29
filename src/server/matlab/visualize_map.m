%{
% visualize_map computes the distortion values and nodes colors for each node in G1 and G2
% highlighting the distortion between the two graphs.
% The method parameters are:
% G1 is the first graph in the following format node1,node2,edge_value.
% G2 is the second graph in the following format node1,node2,edge_value.
% k is the smooth parameter.
% r is the number of singular values and vectors computed, which corresponds to the number of distortion areas to return.
% measure_method is the distortion measure.
% The method outputs are:
% colors_nodes is a vector nx1 where each entry is a color for each node of the graphs.
% distortion_values is a vector nx1 where each entry is a distortion value for the nodes.
% Note that, the method currently only work for undirected graphs. 
%}
function [colors_nodes, distortion_values] = visualize_map(G1, G2, k, r, measure_method)
% get number of nodes as the max node id.
rng(1); % fix randomization.
nv = max(max(max(G1(:,1)), max(G1(:,2))), max(max(G2(:,1)), max(G2(:,2))));
r = min(r, k); % in case r exceeds k 
% convert graph adjacency list to sparse matrix.
M = sparse(G1(:,1), G1(:,2), G1(:,3), nv, nv);
N = sparse(G2(:,1), G2(:,2), G2(:,3), nv, nv);
% Calculate F and G where F and G are a diagonal matrix where each entry represents the
% degree of the nodes of M and N respectively. 
F = spdiags(1./sum(M,2),0, nv, nv);
G = spdiags(sum(N,2),0, nv, nv);

L1 = spdiags(sum(M,2), 0, nv, nv) - M;

% e1 are the eigenvectors of F^-1*L1.
% v1 are the eigenvalues of F^-1*L1.
[e1, v1] = eigs(F*L1, k);
[v1, order] = sort(diag(v1),'ascend');
e1 = e1(:,order);

L2 = spdiags(sum(N,2), 0, nv, nv) - N;

S = zeros(k, k);
V = zeros(nv, k);
if strcmp(measure_method, 'area-based') == 1
    % V are the eigenvectors of e1'*G*e1.
    % S are the eigenvalues of e1'*G*e1.  
    [V, S] = eig(e1'*G*e1);
    [S, order] = sort(diag(S),'descend');
    V = V(:,order);
elseif strcmp(measure_method, 'conformal-based') == 1
    v1_size = size(v1,1);
    % E is a vector containing the diagonal values of the v1.
    E = diag(v1(2:v1_size));
    R = e1'*L2*e1;
    R = R(2:v1_size, 2:v1_size);
    % V are the eigenvectors of E^-1*R.
    % S are the eigenvalues of E^-1*R.
    [V, S] = eig(R, E);
    [S, order] = sort(diag(S), 'descend');
    V = V(:, order);
    % r number of singular vectors and values to return.
    r = min(r, size(V,2));
    V = [zeros(1,r);V(:,1:r)];
    k = 1;
end
% wh is the distortion values for each node based on using the highest singular values. 
wh = e1*V(:,1:r);
% wl is the distortion values for each node based on using the smallest singular values.
%wl = e1*V(:,k-r+1:k);
% sh is the first r eigenvalues for the distortion. 
%sh = S(1:r);
% sl is the last r eigenvalues for the distortion.
%sl = S(k-r+1:k);

distortion_values = wh.^2;

colors_nodes = cell(nv, r);

colors = jet(100);
for i=1:r
    distortion_value_i = distortion_values(:,i);
	for k=1:nv
	    pos = (distortion_value_i(k)-min(distortion_value_i))/(max(distortion_value_i)-min(distortion_value_i));
	    pos = real(ceil(pos*100));
	    if(pos>100 || isnan(pos))
	        pos = 100;
	    elseif(pos<=0)
	        pos = 1;
        end
	    colors_nodes{k,i} = rgb2hex(colors(pos,:));
	end
end