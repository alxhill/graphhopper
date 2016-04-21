package com.graphhopper.marmoset.util;

import com.graphhopper.marmoset.vehicle.SelfDrivingVehicle;
import com.graphhopper.marmoset.vehicle.Vehicle;
import com.graphhopper.routing.util.FastestWeighting;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by alexander on 20/04/2016.
 */
public class ExpectedWeighting extends FastestWeighting {

    protected final List<Vehicle> vehicles;
    protected final double DAMPING_FACTOR = 0.6;
    protected double[] expectedRoutes;

    private static final Logger logger = LoggerFactory.getLogger(ExpectedWeighting.class);

    public ExpectedWeighting(FlagEncoder encoder, PMap pMap, List<Vehicle> vehicles, int maxId)
    {
        super(encoder, pMap);
        this.vehicles = vehicles;
        expectedRoutes = new double[maxId];
        logger.info("Created new expected weighting");
    }

    @Override
    public double calcWeight(EdgeIteratorState edge, boolean reverse, int prevOrNextEdge)
    {
        double weight = super.calcWeight(edge, reverse, prevOrNextEdge);


        return weight;
    }

    public void updateExpectedMap()
    {
        for (int i = 0; i < expectedRoutes.length; i++)
        {
            expectedRoutes[i] *= DAMPING_FACTOR;
        }

        vehicles.stream().map(v -> ((SelfDrivingVehicle) v).getCurrentPath())
                .forEach(edges -> edges.forEach(edge -> expectedRoutes[edge.getEdge()]++));

    }
}
