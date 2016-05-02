function X = plot_graph_wcoordinates(A,X,color)       
    [s1, s2] = find(A>0);
    startx = X(s1,1)';
    starty = X(s1,2)';
    endx = X(s2,1)';
    endy = X(s2,2)';
    
    if(length(s1)==0)
        return;
    end
    
    hold on;
    Colors = repmat(color,length(startx),1);
    set(gca, 'ColorOrder', Colors);
    hold on;

    plot([startx; endx],[starty; endy],'.-','LineWidth',0.1); hold on;    
end