package com.graphhopper.marmoset.vehicle;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.marmoset.MarmosetHopper;
import com.graphhopper.marmoset.util.Location;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.util.EdgeIteratorState;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by alexander on 23/03/2016.
 */
public class DijkstraVehicle extends BaseVehicle {

    public DijkstraVehicle(MarmosetHopper hopper, Location start, Location dest)
    {
        super(hopper, start, dest);
    }

    @Override
    public VehicleIterator getVehicleIterator()
    {
        GraphHopper gh = hopper.getGraphHopper();

        GHRequest ghRequest = new GHRequest(loc.getLat(), loc.getLon(), dest.getLat(), dest.getLon());
        GHResponse ghResponse = new GHResponse();
        List<Path> paths = gh.calcPaths(ghRequest, ghResponse);
        if (ghResponse.hasErrors())
        {
            finish("Routing failed:" + ghResponse.getErrors().stream().map(Throwable::toString).collect(Collectors.joining("\n")));
            return null;
        }

        if (paths.size() == 0)
        {
            finish("No path found");
            return null;
        }

        Path p = paths.get(0);
        List<EdgeIteratorState> edgeList = p.calcEdges();

        if (edgeList.size() <= 1)
        {
            finish("Edge list too short");
            return null;
        }

        FlagEncoder carEncoder = gh.getEncodingManager().getEncoder("car");
        return new DijkstraVehicleIterator(edgeList, carEncoder);
    }
}
