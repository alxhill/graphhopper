package com.graphhopper.marmoset;
import com.graphhopper.*;

/**
 * Created by alexander on 16/02/2016.
 */
public class Vehicle {

    private static int maxId = 0;

    private long currentRoadId; // OSM ID of the current road
    private float lat;
    private float lon;
    public final int id;

    public Vehicle(long startRoadId)
    {
        this.id = maxId++;
        this.currentRoadId = startRoadId;
    }

    public long getCurrentRoadId()
    {
        return currentRoadId;
    }

    public float getLat()
    {
        return lat;
    }

    public float getLon()
    {
        return lon;
    }

    @Override
    public String toString()
    {
        return String.format("%d|%f|%f", id, getLat(), getLon());
    }
}
