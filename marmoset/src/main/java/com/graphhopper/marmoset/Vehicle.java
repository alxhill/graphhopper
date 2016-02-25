package com.graphhopper.marmoset;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.PathWrapper;
import com.graphhopper.marmoset.util.Location;
import com.graphhopper.util.Instruction;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint3D;

/**
 * Created by alexander on 16/02/2016.
 */
public class Vehicle {
    private final long DT = 1000;

    private static int maxId = 0;
    private final int id;

    private GraphHopper hopper;
    private Location loc;
    private Location dest;
    private boolean finished;

    private long time;

    public Vehicle(GraphHopper hopper, Location start, Location dest)
    {
        time = 0;
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

    private InstructionList il;
    private int currInstr;
    private long instrStart;
    private PointList path;
    private int currStep;
    public void calculateStep()
    {
        if (finished)
            return;
        if (il == null)
        {
            GHRequest request = new GHRequest(loc.getLat(), loc.getLon(), dest.getLat(), dest.getLon());
            GHResponse response = hopper.route(request);
            if (response.hasErrors()) {
                System.out.println("Response has errors, dumping:");
                response.getErrors().stream().forEach(Throwable::printStackTrace);
                System.out.println();
                finished = true;
                return;
            }
            il = response.getBest().getInstructions();
            path = response.getBest().getPoints();
            currStep = 0;
            currInstr = 0;
            instrStart = 0;
        }
//        if (instrStart + il.get(currInstr).getTime() < time) {
//            instrStart += il.get(currInstr).getTime();
//            currInstr++;
//            if (currInstr >= il.getSize())
//            {
//                System.out.println("Finished moving vehicle " + id);
//                finished = true;
//                return;
//            }
//        }
//        Instruction inst = il.get(currInstr);

//        final int speed = 60;
//        PointList path = inst.getPoints();
//        path.toGeoJson(false);
        if (currStep >= path.size()) {
            finished = true;
            return;
        }

        loc.set(path.getLat(currStep), path.getLon(currStep));
        currStep++;
        time += DT;
    }

    @Override
    public String toString()
    {
        return String.format("%d|%s", id, loc.toString());
    }
}
