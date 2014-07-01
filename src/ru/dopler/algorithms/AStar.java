package ru.dopler.algorithms;

import ru.dopler.field.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AStar implements Algorithm {

    private static final int DIRECT_TRANSFER = 10;
    private static final int DIAGONAL_TRANSFER = 14;

    private int delayValue = 0;

    private volatile List<Point> openedCells;
    private volatile List<Point> closedCells;

    private int width;
    private int height;

    private int weight;

    private Point startCell;
    private Point endCell;

    private volatile Cell[][] field;

    private boolean isProcess = false;
    private boolean isPathFind = false;

    private List<Point> path;
    private int pathLength = 0;

    public AStar (int width, int height, int weight) {
        this.width = width;
        this.height = height;
        this.weight = weight;
        field = new Cell[width][height];
        path = new ArrayList<Point>();

        initField();
        initAlgorithm();
    }

    private void initField () {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                field[i][j] = new EmptyCell();
            }
        }

        startCell = new Point(1, height / 2);
        endCell = new Point(width - 2, height / 2);

        StartCell startFieldCell = new StartCell();
        startFieldCell.setG(0);
        startFieldCell.setH(countH(startCell));

        EndCell endFieldCell = new EndCell();
        endFieldCell.setG(countG(startCell));
        endFieldCell.setH(0);

        field[startCell.x][startCell.y] = startFieldCell;
        field[endCell.x][endCell.y] = endFieldCell;
    }

    private void initAlgorithm () {
        openedCells = new ArrayList<Point>();
        closedCells = new ArrayList<Point>();
    }

    @Override
    public void start () {
        isProcess = true;
        isPathFind = startAlgorithm();
        countPath();
        isProcess = false;
    }

    private void countPath () {
        Point currentCell = endCell;
        path.add(currentCell);

        while (!(field[currentCell.x][currentCell.y] instanceof StartCell)) {
            Point parentCell = field[currentCell.x][currentCell.y].getParent();

            if (currentCell.x == parentCell.x || currentCell.y == parentCell.y) {
                pathLength += DIRECT_TRANSFER;
            } else {
                pathLength += DIAGONAL_TRANSFER;
            }

            currentCell = parentCell;

            path.add(currentCell);
        }
    }

    private boolean startAlgorithm () {
        addToOpenedCells(startCell);
        while (true) {
            int selectedCellIndex = getNextCellIndex();
            Point selectedCell = openedCells.get(selectedCellIndex);

            openedCells.remove(selectedCellIndex);
            addToClosedCells(selectedCell);

            if (processAdjoiningCells(selectedCell)) {
                return true;
            }

            if (openedCells.isEmpty()) {
                return false;
            }
            delay();
        }
    }

    private boolean processAdjoiningCells (Point selectedCell) {
        java.util.List<Point> adjoiningCells = getAdjoiningCells(selectedCell);
        for (Point adjoiningCell : adjoiningCells) {
            if (field[adjoiningCell.x][adjoiningCell.y] instanceof EndCell) {
                field[adjoiningCell.x][adjoiningCell.y].setParent(selectedCell);
                return true;
            }
            countF(adjoiningCell);
            if (isCellOpened(adjoiningCell)) {
                int adjoiningCellG = ((EmptyCell) field[adjoiningCell.x][adjoiningCell.y]).getG();
                int selectedCellG = ((EmptyCell) field[selectedCell.x][selectedCell.y]).getG();

                int transferG;
                if (adjoiningCell.x == selectedCell.x || adjoiningCell.y == selectedCell.y) {
                    transferG = DIRECT_TRANSFER;
                } else {
                    transferG = DIAGONAL_TRANSFER;
                }

                int summaryG = selectedCellG + transferG;

                if (summaryG < adjoiningCellG) {
                    field[adjoiningCell.x][adjoiningCell.y].setParent(selectedCell);
                    countF(adjoiningCell);
                }
            } else {
                addToOpenedCells(adjoiningCell);
                countF(adjoiningCell);
            }
        }
        return false;
    }

    private int getNextCellIndex () {
        int selectedCellIndex = -1;
        double minF = Double.MAX_VALUE;

        for (int i = 0; i < openedCells.size(); i++) {
            Point currentOpenCell = openedCells.get(i);

            double localF = 0;
            if (field[currentOpenCell.x][currentOpenCell.y] instanceof EmptyCell) {
                localF = ((EmptyCell) field[currentOpenCell.x][currentOpenCell.y]).getF();
            }
            if (localF < minF) {
                minF = localF;
                selectedCellIndex = i;
            }
        }
        return selectedCellIndex;
    }

    private void addToOpenedCells (Point cell) {
        openedCells.add(cell);
    }

    private void addToClosedCells (Point cell) {
        closedCells.add(cell);
/*        if (field[cell.x][cell.y] instanceof EmptyCell) {
            Point endCell = getEndCell();
            int dx = endCell.x - cell.x;
            int dy = endCell.y - cell.y;
            float lenColor = (float) Math.sqrt(dx * dx + dy * dy);

            float alpha = 255 - (lenColor / (lenFromStartToEnd / 100)) * (255 / 100);
            alpha = alpha < 0 ? 0 : alpha;

            Color color = new Color(CLOSED_CELLS_COLOR.getRed(),
                    CLOSED_CELLS_COLOR.getGreen(),
                    CLOSED_CELLS_COLOR.getBlue(),
                    (int) alpha);

            field[cell.x][cell.y].setFillColor(color);
        }*/
    }

    private java.util.List<Point> getAdjoiningCells (Point parentCell) {
        java.util.List<Point> adjoiningCells = new ArrayList<Point>();

        java.util.List<Point> directOffsets = Arrays.asList(new Point(0, -1), new Point(-1, 0), new Point(1, 0), new Point(0, 1));
        java.util.List<Point> diagonalOffsets = Arrays.asList(new Point(-1, -1), new Point(1, -1), new Point(-1, 1), new Point(1, 1));

        for (Point offset : directOffsets) {
            addDirectCell(adjoiningCells, new Point(parentCell.x + offset.x, parentCell.y + offset.y), parentCell);
        }

        for (Point offset : diagonalOffsets) {
            addDiagonalCell(adjoiningCells, new Point(parentCell.x + offset.x, parentCell.y + offset.y), parentCell);
        }

        return adjoiningCells;
    }

    private void addDirectCell (java.util.List<Point> adjoiningCells, Point adjoiningCell, Point parentCell) {
        if (isCellOutOfField(adjoiningCell) || isCellWall(adjoiningCell) || isCellClosed(adjoiningCell) || isCellOpened(adjoiningCell)) {
            return;
        }
        adjoiningCells.add(adjoiningCell);
        field[adjoiningCell.x][adjoiningCell.y].setParent(parentCell);
    }

    private void addDiagonalCell (java.util.List<Point> adjoiningCells, Point adjoiningCell, Point parentCell) {
        if (isCellOutOfField(adjoiningCell) || isCellWall(adjoiningCell) || isCellClosed(adjoiningCell) || isCellOpened(adjoiningCell)) {
            return;
        }

        java.util.List<Point> checkCells = new ArrayList<Point>();
        int dx;
        int dy;
        if (adjoiningCell.x > parentCell.x) {
            dx = 1;
        } else {
            dx = -1;
        }
        if (adjoiningCell.y > parentCell.y) {
            dy = 1;
        } else {
            dy = -1;
        }

        checkCells.add(new Point(parentCell.x + dx, parentCell.y));
        checkCells.add(new Point(parentCell.x, parentCell.y + dy));

        int wallsCount = 0;
        for (Point checkCell : checkCells) {
            if (field[checkCell.x][checkCell.y] instanceof Wall) {
                wallsCount++;
            }
        }

        if (wallsCount > 0) {
            return;
        }

        adjoiningCells.add(adjoiningCell);
        field[adjoiningCell.x][adjoiningCell.y].setParent(parentCell);
    }

    private boolean isCellOutOfField (Point adjoiningCell) {
        return adjoiningCell.x < 0 || adjoiningCell.y < 0 || adjoiningCell.x >= width || adjoiningCell.y >= height;
    }

    private boolean isCellWall (Point adjoiningCell) {
        return field[adjoiningCell.x][adjoiningCell.y] instanceof Wall;
    }

    private boolean isCellClosed (Point cell) {
        for (Point closedCell : closedCells) {
            if (cell.equals(closedCell)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCellOpened (Point cell) {
        for (Point openedCell : openedCells) {
            if (cell.equals(openedCell)) {
                return true;
            }
        }
        return false;
    }

    private double countF (Point currentOpenCell) {
        if (field[currentOpenCell.x][currentOpenCell.y] instanceof EmptyCell) {
            int G = countG(currentOpenCell);
            double H = countH(currentOpenCell);
            ((EmptyCell) field[currentOpenCell.x][currentOpenCell.y]).setG(G);
            ((EmptyCell) field[currentOpenCell.x][currentOpenCell.y]).setH(H);

            return ((EmptyCell) field[currentOpenCell.x][currentOpenCell.y]).getF();
        } else {
            return 0;
        }
    }

    private int countG (Point currentCell) {
        Point localCell = currentCell;

        int G = 0;
        while (!localCell.equals(startCell)) {
            Point parentCell = field[localCell.x][localCell.y].getParent();

            if (localCell.x == parentCell.x || localCell.y == parentCell.y) {
                G += DIRECT_TRANSFER;
            } else {
                G += DIAGONAL_TRANSFER;
            }

            localCell = parentCell;
        }

        return G;
    }

    private double countH (Point currentCell) {
        // Манхетонское расстояние
        //int H = Math.abs(endCell.x - currentCell.x) + Math.abs(endCell.y - currentCell.y);

        // Евкилидово расстояние
        double H = Math.sqrt((endCell.x - currentCell.x) * (endCell.x - currentCell.x) + (endCell.y - currentCell.y) * (endCell.y - currentCell.y));
        return H * weight;
    }

    @Override
    public void reset () {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (field[i][j] instanceof EmptyCell) {
                    field[i][j] = new EmptyCell();
                }
            }
        }
        openedCells = new ArrayList<Point>();
        closedCells = new ArrayList<Point>();
        isProcess = false;
        isPathFind = false;
        path = new ArrayList<Point>();
        pathLength = 0;
    }

    @Override
    public Cell getCell (int i, int j) {
        return field[i][j];
    }

    @Override
    public void setCell (int i, int j, Cell cell) {
        field[i][j] = cell;
    }

    @Override
    public Point getStartCell () {
        return startCell;
    }

    @Override
    public void setStartCell (Point startCell) {
        this.field[this.startCell.x][this.startCell.y] = new EmptyCell();
        this.startCell = startCell;
        this.field[startCell.x][startCell.y] = new StartCell();
    }

    @Override
    public Point getEndCell () {
        return endCell;
    }

    @Override
    public void setEndCell (Point endCell) {
        this.field[this.endCell.x][this.endCell.y] = new EmptyCell();
        this.endCell = endCell;
        this.field[endCell.x][endCell.y] = new EndCell();
    }

    @Override
    public List<Point> getOpenedCells () {
        List<Point> lsOpened = new ArrayList<Point>();
        lsOpened.addAll(openedCells);
        return lsOpened;
    }

    @Override
    public List<Point> getClosedCells () {
        List<Point> lsClosed = new ArrayList<Point>();
        lsClosed.addAll(closedCells);
        return lsClosed;
    }

    @Override
    public boolean isProcess () {
        return isProcess;
    }

    @Override
    public boolean isPathFind () {
        return isPathFind;
    }

    @Override
    public int getPathLength () {
        return pathLength;
    }

    @Override
    public List<Point> getPath () {
        return path;
    }

    @Override
    public void setWeight (int weight) {
        this.weight = weight;
    }

    private void delay () {
        try {
            if (delayValue != 0) {
                Thread.sleep(delayValue);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setDelay (int delayValue) {
        this.delayValue = delayValue;
    }
}
