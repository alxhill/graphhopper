package com.graphhopper.marmoset;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.marmoset.util.CellsGraph;
import com.graphhopper.marmoset.util.Location;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.*;
import com.graphhopper.util.shapes.GHPoint3D;

import java.util.List;
import java.util.OptionalInt;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Created by alexander on 16/02/2016.
 */
public class Vehicle {
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

    private List<EdgeIteratorState> edgeList;
    private int edgeIndex;

    private CellsGraph cg;

    public Vehicle(MarmosetHopper hopper, Location start, Location dest)
    {
        slowProb = 0.0f;
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

        GHRequest ghRequest = new GHRequest(loc.getLat(), loc.getLon(), dest.getLat(), dest.getLon());
        System.out.println(loc + "->" + dest);
        GHResponse ghResponse = new GHResponse();
        List<Path> paths = gh.calcPaths(ghRequest, ghResponse);
        if (ghResponse.hasErrors())
            System.out.println("ERRORS:" + ghResponse.getErrors().stream().map(Throwable::toString).collect(Collectors.joining("\n")));

        edgeList = paths.get(0).calcEdges();
        // start from 1 to avoid the 'fake' edge added by the query graph
        edgeIndex = 1;

        EdgeIteratorState e = edgeList.get(edgeIndex);
        int maxId = edgeList.stream().mapToInt(EdgeIteratorState::getEdge).max().getAsInt();
        int minId = edgeList.stream().mapToInt(EdgeIteratorState::getEdge).min().getAsInt();
        edgeId = e.getEdge();
        System.out.println("edge id: " + edgeId);
        System.out.println("max edge id: " + maxId);
        System.out.println("min edge id: " + minId);

        cg.set(edgeId, cellId, v);
    }

    private int freeCells = -1;
    public void accelerationStep()
    {
        freeCells = cg.freeCellsAhead(edgeId, cellId);
        System.out.println(id + "freecells:"+freeCells + "V:"+v);
        if (freeCells > v+1 && v < maxVelocity)
        {
            System.out.println("Accelerating");
            v++;
        }
    }

    public void slowStep()
    {
        if (freeCells < v)
        {
            System.out.println("Slowing");
            v = (byte) (freeCells);
        }
    }

    public void randomStep()
    {
        if (v > 0 && Math.random() < slowProb)
        {
            System.out.println("Randomly slowing");
            v--;
        }
    }

    public void moveStep()
    {
        System.out.println("Moving from " + cellId + " to " + (cellId + v));
        cg.set(edgeId, cellId, 0);
        cellId += v;
        cg.set(edgeId, cellId, v);
    }

    public void updateLocation()
    {
        double progress = cellId / (float) (cg.getCellCount(edgeId)+1);
        EdgeIteratorState edge = edgeList.get(edgeIndex);

        PointList path = edge.fetchWayGeometry(3);
        if (path.isEmpty())
        {
            System.out.println("Path is empty, not moving...");
            return;
        }
        System.out.println("velocity:" + v);
        System.out.println("progress:" + progress);

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
                if (currDist + nextDist > dist)
                {
                    edgeIndex++;
                }
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
