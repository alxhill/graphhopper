package com.graphhopper.marmoset;

import com.graphhopper.marmoset.util.Location;

/**
 * Created by alexander on 16/02/2016.
 */
public class Vehicle {

    private static int maxId = 0;

    private long currentRoadId; // OSM ID of the current road
    private Location loc;
    public final int id;

    public Vehicle(long startRoadId, double lat, double lon)
    {
        this.loc = new Location(lat, lon);
        this.id = maxId++;
        this.currentRoadId = startRoadId;
    }

    public long getCurrentRoadId()
    {
        return currentRoadId;
    }

    public Location getLocation()
    {
        return loc;
    }

    @Override
    public String toString()
    {
        return String.valueOf(id) + "|" + loc.toString();
    }

    public void moveTo(long newRoad, double newLat, double newLon)
    {
        currentRoadId = newRoad;
        loc.set(newLat, newLon);
    }

    public void moveBy(double latDiff, double lonDiff)
    {
        loc.add(latDiff, lonDiff);
    }
}
