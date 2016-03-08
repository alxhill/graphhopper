package com.graphhopper.marmoset;

import com.graphhopper.GraphHopper;
import com.graphhopper.marmoset.util.CellsGraph;
import com.graphhopper.marmoset.util.Location;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.util.CmdArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by alexander on 15/02/2016.
 */
public class MarmosetHopper {

    private GraphHopper hopper;
    private CellsGraph cellsGraph;
    private List<Vehicle> vehicles;

    private static Logger logger = LoggerFactory.getLogger(MarmosetHopper.class);

    public MarmosetHopper() {
        hopper = new GraphHopper();
        vehicles = new ArrayList<>();
    }

    public void init()
    {
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

        double cellSize = args.getDouble("marmoset.cellsize", 10.0);
        cellsGraph = new CellsGraph(hopper.getGraphHopperStorage().getBaseGraph(), cellSize);
        cellsGraph.init(getFlagEncoder());
    }

    public FlagEncoder getFlagEncoder()
    {
        EncodingManager em = hopper.getEncodingManager();
        List<FlagEncoder> encoders = em.fetchEdgeEncoders();
        if (encoders.size() <= 0)
        {
            logger.error("No flag encoders found!");
            return null;
        }

        if (encoders.size() > 1)
            logger.warn("Multiple encoders found - using the first (" + encoders.get(0).toString() + ")");

        return encoders.get(0);
    }

    private Random latRan = new Random(123);
    private Random lonRan = new Random(456);
    private double randBound(Random r, double low, double high)
    {
        double range = high - low;
        return r.nextDouble() * range + low;
    }

    private Location randLondon()
    {
        double lat = randBound(latRan, 51.2, 51.7);
        double lon = randBound(lonRan, -0.5, 0.25);
        return new Location(lat, lon);
    }

    public synchronized void addVehicle()
    {
        Vehicle v = new Vehicle(this, randLondon(), randLondon());
        v.init();
        if (v.isFinished())
            addVehicle();
        else
            vehicles.add(v);
    }

    public synchronized void startSimulation(int initialVehicles)
    {
        IntStream.range(0, initialVehicles).forEach(v -> addVehicle());
        vehicles = vehicles.stream().filter(v -> !v.isFinished()).collect(Collectors.toList());
    }

    public synchronized void timestep()
    {
        vehicles.stream().forEach(Vehicle::accelerationStep);
        vehicles.stream().forEach(Vehicle::slowStep);
        vehicles.stream().forEach(Vehicle::randomStep);
        vehicles.stream().forEach(Vehicle::moveStep);
        vehicles.stream().forEach(Vehicle::updateLocation);

        vehicles = vehicles.stream().filter(v -> !v.isFinished()).collect(Collectors.toList());
    }

    public synchronized String getVehicleData()
    {
        return vehicles.parallelStream().map(Vehicle::toString).collect(Collectors.joining(","));
    }

    public GraphHopper getGraphHopper()
    {
        return hopper;
    }

    public CellsGraph getCellsGraph()
    {
        return cellsGraph;
    }
}
