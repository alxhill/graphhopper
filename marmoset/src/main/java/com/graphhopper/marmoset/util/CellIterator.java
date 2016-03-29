package com.graphhopper.marmoset.util;

import com.graphhopper.marmoset.vehicle.VehicleIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by alexander on 09/03/2016.
 */
public class CellIterator {

    private VehicleIterator route;
    private CellGraph cg;
    private int cellIndex;
    private static final Logger logger = LoggerFactory.getLogger(CellIterator.class);

    public CellIterator(VehicleIterator route, CellGraph cellGraph, int cellId)
    {
        this.route = route;
        this.cg = cellGraph;
        this.cellIndex = cellId;
    }

    public boolean next()
    {
        cellIndex++;
        if (cellIndex >= cg.getCellCount(route))
        {
            if (route.hasNext())
            {
                cellIndex = 0;
                route.next();
            }
            else
            {
                // allows repeated calls after reaching destination
                cellIndex = cg.getCellCount(route) - 1;
            }
        }
        return cg.get(route, cellIndex);
    }

    public int getCellIndex()
    {
        return cellIndex;
    }

    public int getCellSpeed()
    {
        double roadSpeed = route.getRoadSpeed();
        double v = (roadSpeed / (cg.cellSize * 3.6));
        return Math.max(1, (int) Math.ceil(v));
    }
}
