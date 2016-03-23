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
    }

    public synchronized void addVehicle()
    {
        Vehicle v;
        if (Math.random() < 0.2)
            v = new DijkstraVehicle(this, Location.randLondon(), Location.randCentralLondon());
        else
            v = new RandomVehicle(this, Location.randLondon(), Location.randCentralLondon());
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

    public synchronized void timestep()
    {
        if (isPaused)
            return;
        vehicles.stream().forEach(Vehicle::accelerationStep);
        vehicles.stream().forEach(Vehicle::slowStep);
        vehicles.stream().forEach(Vehicle::randomStep);
        vehicles.stream().forEach(Vehicle::moveStep);
        vehicles.stream().forEach(Vehicle::updateLocation);

        vehicles = vehicles.stream().filter(v -> !v.isFinished()).collect(Collectors.toList());
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
}
