package com.graphhopper.marmoset.util;

import com.graphhopper.marmoset.vehicle.SelfDrivingVehicle;
import com.graphhopper.marmoset.vehicle.SelfDrivingVehicleIterator;
import com.graphhopper.routing.util.FastestWeighting;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by alexander on 20/04/2016.
 */
public class ExpectedWeighting extends FastestWeighting {

    protected final double[] expectedRoutes;

    private static final Logger logger = LoggerFactory.getLogger(ExpectedWeighting.class);

    public ExpectedWeighting(FlagEncoder encoder, PMap pMap, int maxId)
    {
        super(encoder, pMap);
        expectedRoutes = new double[maxId];
        logger.info("Created new expected weighting");
    }

    @Override
    public double calcWeight(EdgeIteratorState edge, boolean reverse, int prevOrNextEdge)
    {
        double speed = reverse ? flagEncoder.getReverseSpeed(edge.getFlags()) : flagEncoder.getSpeed(edge.getFlags());

        if (edge.getEdge() < expectedRoutes.length)
        {
            double expectedVehicles = expectedRoutes[edge.getEdge()];
            if (expectedVehicles > 0)
            {
                double density = 1000 * expectedVehicles / edge.getDistance();
                speed *= densityFunction(density);
            }
        }

        if (speed == 0)
            return Double.POSITIVE_INFINITY;

        return edge.getDistance() / speed * SPEED_CONV;

    }

    public void updateExpectedMap(double dampingFactor, List<SelfDrivingVehicle> vehicles)
    {
        synchronized (expectedRoutes)
        {
            for (int i = 0; i < expectedRoutes.length; i++)
            {
                expectedRoutes[i] *= dampingFactor;
            }

            List<SelfDrivingVehicleIterator> routes = vehicles.stream()
                    .map(SelfDrivingVehicle::getCurrentPath).collect(Collectors.toList());
            for (SelfDrivingVehicleIterator route : routes)
            {
                int i = 0;
                double totalLen = route.getRemainingEdges();
                while (route.next())
                {
                    i++;
                    int edge = route.getEdge();
                    if (edge < expectedRoutes.length)
                        expectedRoutes[edge] += progressFunction(i / totalLen);
                }
            }
        }
    }

    private double densityFunction(double density)
    {
        return (Math.tanh(2.0 - density / 15.0) + 1.5) / 2.5;
    }

    private double progressFunction(double progress)
    {
        return 1.0 / (4 * progress + 1.0);
    }
}
