package com.graphhopper.marmoset;

import com.graphhopper.GraphHopper;
import com.graphhopper.marmoset.util.CellsGraph;
import com.graphhopper.marmoset.util.Location;
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
        cellsGraph.init();
    }

    public synchronized void addVehicle()
    {
        Vehicle v = new Vehicle(this, Location.randLondon(), Location.randCentralLondon());
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
