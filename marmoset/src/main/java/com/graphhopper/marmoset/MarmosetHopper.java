package com.graphhopper.marmoset;

import com.graphhopper.GraphHopper;
import com.graphhopper.util.CmdArgs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by alexander on 15/02/2016.
 */
public class MarmosetHopper {
    GraphHopper hopper;

    List<VehicleController> vehicles;

    public MarmosetHopper() {
        hopper = new GraphHopper();
        vehicles = new ArrayList<VehicleController>();
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
        vehicles.add(new VehicleController(new Vehicle(0, 51.505, -0.09)));
    }

    public String getVehicleData() {
        StringBuilder sb = new StringBuilder();
        for (VehicleController v : vehicles)
        {
            sb.append(v.getVehicle().toString());
        }
        return sb.toString();
    }
}
