package com.graphhopper.marmoset;

import com.graphhopper.GraphHopper;
import com.graphhopper.marmoset.util.CellGraph;
import com.graphhopper.marmoset.util.Location;
import com.graphhopper.marmoset.vehicle.DijkstraVehicle;
import com.graphhopper.marmoset.vehicle.RandomVehicle;
import com.graphhopper.marmoset.vehicle.Vehicle;
import com.graphhopper.util.CmdArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by alexander on 15/02/2016.
 */
public class MarmosetHopper {

    protected GraphHopper hopper;
    protected CellGraph cellGraph;
    protected List<Vehicle> vehicles;

    protected boolean isPaused;

    protected Random rand = new Random(999);
    protected double randPercent;

    private static Logger logger = LoggerFactory.getLogger(MarmosetHopper.class);

    public MarmosetHopper() {
        hopper = new GraphHopper();
        vehicles = new ArrayList<>();
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

        args.put("osmreader.osm", "british-isles-latest.osm.pbf");

        hopper.init(args);
        hopper.importOrLoad();

        float cellSize = (float) args.getDouble("marmoset.cellsize", 10.0);
        cellGraph = new CellGraph(hopper.getGraphHopperStorage().getBaseGraph(), cellSize);
        cellGraph.init();

        randPercent = args.getDouble("marmoset.randpercent", 0.2);
        assert randPercent >= 0 && randPercent <= 1;
    }

    public synchronized void addVehicle()
    {
        Vehicle v;
        if (rand.nextDouble() < randPercent)
            v = new RandomVehicle(this, Location.randLondon(), Location.randCentralLondon());
        else
            v = new DijkstraVehicle(this, Location.randLondon(), Location.randCentralLondon());
        v.init();
        if (v.isFinished())
            addVehicle();
        else
            vehicles.add(v);
    }

    public synchronized void startSimulation(int initialVehicles)
    {
        logger.info("Starting simulation with " + initialVehicles + " vehicles");
        IntStream.range(0, initialVehicles).forEach(v -> addVehicle());
    }

    public synchronized boolean timestep()
    {
        if (isPaused || vehicles.size() == 0)
            return false;

        vehicles.stream().forEach(Vehicle::accelerationStep);
        vehicles.stream().forEach(Vehicle::slowStep);
        vehicles.stream().forEach(Vehicle::randomStep);
        vehicles.stream().forEach(Vehicle::moveStep);
        vehicles.stream().forEach(Vehicle::updateLocation);

        vehicles = vehicles.stream().filter(v -> !v.isFinished()).collect(Collectors.toList());

        return true;
    }

    public synchronized Metrics getMetrics()
    {
        if (vehicles.size() == 0)
            return null;

        int slowed = vehicles.stream().mapToInt(v -> v.didSlow() ? 1 : 0).reduce(0, (acc, i) -> acc + i);
        double averageCells = vehicles.stream().mapToDouble(Vehicle::getVelocity).average().getAsDouble();
        long notAtMax = vehicles.stream().filter(v -> v.getMaxVelocity() <= v.getVelocity()).count();

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
        public int slowed;
        public double averageCells;
        public long notAtMax;
        public int vehicleCount;

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
}
