package com.graphhopper.marmoset.vehicle;

import com.graphhopper.marmoset.event.EventManager;
import com.graphhopper.marmoset.util.ExpectedWeighting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by alexander on 22/04/2016.
 */
public class MultiSDVController {

    private static final Logger logger = LoggerFactory.getLogger(MultiSDVController.class);

    public static final double DAMPING_FACTOR = 0.2;
    public static final double REROUTE_PROBABILITY = 0.001;
    public static final int EXPMAP_UPDATE_FREQUENCY = 100;

    protected List<SelfDrivingVehicle> vehicles;
    protected final ExpectedWeighting expectedWeighting;

    private Random rerouteRand = new Random(9876);

    public MultiSDVController(ExpectedWeighting expectedWeighting)
    {
        this.expectedWeighting = expectedWeighting;
        vehicles = new ArrayList<>();
        EventManager.listenTo("vehicle:added", (s, vehicle) -> vehicles.add((SelfDrivingVehicle) vehicle[0]));
        EventManager.listenTo("timestep:end", (s, args) -> timestepHandler((Integer) args[0]));
    }

    public void timestepHandler(int iteration)
    {
        ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        vehicles.stream().filter(v -> rerouteRand.nextDouble() <= REROUTE_PROBABILITY).map(v -> {
            logger.info("Rerouting vehicle " + v.id);
            return (Runnable) v::recalculateRoute;
        }).forEach(es::submit);

        es.shutdown();
        try
        {
            es.awaitTermination((long) (vehicles.size()*REROUTE_PROBABILITY*200), TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e)
        {
            logger.error("==== rerouting did not finish!!! ===");
            logger.error(es.shutdownNow().size() + " remaining tasks");
        }
        logger.info("Rerouting process complete");


        if (iteration % EXPMAP_UPDATE_FREQUENCY == 0)
        {
            logger.info("Updating expected map");
            expectedWeighting.updateExpectedMap(DAMPING_FACTOR, vehicles);
        }

        vehicles = vehicles.stream().filter(v -> !v.isFinished()).collect(Collectors.toList());
    }
}
