package com.graphhopper.marmoset.vehicle;

import com.graphhopper.marmoset.event.EventManager;
import com.graphhopper.marmoset.util.ExpectedWeighting;
import com.graphhopper.util.CmdArgs;
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

    public final double DAMPING_FACTOR;
    public final double REROUTE_PROBABILITY;
    public final int EXPMAP_UPDATE_FREQUENCY;

    protected LinkedList<SelfDrivingVehicle> vehicles;
    protected ExpectedWeighting expectedWeighting;

    private Random rerouteRand = new Random(9876);

    public MultiSDVController(CmdArgs args)
    {
        DAMPING_FACTOR = args.getDouble("marmoset.dampingfactor", 0.2);
        REROUTE_PROBABILITY = args.getDouble("marmoset.rerouteprob", 0.001);
        EXPMAP_UPDATE_FREQUENCY = args.getInt("marmoset.expmapupdatefreq", 100);

        vehicles = new LinkedList<>();
        EventManager.listenTo("vehicle:added", (s, vehicle) -> {
            Vehicle v = (Vehicle) vehicle[0];
            if (v instanceof SelfDrivingVehicle)
            {
                vehicles.add((SelfDrivingVehicle) v);
            }
        });
        EventManager.listenTo("timestep:end", (s, a) -> timestepHandler((Integer) a[0]));
    }

    public void setExpectedWeighting(ExpectedWeighting expectedWeighting)
    {
        this.expectedWeighting = expectedWeighting;
    }

    public void timestepHandler(int iteration)
    {
        if (expectedWeighting == null)
            return;

        ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

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
