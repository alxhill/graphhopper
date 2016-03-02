package com.graphhopper.marmoset;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.marmoset.util.CellsGraph;
import com.graphhopper.marmoset.util.Location;
import com.graphhopper.routing.Path;
import com.graphhopper.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by alexander on 16/02/2016.
 */
public class Vehicle {

    private static final Logger logger = LoggerFactory.getLogger(Vehicle.class);

    private static int maxId = 0;
    private final int id;

    private MarmosetHopper hopper;
    private Location loc;
    private Location dest;
    private boolean finished;

    private int edgeId;
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
        GHResponse ghResponse = new GHResponse();
        List<Path> paths = gh.calcPaths(ghRequest, ghResponse);
        if (ghResponse.hasErrors())
        {
            logger.error("ERRORS:" + ghResponse.getErrors().stream().map(Throwable::toString).collect(Collectors.joining("\n")));
            finished = true;
            return;
        }

        edgeList = paths.get(0).calcEdges();
        // start from 1 to avoid the 'fake' edge added by the query graph
        edgeIndex = 1;

        EdgeIteratorState e = edgeList.get(edgeIndex);
        int maxId = edgeList.stream().mapToInt(EdgeIteratorState::getEdge).max().getAsInt();
        int minId = edgeList.stream().mapToInt(EdgeIteratorState::getEdge).min().getAsInt();
        edgeId = e.getEdge();
        logger.info("edge id: " + edgeId);
        logger.info("max edge id: " + maxId);
        logger.info("min edge id: " + minId);

        cg.set(edgeId, cellId, true);

        finished = false;
    }

    private int freeCells = -1;
    public void accelerationStep()
    {
        assert !isFinished();

        freeCells = cg.freeCellsAhead(edgeId, cellId);
        if (freeCells > v+1 && v < maxVelocity)
        {
            logger.info("Accelerating");
            v++;
        }
    }

    public void slowStep()
    {
        if (freeCells < v)
        {
            logger.info("Slowing");
            v = (byte) (freeCells);
        }
    }

    public void randomStep()
    {
        int c = cg.getCellCount(edgeId);
        logger.info(id + "freecells:"+freeCells + "V:"+v + "count:"+ c);
        if (v > 0 && Math.random() < slowProb)
        {
            logger.info("Randomly slowing");
            v--;
        }
    }

    public void moveStep()
    {
        logger.info("Moving from " + cellId + " to " + (cellId + v));
        cg.set(edgeId, cellId, false);
        cellId += v;
        cg.set(edgeId, cellId, true);
    }

    public void updateLocation()
    {
        double progress = (cellId+1)/ (float) (cg.getCellCount(edgeId));
        EdgeIteratorState edge = edgeList.get(edgeIndex);

        PointList path = edge.fetchWayGeometry(3);
        if (path.isEmpty())
        {
            logger.info("Path is empty, not moving...");
            return;
        }
        logger.info("velocity:" + v);
        logger.info("progress:" + progress);

        DistanceCalc dc = new DistanceCalc2D();
        double dist = path.calcDistance(dc);
        logger.info("dist: " + dist);
        double distTravelled = progress * dist;
        double currDist = 0;
        logger.info(String.format("start(%d): %f + %f", id, currDist, distTravelled));
        int i = 0;
        while (i < path.getSize()-1 && currDist <= distTravelled)
        {
            double nextDist = dc.calcDist(path.getLat(i), path.getLon(i), path.getLat(i + 1), path.getLon(i + 1));
            logger.info(String.format("-%d|%d: %f + %f", id,i,currDist,nextDist));
            if (currDist + nextDist > distTravelled)
            {
                double partProgress = (distTravelled - currDist)/nextDist;
                double newLat = path.getLat(i) + partProgress * (path.getLat(i + 1) - path.getLat(i));
                double newLon = path.getLon(i) + partProgress * (path.getLon(i + 1) - path.getLon(i));
                loc.set(newLat, newLon);
                if (currDist + nextDist > dist)
                {
                    nextEdge();
                }
                return;
            }
            currDist += nextDist;
            i++;
        }

        // if we get here we've reached the end of the edge
        loc.set(path.getLat(path.getSize() - 1), path.getLon(path.getSize() - 1));
        nextEdge();
    }

    private void nextEdge()
    {
        edgeIndex++;
        edgeId = edgeList.get(edgeIndex).getEdge();
        cellId = 0;
    }

    @Override
    public String toString()
    {
        return String.format("%d|%s", id, loc.toString());
    }

}
