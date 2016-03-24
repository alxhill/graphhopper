package com.graphhopper.marmoset.vehicle;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.util.EdgeIteratorState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by alexander on 09/03/2016.
 */
public class DijkstraVehicleIterator extends BaseVehicleIterator {

    protected List<EdgeIteratorState> edges;
    protected int index;

    private static Logger logger = LoggerFactory.getLogger(DijkstraVehicleIterator.class);

    protected DijkstraVehicleIterator() {}

    public DijkstraVehicleIterator(List<EdgeIteratorState> edges, FlagEncoder encoder)
    {
        super(encoder);
        // starts at 0 to skip first edge, as the first edge is virtual (i.e not in graph)
        index = 0;
        // removes last virtual edge
        edges.remove(edges.size() - 1);
        this.edges = edges;
    }

    @Override
    public boolean hasNext()
    {
        return index < edges.size() - 1;
    }

    @Override
    public boolean next()
    {
        index++;
        if (index >= edges.size())
            return false;
        edge = edges.get(index);
        return true;
    }

    @Override
    public DijkstraVehicleIterator duplicate()
    {
        DijkstraVehicleIterator dvi = new DijkstraVehicleIterator();
        dvi.encoder = encoder;
        dvi.edge = edge;
        dvi.edges = edges;
        dvi.index = index;
        return dvi;
    }
}
