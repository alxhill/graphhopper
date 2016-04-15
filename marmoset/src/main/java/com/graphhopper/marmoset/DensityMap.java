package com.graphhopper.marmoset;

import com.graphhopper.marmoset.vehicle.Vehicle;
import com.graphhopper.storage.Graph;
import com.graphhopper.util.EdgeIteratorState;

import java.util.List;

/**
 * Created by alexander on 15/04/2016.
 */
public class DensityMap {

    private final Graph graph;
    // map of OSM Way ID to number of vehicles
    protected final int[] vehicleCount;

    public DensityMap(Graph graph, List<Vehicle> vehicles)
    {
        this.graph = graph;
        vehicleCount = new int[graph.getAllEdges().getMaxId()];
        vehicles.forEach(v -> vehicleCount[v.getVehicleIterator().getEdge()]++);
    }

    public double getDensity(EdgeIteratorState edge)
    {
        int vehicles = vehicleCount[edge.getEdge()];
        return vehicles / edge.getDistance();
    }
}
