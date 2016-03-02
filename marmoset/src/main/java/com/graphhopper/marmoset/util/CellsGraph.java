package com.graphhopper.marmoset.util;

import com.graphhopper.routing.util.AllEdgesIterator;
import com.graphhopper.storage.Graph;

/**
 * Created by alexander on 01/03/2016.
 */
public class CellsGraph {
    private final int cellSize;
    private Graph graph;
    private byte[][] cells;

    public CellsGraph(Graph graph, int cellSize) {
        this.cellSize = cellSize;
        this.graph = graph;
    }

    public void init()
    {
        AllEdgesIterator iterator = graph.getAllEdges();
        cells = new byte[iterator.getMaxId()][];
        while (iterator.next())
        {
            int cellCount = Math.max(1, (int) (iterator.getDistance() / cellSize));
            cells[iterator.getEdge()] = new byte[cellCount];
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
            if (cells[edgeId][cellId + move] == 0)
                move++;
            else
                return move-1;
        }

        return move-1;
    }

    public void set(int edgeId, int cellId, int v)
    {
        set(edgeId, cellId, (byte) v);
    }

    public void set(int edgeId, int cellId, byte v)
    {
        if (edgeId >= cells.length)
            throw new ArrayIndexOutOfBoundsException(
                    String.format("EdgeId '%d' out of bounds (max %d)", edgeId, cells.length));
        if (cellId >= cells[edgeId].length)
            throw new ArrayIndexOutOfBoundsException(
                    String.format("CellId '%d' out of bounds (max %d) for edge %d", cellId, cells[edgeId].length, edgeId));

        cells[edgeId][cellId] = v;
    }
}
