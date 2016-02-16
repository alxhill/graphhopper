package com.graphhopper.marmoset;

import com.graphhopper.GraphHopper;

/**
 * Created by alexander on 15/02/2016.
 */
public class MarmosetHopper {
    GraphHopper hopper;

    public MarmosetHopper() {
        hopper = new GraphHopper();
    }

    public void init() {
        hopper.load("british-isles-latest.osm.pbf");
    }
}
