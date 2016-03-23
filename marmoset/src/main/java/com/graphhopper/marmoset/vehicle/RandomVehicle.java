package com.graphhopper.marmoset.vehicle;

import com.graphhopper.GraphHopper;
import com.graphhopper.marmoset.MarmosetHopper;
import com.graphhopper.marmoset.util.Location;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.QueryResult;

/**
 * Created by alexander on 23/03/2016.
 */
public class RandomVehicle extends BaseVehicle {

    public RandomVehicle(MarmosetHopper hopper, Location start, Location dest)
    {
        super(hopper, start, dest);
    }

    @Override
    public VehicleIterator getVehicleIterator()
    {
        GraphHopper gh = hopper.getGraphHopper();
        LocationIndex index = gh.getLocationIndex();

        QueryResult qr = index.findClosest(loc.getLat(), loc.getLon(), EdgeFilter.ALL_EDGES);

        FlagEncoder carEncoder = gh.getEncodingManager().getEncoder("car");

        return new RandomVehicleIterator(qr.getClosestEdge(), carEncoder, gh.getGraphHopperStorage());
    }

}
