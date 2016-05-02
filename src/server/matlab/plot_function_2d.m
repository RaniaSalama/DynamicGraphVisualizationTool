function plot_function_2d(X, gg2)
nv = size(gg2,1);
colors = jet(100);
for k=1:nv
    pos = (gg2(k)-min(gg2))/(max(gg2)-min(gg2));
    pos = real(ceil(pos*100));
    if(pos>100 || isnan(pos))
        pos = 100;
    elseif(pos<=0)
        pos = 1;
    end
    plot(X(k,1),X(k,2),'o','Color',colors(pos,:),'MarkerFaceColor',colors(pos,:),'MarkerEdgeColor',colors(pos,:));
    hold on;    
end
