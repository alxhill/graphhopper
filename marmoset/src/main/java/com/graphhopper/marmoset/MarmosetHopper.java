package com.graphhopper.marmoset;

import com.graphhopper.GraphHopper;
import com.graphhopper.marmoset.util.Location;
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
        hopper.importOrLoad();
        vehicles.add(new VehicleController(hopper, new Vehicle(0, 51.505, -0.09), new Location(51.48, -0.10)));
        vehicles.add(new VehicleController(hopper, new Vehicle(0, 51.611621,0.10643), new Location(51.433892,-0.291824)));
    }

    public void timestep() {
        for (VehicleController v : vehicles) {
            v.calculateStep();
        }
    }

    public String getVehicleData() {
        StringBuilder sb = new StringBuilder();
        for (VehicleController v : vehicles)
        {
            sb.append(v.getVehicle().toString());
            sb.append(",");
        }
        sb.deleteCharAt(sb.length() - 1); // remove last comma
        return sb.toString();
    }
}
