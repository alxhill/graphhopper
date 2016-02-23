package com.graphhopper.marmoset.util;

/**
 * Created by alexander on 23/02/2016.
 */
public class Location {
    private double lat;
    private double lon;

    public Location(double lat, double lon)
    {
        this.set(lat, lon);
    }

    public void add(double lat, double lon)
    {
        this.lat += lat;
        this.lon += lon;
    }

    public void set(double lat, double lon)
    {
        this.lat = lat;
        this.lon = lon;
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
        return String.format("%f.10|%f.10", lat, lon);
    }
}
