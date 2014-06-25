package ru.dopler.cellField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CellField extends JPanel implements MouseListener, MouseMotionListener {

    private static final boolean DELAY_FLAG = false;
    private static final int REFRESH_TIME_MS = 20;

    private static final int SIMPLE_TRANSFER = 10;
    private static final int DIAGONAL_TRANSFER = 14;

    private static final Color OPENED_CELLS_COLOR = new Color(170, 245, 245);
    private static final Color CLOSED_CELLS_COLOR = new Color(170, 245, 175);

    private static final boolean DRAW_CELL_INFO_FLAG = false;

    private List<Point> openedCells;
    private List<Point> closedCells;

    private int width = 100;
    private int height = 60;

    private final Point defaultStartPoint = new Point(1, height / 2);
    private final Point defaultEndPoint = new Point(width - 2, height / 2);

    private int cellSize = 10;

    private int mouseX;
    private int mouseY;

    private int lastI;
    private int lastJ;

    private Cell draggedCell = null;

    private Cell[][] grid = new Cell[width][height];

    private boolean isPathFind = false;

    public CellField () {
        super();
        setLayout(null);
        setFocusable(true);
        requestFocus();

        addMouseListener(this);
        addMouseMotionListener(this);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased (KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                    new Thread(new Runnable() {
                        @Override
                        public void run () {
                            isPathFind = startPathfinder();
                        }
                    }).start();

                } else if (e.getKeyChar() == KeyEvent.VK_SPACE) {
                    isPathFind = false;
                    resetGrid();
                }
            }
        });

        initGrid();
        initPainter();
    }

    private void initGrid () {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                grid[i][j] = new EmptyCell();
            }
        }
        grid[defaultStartPoint.x][defaultStartPoint.y] = new StartCell();
        grid[defaultEndPoint.x][defaultEndPoint.y] = new EndCell();

        openedCells = new ArrayList<Point>();
        closedCells = new ArrayList<Point>();
    }

    private void resetGrid () {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (grid[i][j] instanceof EmptyCell) {
                    grid[i][j] = new EmptyCell();
                }
            }
        }
        openedCells = new ArrayList<Point>();
        closedCells = new ArrayList<Point>();
    }

    private void initPainter () {
        ScheduledExecutorService painter = Executors.newSingleThreadScheduledExecutor();
        painter.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run () {
                repaint();
            }
        }, 0, REFRESH_TIME_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void paint (Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        super.paint(g2d);

        drawGridInfo(g2d);
        drawCells(g2d);
        if (DRAW_CELL_INFO_FLAG) {
            drawCellsInfo(g2d);
        }
        if (isPathFind) {
            drawPath(g2d);
        }
    }

    private void drawGridInfo (Graphics2D g2d) {
        g2d.drawString(String.format("Mouse: x = %s y = %s", mouseX, mouseY), width * cellSize + 20, 50);

        Point point = getStartCell();
        g2d.drawString(String.format("Start point: i = %s j = %s", point.x, point.y), width * cellSize + 20, 70);

        point = getEndCell();
        g2d.drawString(String.format("End point: i = %s j = %s", point.x, point.y), width * cellSize + 20, 90);
    }

    private void drawCells (Graphics2D g2d) {
        for (int i = 0; i < width; i++) {
            int x = i * cellSize;
            for (int j = 0; j < height; j++) {
                int y = j * cellSize;

                g2d.setColor(grid[i][j].getFillColor());
                g2d.fillRect(x + 1, y + 1, cellSize - 2, cellSize - 2);

                g2d.setColor(grid[i][j].getBorderColor());
                g2d.drawRect(x + 1, y + 1, cellSize - 2, cellSize - 2);
            }
        }
    }

    private void drawCellsInfo (Graphics2D g2d) {
        Point paintCurrentCell = new Point();
        Point paintParentCell;
        for (int i = 0; i < width; i++) {
            paintCurrentCell.x = i * cellSize;
            for (int j = 0; j < height; j++) {
                paintCurrentCell.y = j * cellSize;
                paintParentCell = getPaintParentCell(i, j);

                drawCellText(i, j, paintCurrentCell, g2d);
                drawParentPointer(paintCurrentCell, paintParentCell, g2d);
            }
        }
    }

    private Point getPaintParentCell (int i, int j) {
        Point parent = grid[i][j].getParent();
        if (parent != null) {
            return new Point(parent.x * cellSize, parent.y * cellSize);
        } else {
            return null;
        }
    }

    private void drawCellText (int i, int j, Point paintCurrentCell, Graphics2D g2d) {
        if (grid[i][j] instanceof EmptyCell) {
            g2d.setColor(Color.black);
            g2d.drawString(String.valueOf(((EmptyCell) grid[i][j]).getF()), paintCurrentCell.x + 3, paintCurrentCell.y + 16);
            g2d.drawString(String.valueOf(((EmptyCell) grid[i][j]).getG()), paintCurrentCell.x + 3, paintCurrentCell.y + cellSize - 3);
            g2d.drawString(String.valueOf(((EmptyCell) grid[i][j]).getH()), paintCurrentCell.x + cellSize - 16, paintCurrentCell.y + cellSize - 3);
        }
    }

    private void drawParentPointer (Point paintCurrentCell, Point paintParentCell, Graphics2D g2d) {
        if (paintParentCell == null) {
            return;
        }
        g2d.setColor(Color.black);

        Point currentCellCenter = getCellCenter(paintCurrentCell);
        Point parentCellCenter = getCellCenter(paintParentCell);

        final int length = 7;

        Point p1 = getPointByLengthOnLine(currentCellCenter, parentCellCenter, length);
        Point p2 = getPointByLengthOnLine(currentCellCenter, parentCellCenter, -length);

        g2d.drawLine(p1.x, p1.y, p2.x, p2.y);

        p1 = getPointByLengthOnLine(currentCellCenter, parentCellCenter, -length - 3);

        g2d.drawOval(p1.x - 3, p1.y - 3, 6, 6);
    }

    private void drawPath (Graphics2D g2d) {
        g2d.setColor(Color.yellow);
        Point currentCell = getEndCell();
        while (!(grid[currentCell.x][currentCell.y] instanceof StartCell)) {
            Point parentCell = grid[currentCell.x][currentCell.y].getParent();
            Point paintCurrentCell = new Point(currentCell.x * cellSize, currentCell.y * cellSize);
            Point paintParentCell = getPaintParentCell(currentCell.x, currentCell.y);

            Point currentCenter = getCellCenter(paintCurrentCell);
            Point parentCenter = getCellCenter(paintParentCell);

            g2d.drawLine(currentCenter.x, currentCenter.y, parentCenter.x, parentCenter.y);

            currentCell = parentCell;
        }
    }

    private Point getCellCenter (Point cell) {
        return new Point(cell.x + cellSize / 2, cell.y + cellSize / 2);
    }

    public Point getStartCell () {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (grid[i][j] instanceof StartCell) {
                    return new Point(i, j);
                }
            }
        }

        draggedCell = null;
        grid[defaultStartPoint.x][defaultStartPoint.y] = new StartCell();
        return new Point(defaultStartPoint.x, defaultStartPoint.y);
    }

    public Point getEndCell () {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (grid[i][j] instanceof EndCell) {
                    return new Point(i, j);
                }
            }
        }

        draggedCell = null;
        grid[defaultEndPoint.x][defaultEndPoint.y] = new EndCell();
        return new Point(defaultEndPoint.x, defaultEndPoint.y);
    }

    private Point getPointByLengthOnLine (Point p1, Point p2, int length) {
        int x = (int) (p1.x + length * (p2.x - p1.x) /
                Math.sqrt((p2.x - p1.x) * (p2.x - p1.x) + (p2.y - p1.y) * (p2.y - p1.y)));
        int y = (int) (p1.y + length * (p2.y - p1.y) /
                Math.sqrt((p2.x - p1.x) * (p2.x - p1.x) + (p2.y - p1.y) * (p2.y - p1.y)));

        return new Point(x, y);
    }

    @Override
    public void mouseClicked (MouseEvent e) {
        int i = e.getX() / cellSize;
        int j = e.getY() / cellSize;
        if (e.getButton() == MouseEvent.BUTTON1 && !(grid[i][j] instanceof StartCell || grid[i][j] instanceof EndCell)) {
            grid[i][j] = new Wall();
        } else if (e.getButton() == MouseEvent.BUTTON3 && !(grid[i][j] instanceof StartCell || grid[i][j] instanceof EndCell)) {
            grid[i][j] = new EmptyCell();
        }
    }

    @Override
    public void mousePressed (MouseEvent e) {
        int i = e.getX() / cellSize;
        int j = e.getY() / cellSize;
        if (grid[i][j] instanceof StartCell || grid[i][j] instanceof EndCell) {
            draggedCell = grid[i][j];
        } else {
            draggedCell = null;
        }
    }

    @Override
    public void mouseReleased (MouseEvent e) {
        draggedCell = null;
    }

    @Override
    public void mouseEntered (MouseEvent e) {

    }

    @Override
    public void mouseExited (MouseEvent e) {

    }

    @Override
    public void mouseMoved (MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();

        lastI = getRowByMouseX(e.getX());
        lastJ = getColumnByMouseY(e.getY());
    }

    @Override
    public void mouseDragged (MouseEvent e) {
        int i = getRowByMouseX(e.getX());
        int j = getColumnByMouseY(e.getY());

        if (e.getModifiersEx() == MouseEvent.BUTTON1_DOWN_MASK) {
            if (draggedCell != null) {
                grid[i][j] = draggedCell;
                if (i != lastI || j != lastJ) {
                    grid[lastI][lastJ] = new EmptyCell();
                }

            } else {
                if (!(grid[i][j] instanceof StartCell || grid[i][j] instanceof EndCell)) {
                    grid[i][j] = new Wall();
                }
            }

        } else if (e.getModifiersEx() == MouseEvent.BUTTON3_DOWN_MASK) {
            if (draggedCell != null) {
                grid[i][j] = draggedCell;
                if (i != lastI || j != lastJ) {
                    grid[lastI][lastJ] = new EmptyCell();
                }
            } else {
                grid[i][j] = new EmptyCell();
            }
        }

        mouseMoved(e);
    }

    private int getRowByMouseX (int x) {
        int i = x / cellSize;
        i = (i >= width) ? width - 1 : i;
        i = (i < 0) ? 0 : i;

        return i;
    }

    private int getColumnByMouseY (int y) {
        int j = y / cellSize;
        j = (j >= height) ? height - 1 : j;
        j = (j < 0) ? 0 : j;

        return j;
    }

    private boolean startPathfinder () {
        addToOpenedCells(getStartCell());
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
        }
    }

    private boolean processAdjoiningCells (Point selectedCell) {
        List<Point> adjoiningCells = getAdjoiningCells(selectedCell);
        for (Point adjoiningCell : adjoiningCells) {
            if (grid[adjoiningCell.x][adjoiningCell.y] instanceof EndCell) {
                grid[adjoiningCell.x][adjoiningCell.y].setParent(selectedCell);
                return true;
            }
            countF(adjoiningCell);
            if (isCellOpened(adjoiningCell)) {
                int adjoiningCellG = ((EmptyCell) grid[adjoiningCell.x][adjoiningCell.y]).getG();
                int selectedCellG = ((EmptyCell) grid[selectedCell.x][selectedCell.y]).getG();

                int transferG;
                if (adjoiningCell.x == selectedCell.x || adjoiningCell.y == selectedCell.y) {
                    transferG = SIMPLE_TRANSFER;
                } else {
                    transferG = DIAGONAL_TRANSFER;
                }

                int summaryG = selectedCellG + transferG;

                if (summaryG < adjoiningCellG) {
                    grid[adjoiningCell.x][adjoiningCell.y].setParent(selectedCell);
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
        int minF = Integer.MAX_VALUE;

        for (int i = 0; i < openedCells.size(); i++) {
            Point currentOpenCell = openedCells.get(i);

            int localF = 0;
            if (grid[currentOpenCell.x][currentOpenCell.y] instanceof EmptyCell) {
                localF = ((EmptyCell) grid[currentOpenCell.x][currentOpenCell.y]).getF();
            }

            if (localF < minF) {
                minF = localF;
                selectedCellIndex = i;
            }
            delay();
        }
        return selectedCellIndex;
    }

    private void addToOpenedCells (Point cell) {
        openedCells.add(cell);
        if (grid[cell.x][cell.y] instanceof EmptyCell) {
            grid[cell.x][cell.y].setFillColor(OPENED_CELLS_COLOR);
        }
    }

    private void addToClosedCells (Point cell) {
        closedCells.add(cell);
        if (grid[cell.x][cell.y] instanceof EmptyCell) {
            grid[cell.x][cell.y].setFillColor(CLOSED_CELLS_COLOR);
        }
    }

    private List<Point> getAdjoiningCells (Point parentCell) {
        List<Point> adjoiningCells = new ArrayList<Point>();

        addAdjoiningCell(adjoiningCells, new Point(parentCell.x - 1, parentCell.y - 1), parentCell);
        addAdjoiningCell(adjoiningCells, new Point(parentCell.x, parentCell.y - 1), parentCell);
        addAdjoiningCell(adjoiningCells, new Point(parentCell.x + 1, parentCell.y - 1), parentCell);

        addAdjoiningCell(adjoiningCells, new Point(parentCell.x - 1, parentCell.y), parentCell);
        addAdjoiningCell(adjoiningCells, new Point(parentCell.x + 1, parentCell.y), parentCell);

        addAdjoiningCell(adjoiningCells, new Point(parentCell.x - 1, parentCell.y + 1), parentCell);
        addAdjoiningCell(adjoiningCells, new Point(parentCell.x, parentCell.y + 1), parentCell);
        addAdjoiningCell(adjoiningCells, new Point(parentCell.x + 1, parentCell.y + 1), parentCell);

        return adjoiningCells;
    }

    private void addAdjoiningCell (List<Point> adjoiningCells, Point adjoiningCell, Point parentCell) {
        if (adjoiningCell.x < 0 || adjoiningCell.y < 0 || adjoiningCell.x >= width || adjoiningCell.y >= height) {
            return;
        }

        if (!(grid[adjoiningCell.x][adjoiningCell.y] instanceof Wall) && !isCellClosed(adjoiningCell) && !isCellOpened(adjoiningCell)) {
            adjoiningCells.add(new Point(adjoiningCell.x, adjoiningCell.y));
            grid[adjoiningCell.x][adjoiningCell.y].setParent(parentCell);
        }
        delay();
    }

    private boolean isCellClosed (Point adjoiningCell) {
        for (Point closedCell : closedCells) {
            if (adjoiningCell.equals(closedCell)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCellOpened (Point adjoiningCell) {
        for (Point openedCell : openedCells) {
            if (adjoiningCell.equals(openedCell)) {
                return true;
            }
        }
        return false;
    }

    private int countF (Point currentOpenCell) {
        if (grid[currentOpenCell.x][currentOpenCell.y] instanceof EmptyCell) {
            int localF = countG(currentOpenCell) + countH(currentOpenCell);
            ((EmptyCell) grid[currentOpenCell.x][currentOpenCell.y]).setF(localF);
            return localF;
        } else {
            return 0;
        }
    }

    private int countG (Point currentCell) {
        Point startCell = getStartCell();
        Point localCell = currentCell;

        int G = 0;
        while (!localCell.equals(startCell)) {
            Point parentCell = grid[localCell.x][localCell.y].getParent();

            if (localCell.x == parentCell.x || localCell.y == parentCell.y) {
                G += SIMPLE_TRANSFER;
            } else {
                G += DIAGONAL_TRANSFER;
            }

            localCell = parentCell;
        }
        ((EmptyCell) grid[currentCell.x][currentCell.y]).setG(G);

        return G;
    }

    private int countH (Point currentCell) {
        Point endCell = getEndCell();

        //int H = Math.abs(endCell.x - currentCell.x) + Math.abs(endCell.y - currentCell.y);
        int H = (int) Math.sqrt((endCell.x - currentCell.x) * (endCell.x - currentCell.x) +
                (endCell.y - currentCell.y) * (endCell.y - currentCell.y));

        ((EmptyCell) grid[currentCell.x][currentCell.y]).setH(H);
        return H;
    }

    private void delay () {
        try {
            if (DELAY_FLAG) {
                Thread.sleep(20);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}