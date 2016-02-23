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
public class Vehicle {
    private static int maxId = 0;

    private final int id;

    private GraphHopper hopper;
    private Location loc;
    private Location dest;
    private boolean finished;

    public Vehicle(GraphHopper hopper, Location start, Location dest)
    {
        this.hopper = hopper;
        this.dest = dest;
        this.loc = start;
        this.id = maxId++;
        finished = false;
    }

    public boolean isFinished()
    {
        return finished;
    }

//    private PathWrapper route;
    public void calculateStep()
    {
        if (finished)
            return;

        GHRequest r = new GHRequest(loc.getLat(), loc.getLon(), dest.getLat(), dest.getLon());
        PathWrapper route = hopper.route(r).getBest();

        PointList path = route.getPoints();
        InstructionList il = route.getInstructions();

        if (route.getPoints().size() <= 3)
        {
            System.out.println("Vehicle " + id + " reached destination");
            finished = true;
            return;
        }

//        System.out.println(path.toString());
//        System.out.println(il.toString());
        loc.set(path.getLat(2), path.getLon(2));
    }

    @Override
    public String toString()
    {
        return String.format("%d|%s", id, loc.toString());
    }
}
