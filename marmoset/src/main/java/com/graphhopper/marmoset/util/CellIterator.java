package com.graphhopper.marmoset.util;

import com.graphhopper.marmoset.VehicleEdgeIterator;

/**
 * Created by alexander on 09/03/2016.
 */
public class CellIterator {

    private VehicleEdgeIterator route;
    private CellsGraph cg;
    private int cellIndex;

    public CellIterator(VehicleEdgeIterator route, CellsGraph cellsGraph, int cellId)
    {
        this.route = route;
        this.cg = cellsGraph;
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
}
