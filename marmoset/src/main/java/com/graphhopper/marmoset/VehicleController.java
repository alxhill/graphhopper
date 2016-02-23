package com.graphhopper.marmoset;

import com.graphhopper.GHRequest;
import com.graphhopper.GraphHopper;
import com.graphhopper.PathWrapper;
import com.graphhopper.marmoset.util.Location;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.PointList;

/**
 * Created by alexander on 16/02/2016.
 */
public class VehicleController {
    private Vehicle vehicle;
    private GraphHopper hopper;
    private Location dest;

    public VehicleController(GraphHopper hopper, Vehicle vehicle, Location dest)
    {
        this.hopper = hopper;
        this.vehicle = vehicle;
        this.dest = dest;
    }

    public Vehicle getVehicle()
    {
        return vehicle;
    }

    public void calculateStep()
    {
        GHRequest r = new GHRequest(vehicle.getLocation().getLat(), vehicle.getLocation().getLon(),
                dest.getLat(), dest.getLon());
        PathWrapper route = hopper.route(r).getBest();

        PointList path = route.getPoints();
        InstructionList il = route.getInstructions();

        System.out.println(path.toString());
        System.out.println(il.toString());
        vehicle.moveTo(0, path.getLat(2), path.getLon(2));
    }
}
