package com.graphhopper.marmoset;

import com.graphhopper.GraphHopper;
import com.graphhopper.marmoset.util.CellsGraph;
import com.graphhopper.marmoset.util.Location;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.*;
import com.graphhopper.util.shapes.GHPoint3D;

import java.util.Random;

/**
 * Created by alexander on 16/02/2016.
 */
public class Vehicle {
    private final long DT = 1000;

    private static int maxId = 0;
    private final int id;

    private MarmosetHopper hopper;
    private Location loc;
    private Location dest;
    private boolean finished;

    private int edgeId;
    private int adjId;
    private int cellId;

    private byte v; // velocity
    private float slowProb;
    private byte maxVelocity = 5;

    private long time;
    private CellsGraph cg;

    public Vehicle(MarmosetHopper hopper, Location start, Location dest)
    {
        time = 0;
        slowProb = 0.5f;
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

    public void init()
    {
        cellId = 0; // TODO: figure out which cell the vehicle should start at
        v = 0;

        cg = hopper.getCellsGraph();

        GraphHopper gh = hopper.getGraphHopper();
        LocationIndex locationIndex = gh.getLocationIndex();
        QueryResult closest = locationIndex.findClosest(loc.getLat(), loc.getLon(), EdgeFilter.ALL_EDGES);
        EdgeIteratorState e = closest.getClosestEdge();
        System.out.println(e);
        edgeId = closest.getClosestEdge().getEdge();
        adjId = closest.getClosestEdge().getAdjNode();
        GHPoint3D p = closest.getSnappedPoint();
        loc.set(p.lat, p.lon);

        cg.set(edgeId, cellId, v);
    }

    private int freeCells = -1;
    public void accelerationStep()
    {
        freeCells = cg.freeCellsAhead(edgeId, cellId, v + 1);
        System.out.println(id + "freecells:"+freeCells + "V:"+v);
        if (freeCells >= v && v < maxVelocity)
        {
            v++;
        }
    }

    public void slowStep()
    {
        if (freeCells < v)
        {
            v = (byte) (freeCells - 1);
        }
    }

    public void randomStep()
    {
        if (v > 0 && Math.random() > slowProb)
        {
            v--;
        }
    }

    public void moveStep()
    {
        cg.set(edgeId, cellId, 0);
        cellId += v;
        cg.set(edgeId, cellId, v);
    }

    public void updateLocation()
    {
        double progress = cellId / (float) cg.getCellCount(edgeId);
        GraphHopper gh = hopper.getGraphHopper();
        GraphHopperStorage graph = gh.getGraphHopperStorage();
        EdgeIteratorState edge = graph.getEdgeIteratorState(edgeId, adjId);

        PointList path = edge.fetchWayGeometry(3);
        if (path.isEmpty())
        {
            System.out.println("Path is empty, not moving...");
            return;
        }
        System.out.println("size:" + path.getSize());

        DistanceCalc dc = new DistanceCalc2D();
        double dist = path.calcDistance(dc);
        System.out.println("dist: " + dist);
        double distTravelled = progress * dist;
        double currDist = 0;
        System.out.printf("start(%d): %f + %f\n", id,currDist,distTravelled);
        int i = 0;
        while (i < path.getSize()-1 && currDist <= distTravelled)
        {
            double nextDist = dc.calcDist(path.getLat(i), path.getLon(i), path.getLat(i + 1), path.getLon(i + 1));
            System.out.printf("-%d|%d: %f + %f\n", id,i,currDist,nextDist);
            if (currDist + nextDist > distTravelled)
            {
                double partProgress = (distTravelled - currDist)/nextDist;
                double newLat = path.getLat(i) + partProgress * (path.getLat(i + 1) - path.getLat(i));
                double newLon = path.getLon(i) + partProgress * (path.getLon(i + 1) - path.getLon(i));
                loc.set(newLat, newLon);
                return;
            }
            currDist += nextDist;
        }

    }

    @Override
    public String toString()
    {
        return String.format("%d|%s", id, loc.toString());
    }

}
