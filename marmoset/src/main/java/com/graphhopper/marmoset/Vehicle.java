package com.graphhopper.marmoset;
import com.graphhopper.*;

/**
 * Created by alexander on 16/02/2016.
 */
public class Vehicle {

    public Vehicle(long startRoadId)
    {
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

    private long currentRoadId; // OSM ID of the current road
    private float lat;
    private float lon;
}
