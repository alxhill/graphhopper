package com.graphhopper.marmoset.vehicle;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.marmoset.MarmosetHopper;
import com.graphhopper.marmoset.util.CellIterator;
import com.graphhopper.marmoset.util.CellGraph;
import com.graphhopper.marmoset.util.Location;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Created by alexander on 16/02/2016.
 */
public class Vehicle {

    protected static final Logger logger = LoggerFactory.getLogger(Vehicle.class);

    protected static int maxId = 0;
    protected final int id;

    protected MarmosetHopper hopper;
    protected Location loc;
    protected Location dest;
    protected boolean finished;

    protected DijkstraVehicleIterator route;

    protected int cellId;
    protected int v; // velocity
    protected float slowProb;
    protected Random slowRand;
    protected int maxVelocity = 5;

    protected CellGraph cg;

    public Vehicle(MarmosetHopper hopper, Location start, Location dest)
    {
        this.hopper = hopper;
        this.dest = dest;
        this.loc = start;
        this.id = maxId++;

        finished = false;
        slowProb = 0.4f;
        slowRand = new Random(id);
    }

    public boolean isFinished()
    {
        return finished;
    }

    private void finish(String error)
    {
        if (error != null)
            logger.error(error);

        if (route != null)
            cg.set(route, cellId, false);

        finished = true;
    }

    public void init()
    {
        cellId = 0; // TODO: figure out which cell the vehicle should start at
        v = 0;

        cg = hopper.getCellGraph();

        GraphHopper gh = hopper.getGraphHopper();

        GHRequest ghRequest = new GHRequest(loc.getLat(), loc.getLon(), dest.getLat(), dest.getLon());
        GHResponse ghResponse = new GHResponse();
        List<Path> paths = gh.calcPaths(ghRequest, ghResponse);
        if (ghResponse.hasErrors())
        {
            finish("Routing failed:" + ghResponse.getErrors().stream().map(Throwable::toString).collect(Collectors.joining("\n")));
            return;
        }

        if (paths.size() == 0)
        {
            finish("No path found");
            return;
        }

        Path p = paths.get(0);
        List<EdgeIteratorState> edgeList = p.calcEdges();

        if (edgeList.size() <= 1)
        {
            finish("Edge list too short");
            return;
        }

        FlagEncoder carEncoder = gh.getEncodingManager().getEncoder("car");
        route = new DijkstraVehicleIterator(edgeList, carEncoder);
        route.next();

        cg.set(route, cellId, true);

        finished = false;
    }

    public void accelerationStep()
    {
        CellIterator c = new CellIterator(new DijkstraVehicleIterator(route), cg, cellId);

        int newMaxVel = maxVelocity;

        int freeCells = 0;
        while (!c.next() && freeCells < v + 1)
        {
            newMaxVel = Math.min(newMaxVel, c.getCellSpeed());
            freeCells++;
        }
        // because who actually drives at the speed limit?
        maxVelocity = Math.max(2, newMaxVel);

        if (v >= maxVelocity) {
            v = maxVelocity;
            return;
        }

        if (freeCells == v + 1)
        {
            logger.debug("Accelerating");
            v++;
        }

    }

    public void slowStep()
    {
        int j = 0;
        CellIterator c = new CellIterator(new DijkstraVehicleIterator(route), cg, cellId);

        while (!c.next() && j <= v)
            j++;

        if (j <= v)
        {
            logger.debug("Slowing");
            v = (byte) j;
        }
    }

    public void randomStep()
    {
        if (v > 0 && slowRand.nextDouble() < slowProb)
        {
            logger.debug("Randomly slowing");
            v--;
        }
    }

    public void moveStep()
    {
        logger.debug("Moving from " + cellId + " to " + (cellId + v) + " (unless it's going over the edge)");
        cg.set(route, cellId, false);
        CellIterator c = new CellIterator(route, cg, cellId);
        int steps = v;
        while (steps > 0)
        {
            c.next();
            steps--;
        }
        cellId = c.getCellIndex();
        cg.set(route, cellId, true);

        if (!route.hasNext() && cellId == cg.getCellCount(route) - 1)
        {
            finished = true;
            logger.info("Vehicle " + id + " reached destination");
        }
    }

    public void updateLocation()
    {
        double progress = (cellId + 1) / (float) (cg.getCellCount(route));

        PointList path = route.fetchWayGeometry(3);
        if (path.isEmpty())
        {
            logger.debug("Path is empty, not moving...");
            return;
        }
        logger.debug("velocity:" + v);
        logger.debug("progress:" + progress);

        DistanceCalc dc = new DistanceCalc2D();
        double dist = path.calcDistance(dc);
        logger.debug("dist: " + dist);
        double distTravelled = progress * dist;
        double currDist = 0;
        logger.debug(String.format("start(%d): %f + %f", id, currDist, distTravelled));
        int i = 0;
        while (i < path.getSize() - 1 && currDist <= distTravelled)
        {
            double nextDist = dc.calcDist(path.getLat(i), path.getLon(i), path.getLat(i + 1), path.getLon(i + 1));
            logger.debug(String.format("-%d|%d: %f + %f", id, i, currDist, nextDist));
            if (currDist + nextDist > distTravelled)
            {
                double partProgress = (distTravelled - currDist) / nextDist;
                double newLat = path.getLat(i) + partProgress * (path.getLat(i + 1) - path.getLat(i));
                double newLon = path.getLon(i) + partProgress * (path.getLon(i + 1) - path.getLon(i));
                loc.set(newLat, newLon);
                return;
            }
            currDist += nextDist;
            i++;
        }

        // if we get here we've reached the end of the edge
        loc.set(path.getLat(path.getSize() - 1), path.getLon(path.getSize() - 1));
    }

    @Override
    public String toString()
    {
        return String.format("%d|%s|%d", id, loc.toString(), v);
    }

    public void addToBuffer(ByteBuffer b)
    {
        int pos = b.position();
        b.putInt(id).putInt(v).putDouble(loc.getLat()).putDouble(loc.getLon());
        logger.debug(String.format("[%d]%d|%d|%f|%f", id, b.getInt(pos), b.getInt(pos + 4), b.getDouble(pos + 8), b.getDouble(pos + 16)));
    }
}
