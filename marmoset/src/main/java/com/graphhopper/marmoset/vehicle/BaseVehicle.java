package com.graphhopper.marmoset.vehicle;

import com.graphhopper.marmoset.Marmoset;
import com.graphhopper.marmoset.MarmosetHopper;
import com.graphhopper.marmoset.util.CellGraph;
import com.graphhopper.marmoset.util.CellIterator;
import com.graphhopper.marmoset.util.Location;
import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.DistanceCalc2D;
import com.graphhopper.util.PointList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.Random;

/**
 * Created by alexander on 16/02/2016.
 */
public abstract class BaseVehicle implements Vehicle {

    private static final Logger logger = LoggerFactory.getLogger(BaseVehicle.class);

    protected static int maxId = 0;
    protected final int id;

    protected MarmosetHopper hopper;
    protected Location loc;
    protected Location dest;
    protected boolean finished;

    protected VehicleIterator route;

    protected int cellId;
    protected int v; // velocity
    protected float slowProb;
    protected Random slowRand;
    protected int maxVelocity = 5;

    protected boolean didSlow;

    protected CellGraph cg;

    public BaseVehicle(MarmosetHopper hopper, Location start, Location dest)
    {
        this.hopper = hopper;
        this.dest = dest;
        this.loc = start;
        this.id = maxId++;

        finished = false;
        slowProb = 0.4f;
        slowRand = new Random(id);
        didSlow = false;
    }

    @Override
    public boolean didSlow()
    {
        return didSlow;
    }

    @Override
    public int getVelocity()
    {
        return v;
    }

    @Override
    public int getMaxVelocity()
    {
        CellIterator c = new CellIterator(route, cg, cellId);
        return c.getCellSpeed();
    }

    @Override
    public boolean isFinished()
    {
        return finished;
    }

    protected void finish(String error)
    {
        if (error != null)
            logger.error(error);

        if (route != null)
            cg.set(route, cellId, false);

        finished = true;
    }

    // called only if there's no error, so we use this to capture metrics
    protected void finish()
    {
        String filename = Marmoset.metricFolder + "/vehicles/" + id;
        try
        {
            PrintWriter p = new PrintWriter(filename);
            printMetrics(p);
            p.close();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

        finish(null);
    }

    @Override
    public void init()
    {
        cellId = 0;
        v = 0;

        cg = hopper.getCellGraph();

        route = getVehicleIterator();

        if (isFinished())
            return;

        route.next();

        cg.set(route, cellId, true);

        finished = false;
        didSlow = false;
    }

    @Override
    public void accelerationStep()
    {
        didSlow = false;

        CellIterator c = new CellIterator(route.duplicate(), cg, cellId);

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

    @Override
    public void slowStep()
    {
        int j = 0;
        CellIterator c = new CellIterator(route.duplicate(), cg, cellId);

        while (!c.next() && j <= v)
            j++;

        if (j <= v)
        {
            logger.debug("Slowing");
            didSlow = true;
            v = (byte) j;
        }
    }

    @Override
    public void randomStep()
    {
        if (v > 0 && slowRand.nextDouble() < slowProb)
        {
            logger.debug("Randomly slowing");
            v--;
        }
    }

    @Override
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
            finish();
            logger.info("BaseVehicle " + id + " reached destination");
        }
    }

    @Override
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

    @Override
    public void addToBuffer(ByteBuffer b)
    {
        int pos = b.position();
        b.putInt(id).putInt(v).putDouble(loc.getLat()).putDouble(loc.getLon());
        logger.debug(String.format("[%d]%d|%d|%f|%f", id, b.getInt(pos), b.getInt(pos + 4), b.getDouble(pos + 8), b.getDouble(pos + 16)));
    }

    public void printMetrics(PrintWriter p)
    {

    }
}
