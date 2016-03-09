package com.graphhopper.marmoset.util;

import java.util.Random;

/**
 * Created by alexander on 23/02/2016.
 */
public class Location {

    private static Random latRan = new Random(123);
    private static Random lonRan = new Random(456);

    public static Location randLondon()
    {
        double lat = randBound(latRan, 51.2, 51.7);
        double lon = randBound(lonRan, -0.5, 0.25);
        return new Location(lat, lon);
    }

    public static Location randCentralLondon()
    {
        double lat = randBound(latRan, 51.4, 51.5);
        double lon = randBound(lonRan, -0.2, 0.0);
        return new Location(lat, lon);
    }

    private static double randBound(Random r, double low, double high)
    {
        double range = high - low;
        return r.nextDouble() * range + low;
    }

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
        return String.format("%.10f|%.10f", lat, lon);
    }
}
