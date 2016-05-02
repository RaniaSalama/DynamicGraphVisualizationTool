function X = plot_graph_wbgl(A)
    options.iterations = 200;
    
%    X =  fruchterman_reingold_force_directed_layout(A,options);
    X =  kamada_kawai_spring_layout(A,options);
    plot(X(:,1),X(:,2),'o','MarkerFaceColor','b','MarkerSize',1);
    [s1, s2] = find(A>0);
    startx = X(s1,1)';
    starty = X(s1,2)';
    endx = X(s2,1)';
    endy = X(s2,2)';
    
    hold on;
%    Colors = repmat([0.81 0.81 0.81],length(startx),1);
    Colors = repmat([1 0.81 0],length(startx),1);
    set(gca, 'ColorOrder', Colors);
    hold on;

    plot([startx; endx],[starty; endy],'.-','LineWidth',2); hold on;    
    plot(X(:,1),X(:,2),'o','MarkerFaceColor','b');
end