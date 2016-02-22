package com.graphhopper.marmoset;

/**
 * Created by alexander on 16/02/2016.
 */
public class VehicleController {
    private Vehicle vehicle;

    public VehicleController(Vehicle vehicle)
    {
        this.vehicle = vehicle;
    }

    public Vehicle getVehicle()
    {
        return vehicle;
    }

    public void calculateStep() {
        vehicle.moveTo(0, vehicle.getLat() + 0.01, vehicle.getLon() + 0.01);
    }
}
