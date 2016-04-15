package com.graphhopper.marmoset;

import com.graphhopper.routing.util.FastestWeighting;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PMap;

/**
 * Created by alexander on 15/04/2016.
 */
public class DensityWeighting extends FastestWeighting {

    protected DensityMap densityMap;

    public DensityWeighting(FlagEncoder encoder, PMap pMap)
    {
        super(encoder, pMap);
    }

    @Override
    public double calcWeight(EdgeIteratorState edgeState, boolean reverse, int prevOrNextEdgeId)
    {
        double weight = super.calcWeight(edgeState, reverse, prevOrNextEdgeId);

        if (densityMap != null)
        {
            double density = densityMap.getDensity(edgeState);
            if (density > 0)
                return Double.POSITIVE_INFINITY;
        }

        return weight;
    }

    @Override
    public String getName()
    {
        return "density";
    }

    public void setDensityMap(DensityMap densityMap)
    {
        this.densityMap = densityMap;
    }
}
