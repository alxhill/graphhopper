package com.graphhopper.marmoset;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PointList;

import java.util.List;

/**
 * Created by alexander on 09/03/2016.
 */
public class VehicleEdgeIterator implements EdgeIterator {

    private List<EdgeIteratorState> edges;
    private EdgeIteratorState edge;
    private int index;
    private FlagEncoder encoder;

    public VehicleEdgeIterator(List<EdgeIteratorState> edges, FlagEncoder encoder)
    {
        this.encoder = encoder;
        // starts at 0 to skip first edge, as the first edge is virtual (i.e not in graph)
        index = 0;
        this.edges = edges;
    }

    public VehicleEdgeIterator(VehicleEdgeIterator e)
    {
        encoder = e.encoder;
        edges = e.edges;
        edge = e.edge;
        index = e.index;
    }

    public double getRoadSpeed()
    {
        return encoder.getSpeed(edge.getFlags());
    }

    public boolean hasNext()
    {
        return index < edges.size() - 1;
    }

    @Override
    public boolean next()
    {
        index++;
        if (index >= edges.size() - 1) // to skip last virtual edge
            return false;
        edge = edges.get(index);
        return true;
    }

    @Override
    public int getEdge()
    {
        return edge.getEdge();
    }

    @Override
    public int getBaseNode()
    {
        return edge.getBaseNode();
    }

    @Override
    public int getAdjNode()
    {
        return edge.getAdjNode();
    }

    @Override
    public PointList fetchWayGeometry(int mode)
    {
        return edge.fetchWayGeometry(mode);
    }

    @Override
    public EdgeIteratorState setWayGeometry(PointList list)
    {
        return edge.setWayGeometry(list);
    }

    @Override
    public double getDistance()
    {
        return edge.getDistance();
    }

    @Override
    public EdgeIteratorState setDistance(double dist)
    {
        return edge.setDistance(dist);
    }

    @Override
    public long getFlags()
    {
        return edge.getFlags();
    }

    @Override
    public EdgeIteratorState setFlags(long flags)
    {
        return edge.setFlags(flags);
    }

    @Override
    public int getAdditionalField()
    {
        return edge.getAdditionalField();
    }

    @Override
    public boolean isForward(FlagEncoder encoder)
    {
        return edge.isForward(encoder);
    }

    @Override
    public boolean isBackward(FlagEncoder encoder)
    {
        return edge.isBackward(encoder);
    }

    @Override
    public boolean getBoolean(int key, boolean reverse, boolean _default)
    {
        return edge.getBoolean(key, reverse, _default);
    }

    @Override
    public EdgeIteratorState setAdditionalField(int value)
    {
        return edge.setAdditionalField(value);
    }

    @Override
    public String getName()
    {
        return edge.getName();
    }

    @Override
    public EdgeIteratorState setName(String name)
    {
        return edge.setName(name);
    }

    @Override
    public EdgeIteratorState detach(boolean reverse)
    {
        return edge.detach(reverse);
    }

    @Override
    public EdgeIteratorState copyPropertiesTo(EdgeIteratorState e)
    {
        return edge.copyPropertiesTo(e);
    }
}
