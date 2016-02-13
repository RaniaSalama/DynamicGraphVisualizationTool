function [laplacian] = get_laplacian(G, method)

if strcmp(method, 'standard') == 1
    nv = size(G,1);
    laplacian = spdiags(sum(G,2), 0, nv, nv) - G;
end

