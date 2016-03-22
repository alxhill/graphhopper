package com.graphhopper.marmoset.vehicle;

import com.graphhopper.util.EdgeIterator;

/**
 * Created by alexander on 22/03/2016.
 */
public interface VehicleIterator extends EdgeIterator{
    double getRoadSpeed();
    boolean hasNext();
}
