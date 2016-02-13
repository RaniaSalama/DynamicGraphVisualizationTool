function colors_nodes = visualize_map(file_graph_1, file_graph_2, k, r, measure_method)
G1 = load(file_graph_1);
G2 = load(file_graph_2);

% get number of nodes as the max node id
nv = max(max(G1(:,1))); 
nv = max(max(nv, G1(:,2)));
nv = max(max(nv, G2(:,1)));
nv = max(max(nv, G2(:,2)));
% convert graph adjacent list to sparse adjacent matrix 
M = sparse(G1(:,1), G1(:,2), 1, nv, nv);
N = sparse(G2(:,1), G2(:,2), 1, nv, nv);
% make the matricies symmetric, working on undirected graphs ...
M = M + M';
N = N + N';

% Calculate F and G
laplacian_method_1 = 'standard';
laplacian_method_2 = 'standard';
F = spdiags(sum(M,2),0, nv, nv);
G = spdiags(sum(N,2),0, nv, nv);

L1 = get_laplacian(M, laplacian_method_1);
[e1, v1] = eigs(L1, F, k, -1e-7);
[v1, order] = sort(diag(v1),'ascend');
e1 = e1(:,order);

L2 = get_laplacian(N, laplacian_method_2);

S = zeros(k, k);
V = zeros(nv, k);
if strcmp(measure_method, 'area-based') == 1
    [V, S] = eig(e1'*G*e1);
    [S,order] = sort(diag(S),'descend');
    V = V(:,order);
elseif strcmp(measure_method, 'conformal-based') == 1
    myend = size(v1,1);
    E = diag(v1(2:myend));
    R = e1'*L2*e1;
    R = R(2:myend, 2:myend);
    [V, S] = eig(R, E);
    [S, order] = sort(diag(S), 'descend');
    V = V(:, order);
    V = [0;V(:,1:r)];
    k = 1;
end

wh = e1*V(:,1:r);
wl = e1*V(:,k-r+1:k);

sh = S(1:r);
sl = S(k-r+1:k);

distortion_values = wh.^2;
colors_nodes = repmat('#000000',[nv 1]);
colors = jet(100);
for k=1:nv
    pos = (distortion_values(k)-min(distortion_values))/(max(distortion_values)-min(distortion_values));
    pos = ceil(pos*100);
    if(pos>100 || isnan(pos))
        pos = 100;
    elseif(pos<=0)
        pos = 1;
    end
    colors_nodes(k,:) = rgb2hex(colors(pos,:));
end
