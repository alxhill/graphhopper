package com.graphhopper.marmoset.vehicle;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.util.EdgeIteratorState;

import java.util.List;

/**
 * Created by alexander on 20/04/2016.
 */
public class SelfDrivingVehicleIterator extends DijkstraVehicleIterator {
    public SelfDrivingVehicleIterator(List<EdgeIteratorState> edges, FlagEncoder encoder)
    {
        super(edges, encoder);
    }

    public void resetEdges(List<EdgeIteratorState> edgeList)
    {
        this.edges = edgeList;
    }

}
