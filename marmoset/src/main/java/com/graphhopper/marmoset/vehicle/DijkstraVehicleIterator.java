package com.graphhopper.marmoset.vehicle;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.util.EdgeIteratorState;

import java.util.List;

/**
 * Created by alexander on 09/03/2016.
 */
public class DijkstraVehicleIterator extends BaseVehicleIterator {

    protected List<EdgeIteratorState> edges;
    protected int index;

    protected DijkstraVehicleIterator() {}

    public DijkstraVehicleIterator(List<EdgeIteratorState> edges, FlagEncoder encoder)
    {
        super(encoder);
        // starts at 0 to skip first edge, as the first edge is virtual (i.e not in graph)
        index = 0;
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
        if (index >= edges.size() - 1) // to skip last virtual edge
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
