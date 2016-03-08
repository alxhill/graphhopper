package com.graphhopper.marmoset.util;

import com.graphhopper.routing.util.AllEdgesIterator;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.storage.Graph;

/**
 * Created by alexander on 01/03/2016.
 */
public class CellsGraph {
    private final double cellSize;
    private Graph graph;
    private boolean[][] cells;
    private boolean[][] reverseCells;

    public CellsGraph(Graph graph, double cellSize) {
        this.cellSize = cellSize;
        this.graph = graph;

    }

    public void init(FlagEncoder fe)
    {
        AllEdgesIterator iterator = graph.getAllEdges();
        cells = new boolean[iterator.getMaxId()][];
        reverseCells = new boolean[iterator.getMaxId()][];
        while (iterator.next())
        {
            int cellCount = Math.max(1, (int) (iterator.getDistance() / cellSize));

            if (fe.isForward(iterator.getFlags()))
                cells[iterator.getEdge()] = new boolean[cellCount];

            if (fe.isBackward(iterator.getFlags()))
                reverseCells[iterator.getEdge()] = new boolean[cellCount];
        }
    }

    public int getCellCount(int edgeId)
    {
        if (edgeId >= cells.length)
            throw new ArrayIndexOutOfBoundsException(
                    String.format("EdgeId '%d' out of bounds (max %d)", edgeId, cells.length));
        return cells[edgeId].length;
    }

    public int freeCellsAhead(int edgeId, int cellId)
    {
        int move = 1;
        while (cellId + move < cells[edgeId].length)
        {
            if (!cells[edgeId][cellId + move])
                move++;
            else
                return move-1;
        }

        return move-1;
    }

    public void set(int edgeId, int cellId, boolean hasVehicle)
    {
        if (edgeId >= cells.length)
            throw new ArrayIndexOutOfBoundsException(
                    String.format("EdgeId '%d' out of bounds (max %d)", edgeId, cells.length));
        if (cellId >= cells[edgeId].length)
            throw new ArrayIndexOutOfBoundsException(
                    String.format("CellId '%d' out of bounds (max %d) for edge %d", cellId, cells[edgeId].length, edgeId));

        cells[edgeId][cellId] = hasVehicle;
    }
}
