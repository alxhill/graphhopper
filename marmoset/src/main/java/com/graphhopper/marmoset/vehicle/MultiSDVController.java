package com.graphhopper.marmoset.vehicle;

import com.graphhopper.marmoset.event.EventManager;
import com.graphhopper.marmoset.util.ExpectedWeighting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by alexander on 22/04/2016.
 */
public class MultiSDVController {

    private static final Logger logger = LoggerFactory.getLogger(MultiSDVController.class);

    public static final double DAMPING_FACTOR = 0.2;
    public static final double REROUTE_PROBABILITY = 0.001;
    public static final int EXPMAP_UPDATE_FREQUENCY = 100;

    protected LinkedList<SelfDrivingVehicle> vehicles;
    protected final ExpectedWeighting expectedWeighting;

    private Random rerouteRand = new Random(9876);

    public MultiSDVController(ExpectedWeighting expectedWeighting)
    {
        this.expectedWeighting = expectedWeighting;
        vehicles = new LinkedList<>();
        EventManager.listenTo("vehicle:added", (s, vehicle) -> {
            Vehicle v = (Vehicle) vehicle[0];
            if (v instanceof SelfDrivingVehicle)
            {
                vehicles.add((SelfDrivingVehicle) v);
            }
        });
        EventManager.listenTo("timestep:end", (s, args) -> timestepHandler((Integer) args[0]));
    }

    public void timestepHandler(int iteration)
    {
        ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
//        vehicles.stream().filter(v -> rerouteRand.nextDouble() <= REROUTE_PROBABILITY).map(v -> {
//            logger.info("Rerouting vehicle " + v.id);
//            return (Runnable) v::recalculateRoute;
//        }).forEach(es::submit);

        int rerouteCount = (int) (REROUTE_PROBABILITY * vehicles.size());
        for (int i = 0; i < rerouteCount; i++)
        {
            SelfDrivingVehicle v = vehicles.pollFirst();
            while (v.isFinished())
            {
                logger.info("Skipping vehicle " + v.id);
                v = vehicles.pollFirst();
            }

            logger.info("Rerouting vehicle " + v.id);
            es.submit((Runnable) v::recalculateRoute);
            vehicles.addLast(v);
            logger.info("vehicle count: " + vehicles.size());
        }

        es.shutdown();
        try
        {
            es.awaitTermination(rerouteCount*200, TimeUnit.MILLISECONDS);
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

//        vehicles = vehicles.stream().filter(v -> !v.isFinished()).collect(Collectors.toList());
    }
}
