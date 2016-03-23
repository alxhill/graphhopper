package com.graphhopper.marmoset.vehicle;

import com.graphhopper.routing.util.DefaultEdgeFilter;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.storage.Graph;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeIteratorState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by alexander on 23/03/2016.
 */
public class RandomVehicleIterator extends BaseVehicleIterator {

    private Graph graph;
    private EdgeExplorer edgeExplorer;

    private static Logger logger = LoggerFactory.getLogger(RandomVehicleIterator.class);

    public RandomVehicleIterator(EdgeIteratorState firstEdge, FlagEncoder encoder, Graph graph)
    {
        super(encoder);
        this.graph = graph;
        edgeExplorer = graph.createEdgeExplorer(new DefaultEdgeFilter(encoder, false, true));
        edge = firstEdge;
    }

    @Override
    public boolean hasNext()
    {
        return true;
    }

    @Override
    public boolean next()
    {
        EdgeIterator it = edgeExplorer.setBaseNode(edge.getBaseNode());
        int i = 0;
        while (it.next())
        {
            logger.info("random vehicle edge number " + i);
        }

        return false;
    }

    @Override
    public RandomVehicleIterator duplicate()
    {
        return new RandomVehicleIterator(edge, encoder, graph);
    }

}
