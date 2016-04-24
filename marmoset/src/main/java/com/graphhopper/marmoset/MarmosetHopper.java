package com.graphhopper.marmoset;

import com.graphhopper.GraphHopper;
import com.graphhopper.marmoset.event.EventManager;
import com.graphhopper.marmoset.util.CellGraph;
import com.graphhopper.marmoset.util.ExpectedWeighting;
import com.graphhopper.marmoset.util.Location;
import com.graphhopper.marmoset.vehicle.*;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.Weighting;
import com.graphhopper.routing.util.WeightingMap;
import com.graphhopper.util.CmdArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by alexander on 15/02/2016.
 */
public class MarmosetHopper {

    protected MarmosetGraphHopper hopper;
    protected CellGraph cellGraph;
    protected List<Vehicle> vehicles;

    protected static MultiSDVController sdvController;

    protected boolean isPaused;

    protected Random rand = new Random(999);
    protected double sdvPercent;

    private static Logger logger = LoggerFactory.getLogger(MarmosetHopper.class);

    public MarmosetHopper() {
        hopper = new MarmosetGraphHopper();
        vehicles = Collections.synchronizedList(new ArrayList<>());
    }

    public void init()
    {
        isPaused = false;
        CmdArgs args;
        try
        {
            args = CmdArgs.readFromConfig("config.properties", "graphhopper.config");
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return;
        }

        args.put("osmreader.osm", "london.osm.pbf");

        hopper.init(args);
        hopper.importOrLoad();

        float cellSize = (float) args.getDouble("marmoset.cellsize", 10.0);
        cellGraph = new CellGraph(hopper.getGraphHopperStorage().getBaseGraph(), cellSize);
        cellGraph.init();

        sdvPercent = args.getDouble("marmoset.sdvpercent", 1);
        assert sdvPercent >= 0 && sdvPercent <= 1;
    }

    public void addVehicle()
    {
        Vehicle v;
        if (rand.nextDouble() < sdvPercent)
            v = new SelfDrivingVehicle(this, Location.randLondon(), Location.randCentralLondon());
        else
            v = new DijkstraVehicle(this, Location.randLondon(), Location.randCentralLondon());
        v.init();
        if (v.isFinished())
            addVehicle();
        else {
            vehicles.add(v);
            EventManager.trigger("vehicle:added", v);
        }
    }

    public synchronized void startSimulation(int initialVehicles)
    {
        logger.info("Starting simulation with " + initialVehicles + " vehicles");
        IntStream.range(0, initialVehicles).parallel().forEach(v -> addVehicle());
    }

    public boolean timestep()
    {
        return timestep(true);
    }

    public synchronized boolean timestep(boolean webMode)
    {
        long startTimestep = System.nanoTime();
        if (isPaused || vehicles.size() == 0)
            return false;

        vehicles.parallelStream().forEach(Vehicle::accelerationStep);
        vehicles.parallelStream().forEach(Vehicle::slowStep);
        vehicles.parallelStream().forEach(Vehicle::randomStep);
        vehicles.parallelStream().forEach(Vehicle::moveStep);
        if (webMode)
            vehicles.stream().forEach(Vehicle::updateLocation);

        vehicles = Collections.synchronizedList(
                vehicles.parallelStream().filter(v -> !v.isFinished()).collect(Collectors.toList()));

        logger.info("Timestep took " + (System.nanoTime() - startTimestep) / 1e6 + "ms");
        return true;
    }

    public synchronized Metrics getMetrics()
    {
        if (vehicles.size() == 0)
            return null;

        int slowed = vehicles.parallelStream().mapToInt(v -> v.didSlow() ? 1 : 0).reduce(0, (acc, i) -> acc + i);
        double averageCells = vehicles.parallelStream().mapToDouble(Vehicle::getVelocity).average().getAsDouble();
        long notAtMax = vehicles.parallelStream().filter(v -> v.getVelocity() < v.getMaxVelocity()).count();

        return new Metrics(slowed, averageCells, notAtMax, vehicles.size());
    }

    public synchronized String getVehicleString()
    {
        return vehicles.stream().map(Vehicle::toString).collect(Collectors.joining(","));
    }

    public synchronized ByteBuffer getVehicleBytes()
    {
        ByteBuffer buffer =  ByteBuffer.allocate(vehicles.size() * 4 * 6);
        vehicles.stream().forEach(v -> v.addToBuffer(buffer));
        buffer.rewind();
        return buffer;
    }

    public GraphHopper getGraphHopper()
    {
        return hopper;
    }

    public CellGraph getCellGraph()
    {
        return cellGraph;
    }

    public void pause()
    {
        logger.info("Pausing simulation");
        isPaused = true;
    }
    public void unpause()
    {
        logger.info("Resuming simulation");
        isPaused = false;
    }

    public boolean paused()
    {
        return isPaused;
    }

    public int getVehicleCount()
    {
        return vehicles.size();
    }

    public static class Metrics {
        public final int slowed;
        public final double averageCells;
        public final long notAtMax;
        public final int vehicleCount;

        public Metrics(int slowed, double averageCells, long notAtMax, int vehicleCount)
        {
            this.slowed = slowed;
            this.averageCells = averageCells;
            this.notAtMax = notAtMax;
            this.vehicleCount = vehicleCount;
        }

        public static String getHeader()
        {
            return "VehicleCount,VehiclesSlowed,AverageCellSpeed,NotAtMax";
        }

        @Override
        public String toString()
        {
            return String.format("%d,%d,%f,%d", vehicleCount, slowed, averageCells, notAtMax);
        }

        public String getDescription()
        {
            return String.format("%d/%d (%.2f%%) of vehicles slowed, moving at %.2fc/s with %d not at max",
                    slowed, vehicleCount, (float) slowed * 100.0 / vehicleCount,
                    averageCells, notAtMax);
        }
    }

    public static class MarmosetGraphHopper extends GraphHopper {

        public static ExpectedWeighting expectedWeighting;

        @Override
        public synchronized Weighting createWeighting(WeightingMap wMap, FlagEncoder encoder)
        {
            if ("expected".equalsIgnoreCase(wMap.getWeighting()))
            {
                if (expectedWeighting == null)
                {
                    int maxId = this.getGraphHopperStorage().getAllEdges().getMaxId();
                    expectedWeighting = new ExpectedWeighting(encoder, wMap, maxId);
                    sdvController = new MultiSDVController(expectedWeighting);
                }
                return expectedWeighting;
            }
            return super.createWeighting(wMap, encoder);
        }

    }
}
