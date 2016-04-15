package com.graphhopper.marmoset.util;

import com.graphhopper.routing.util.AllEdgesIterator;
import com.graphhopper.storage.Graph;
import com.graphhopper.util.EdgeIteratorState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by alexander on 01/03/2016.
 */
public class CellGraph {

    public final float cellSize;
    private Graph graph;
    private boolean[][] cells;
    private boolean[][] reverseCells;

    private static final Logger logger = LoggerFactory.getLogger(CellGraph.class);

    public CellGraph(Graph graph, float cellSize) {
        this.cellSize = cellSize;
        this.graph = graph;
        logger.info("slowest speed: " + 1 * cellSize * 3.6 * 0.62 + "mph");
        logger.info("fastest speed: " + 7 * cellSize * 3.6 * 0.62 + "mph");
    }

    public void init()
    {
        AllEdgesIterator iterator = graph.getAllEdges();
        cells = new boolean[iterator.getMaxId()][];
        reverseCells = new boolean[iterator.getMaxId()][];
        while (iterator.next())
        {
            int cellCount = Math.max(1, (int) (iterator.getDistance() / cellSize));

            cells[iterator.getEdge()] = new boolean[cellCount];
            reverseCells[iterator.getEdge()] = new boolean[cellCount];
        }
    }

    public int getCellCount(EdgeIteratorState edge)
    {
        int edgeId = edge.getEdge();
        boolean[][] currCells = getCells(edge);
        if (edgeId >= currCells.length)
            throw new ArrayIndexOutOfBoundsException(
                    String.format("EdgeId '%d' out of bounds (max %d)", edgeId, currCells.length));
        return currCells[edgeId].length;
    }

    public void set(EdgeIteratorState edge, int cellId, boolean hasVehicle)
    {
        boolean[][] currCells = getCells(edge);
        int edgeId = edge.getEdge();
        if (edgeId >= currCells.length)
            throw new ArrayIndexOutOfBoundsException(
                    String.format("EdgeId '%d' out of bounds (max %d)", edgeId, currCells.length));
        if (cellId >= currCells[edgeId].length)
            throw new ArrayIndexOutOfBoundsException(
                    String.format("CellId '%d' out of bounds (max %d) for edge %d", cellId, currCells[edgeId].length, edgeId));

        currCells[edgeId][cellId] = hasVehicle;
    }

    public boolean get(EdgeIteratorState edge, int cellId)
    {
        boolean[][] currCells = getCells(edge);
        int edgeId = edge.getEdge();
        if (edgeId >= currCells.length)
            throw new ArrayIndexOutOfBoundsException(
                    String.format("EdgeId '%d' out of bounds (max %d)", edgeId, currCells.length));
        if (cellId >= currCells[edgeId].length)
            throw new ArrayIndexOutOfBoundsException(
                    String.format("CellId '%d' out of bounds (max %d) for edge %d", cellId, currCells[edgeId].length, edgeId));
        return currCells[edgeId][cellId];
    }

    private boolean[][] getCells(EdgeIteratorState edge)
    {
        if (edge.getBaseNode() < edge.getAdjNode())
            return cells;

        return reverseCells;
    }
}
