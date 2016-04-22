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
 * Created by alexander on 20/04/2016.
 */
public class SelfDrivingVehicle extends BaseVehicle {

    public SelfDrivingVehicle(MarmosetHopper hopper, Location start, Location dest)
    {
        super(hopper, start, dest);
    }

    private List<EdgeIteratorState> edgeList;

    @Override
    public VehicleIterator getVehicleIterator()
    {
        edgeList = calculateRoute();

        if (edgeList == null)
            return null;

        FlagEncoder carEncoder = hopper.getGraphHopper().getEncodingManager().getEncoder("car");
        return new SelfDrivingVehicleIterator(edgeList, carEncoder);
    }

    public List<EdgeIteratorState> getCurrentPath()
    {
        return edgeList;
    }

    public void recalculateRoute()
    {
        List<EdgeIteratorState> edges = calculateRoute();
        if (edges == null)
            return;

        edgeList = edges;

        SelfDrivingVehicleIterator sdvRoute = (SelfDrivingVehicleIterator) route;
        sdvRoute.resetEdges(edgeList);

    }

    protected List<EdgeIteratorState> calculateRoute()
    {
        GraphHopper gh = hopper.getGraphHopper();

        GHRequest ghRequest = new GHRequest(loc.getLat(), loc.getLon(), dest.getLat(), dest.getLon());
        ghRequest.setWeighting("expected");
        ghRequest.setAlgorithm("astarbi");
        GHResponse ghResponse = new GHResponse();
        List<Path> paths = gh.calcPaths(ghRequest, ghResponse);

        if (ghResponse.hasErrors())
        {
            finish("Routing failed (id "+id+"):" + ghResponse.getErrors().stream().map(Throwable::toString).collect(Collectors.joining("\n")));
            return null;
        }

        if (paths.size() == 0)
        {
            finish("No path found");
            return null;
        }

        Path p = paths.get(0);
        List<EdgeIteratorState> edges = p.calcEdges();

        if (edges.size() <= 1)
        {
            finish("Edge list too short");
            return null;
        }

        return edges;
    }
}
