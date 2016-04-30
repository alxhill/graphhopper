package com.graphhopper.marmoset.vehicle;

import java.nio.ByteBuffer;

/**
 * Created by alexander on 23/03/2016.
 */
public interface Vehicle {

    VehicleIterator getVehicleIterator();

    void init();
    void accelerationStep();
    void slowStep();
    void randomStep();
    void moveStep();
    void updateLocation();

    void addToBuffer(ByteBuffer byteBuffer);

    boolean isFinished();

    // metric related stuff
    boolean didSlow();
    int getVelocity();
    int getMaxVelocity();
}
