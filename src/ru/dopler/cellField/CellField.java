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

    private static final boolean DELAY_FLAG = true;
    private static final int REFRESH_TIME_MS = 20;

    private static final int SIMPLE_TRANSFER = 10;
    private static final int DIAGONAL_TRANSFER = 14;

    private static final Color OPENED_CELLS_COLOR = Color.blue;
    private static final Color CLOSED_CELLS_COLOR = Color.red;

    private List<Point> openedCells;
    private List<Point> closedCells;

    private int width = 16;
    private int height = 13;

    private final Point defaultStartPoint = new Point(1, height / 2);
    private final Point defaultEndPoint = new Point(width - 2, height / 2);

    private int cellSize = 50;

    private int mouseX;
    private int mouseY;

    private int lastI;
    private int lastJ;

    private Cell draggedCell = null;

    private Cell[][] grid = new Cell[width][height];

    public CellField () {
        super();
        setLayout(null);

        addMouseListener(this);
        addMouseMotionListener(this);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased (KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                    new Thread(new Runnable() {
                        @Override
                        public void run () {
                            startPathfinder();
                        }
                    }).start();
                } else if (e.getKeyChar() == KeyEvent.VK_SPACE) {
                    initGrid();
                }
            }
        });

        setFocusable(true);
        requestFocus();

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
        drawCellsInfo(g2d);
    }

    private void drawGridInfo (Graphics2D g2d) {
        g2d.drawString(String.format("Mouse: x = %s y = %s", mouseX, mouseY), 840, 50);

        Point point = getStartCell();
        g2d.drawString(String.format("Start point: i = %s j = %s", point.x, point.y), 840, 70);

        point = getEndCell();
        g2d.drawString(String.format("End point: i = %s j = %s", point.x, point.y), 840, 90);
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
        if (paintParentCell != null) {
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

    private void startPathfinder () {
        try {
            Point startCell = getStartCell();
            Point endCell = getEndCell();

            Point selectedCell = startCell;
            int selectedCellIndex;

            Point lastOpenedCell;
            do {
                addToOpened(selectedCell);
                delay();

                selectedCellIndex = openedCells.size() - 1;

                addAllToOpened(getAdjoiningCells(selectedCell));
                openedCells.remove(selectedCellIndex);
                addToClosed(selectedCell);

                int minF = Integer.MAX_VALUE;
                for (int i = 0; i < openedCells.size(); i++) {
                    Point currentCell = openedCells.get(i);

                    int localF = countG(currentCell) + countH(currentCell);
                    ((EmptyCell) grid[currentCell.x][currentCell.y]).setF(localF);

                    if (localF < minF) {
                        minF = localF;
                        selectedCellIndex = i;
                        selectedCell = openedCells.get(selectedCellIndex);
                    }
                    delay();
                }

                openedCells.remove(selectedCellIndex);
                addToClosed(selectedCell);

                lastOpenedCell = openedCells.get(openedCells.size() - 1);

                delay();
            } while (lastOpenedCell.equals(endCell) || openedCells.isEmpty());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addAllToOpened (List<Point> cells) {
        for (Point cell : cells) {
            addToOpened(cell);
            delay();
        }
    }

    private void addToOpened (Point cell) {
        openedCells.add(cell);
        grid[cell.x][cell.y].setBorderColor(OPENED_CELLS_COLOR);
    }

    private void addToClosed (Point cell) {
        closedCells.add(cell);
        grid[cell.x][cell.y].setBorderColor(CLOSED_CELLS_COLOR);
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
        if (!(grid[adjoiningCell.x][adjoiningCell.y] instanceof Wall) && !isCellClosed(adjoiningCell)) {
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

        int H = Math.abs(endCell.x - currentCell.x) + Math.abs(endCell.y - currentCell.y);

        ((EmptyCell) grid[currentCell.x][currentCell.y]).setH(H);
        return H;
    }

    private void delay () {
        try {
            if (DELAY_FLAG) {
                Thread.sleep(30);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}