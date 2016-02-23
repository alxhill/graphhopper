package com.graphhopper.marmoset;

import com.graphhopper.GraphHopper;
import com.graphhopper.marmoset.util.Location;
import com.graphhopper.util.CmdArgs;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by alexander on 15/02/2016.
 */
public class MarmosetHopper {

    GraphHopper hopper;
    List<Vehicle> vehicles;

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
        int count = 100;
        Random latRan = new Random(123);
        Random lonRan = new Random(456);

        LinkedList<Double> longitudes = lonRan.doubles(-0.5, 0.25).limit(count * 2).boxed().collect(Collectors.toCollection(LinkedList::new));
        LinkedList<Double> latitudes = latRan.doubles(51.2, 51.7).limit(count * 2).boxed().collect(Collectors.toCollection(LinkedList::new));

        while (count-- > 0)
        {
            vehicles.add(new Vehicle(hopper, new Location(latitudes.poll(), longitudes.poll()), new Location(latitudes.poll(), longitudes.poll())));
        }
    }

    public void timestep() {
        vehicles.parallelStream().forEach(Vehicle::calculateStep);
    }

    public String getVehicleData() {
        return vehicles.stream().map(Vehicle::toString).collect(Collectors.joining(","));
    }
}
