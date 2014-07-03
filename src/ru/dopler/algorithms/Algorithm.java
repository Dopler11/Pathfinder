package ru.dopler.algorithms;

import ru.dopler.cells.Cell;

import java.awt.*;
import java.util.List;

public interface Algorithm {

    void start ();

    void reset ();

    Cell getCell (int i, int j);

    void setCell (int i, int j, Cell cell);

    Point getStartCell ();

    void setStartCell (Point startCell);

    Point getEndCell ();

    void setEndCell (Point endCell);

    List<Point> getOpenedCells ();

    List<Point> getClosedCells ();

    boolean isProcess ();

    boolean isPathFind ();

    int getPathLength ();

    List<Point> getPath ();

    void setWeight (int weight);

    void setDelay (int delayValue);
}
