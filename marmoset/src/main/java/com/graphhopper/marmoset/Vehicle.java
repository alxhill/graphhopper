package com.graphhopper.marmoset;

/**
 * Created by alexander on 16/02/2016.
 */
public class Vehicle {

    private static int maxId = 0;

    private long currentRoadId; // OSM ID of the current road
    private double lat;
    private double lon;
    public final int id;

    public Vehicle(long startRoadId, double lat, double lon)
    {
        this.lat = lat;
        this.lon = lon;
        this.id = maxId++;
        this.currentRoadId = startRoadId;
    }

    public long getCurrentRoadId()
    {
        return currentRoadId;
    }

    public double getLat()
    {
        return lat;
    }

    public double getLon()
    {
        return lon;
    }

    @Override
    public String toString()
    {
        return String.format("%d|%f|%f", id, getLat(), getLon());
    }
}
