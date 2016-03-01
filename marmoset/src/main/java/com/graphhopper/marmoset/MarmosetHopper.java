package com.graphhopper.marmoset;

import com.graphhopper.GraphHopper;
import com.graphhopper.marmoset.util.CellsGraph;
import com.graphhopper.marmoset.util.Location;
import com.graphhopper.util.CmdArgs;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by alexander on 15/02/2016.
 */
public class MarmosetHopper {

    private GraphHopper hopper;
    private CellsGraph cellsGraph;
    private List<Vehicle> vehicles;

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

        cellsGraph = new CellsGraph(hopper.getGraphHopperStorage(), 10);
        cellsGraph.init();

        int count = args.getInt("marmoset.vehicles", 1000);
        Random latRan = new Random(123);
        Random lonRan = new Random(456);

        ArrayList<Double> lons = lonRan.doubles(-0.5, 0.25).limit(count * 2).boxed().collect(Collectors.toCollection(ArrayList::new));
        ArrayList<Double> lats = latRan.doubles(51.2, 51.7).limit(count * 2).boxed().collect(Collectors.toCollection(ArrayList::new));

        vehicles = IntStream.range(0, count).map(c -> c * 2)
                .mapToObj(c -> new Vehicle(this, new Location(lats.get(c), lons.get(c)), new Location(c+1, c+1)))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public void startSimulation()
    {
        vehicles.parallelStream().forEach(Vehicle::init);
    }

    public void timestep() {
        vehicles.stream().forEach(Vehicle::accelerationStep);
        vehicles.stream().forEach(Vehicle::slowStep);
        vehicles.stream().forEach(Vehicle::randomStep);
        vehicles.stream().forEach(Vehicle::moveStep);
        vehicles.stream().forEach(Vehicle::updateLocation);
    }

    public String getVehicleData() {
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
