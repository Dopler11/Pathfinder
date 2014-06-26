package ru.dopler.cellField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CellField extends JPanel implements MouseListener, MouseMotionListener {

    private static final boolean DELAY_FLAG = false;
    private static final int REFRESH_TIME_MS = 1;

    private static final int DIRECT_TRANSFER = 10;
    private static final int DIAGONAL_TRANSFER = 14;

    private static final Color OPENED_CELLS_COLOR = new Color(170, 245, 245);
    private static final Color CLOSED_CELLS_COLOR = new Color(170, 245, 175);

    private static final boolean DRAW_CELL_INFO_FLAG = false;
    private int D = 1;

    private List<Point> openedCells;
    private List<Point> closedCells;

    private int width = 70;
    private int height = 50;

    private final Point defaultStartPoint = new Point(1, height / 2);
    private final Point defaultEndPoint = new Point(width - 2, height / 2);

    private int cellSize = 20;

    private int mouseX;
    private int mouseY;

    private int lastI;
    private int lastJ;

    private Cell draggedCell = null;

    private Cell[][] field = new Cell[width][height];

    private boolean isPathFind = false;
    private float pathLength;

    float lenFromStartToEnd;

    private JTextField dTextField = new JTextField("5");

    private Thread solutionThread;

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
                    resetSolution();
                    initGrid();
                    startSolution();

                } else if (e.getKeyChar() == KeyEvent.VK_SPACE) {
                    resetSolution();
                }
            }
        });

        int textX = width * cellSize + 20;
        JLabel dLabel = new JLabel("Weight:");
        dLabel.setLocation(textX, 130);
        dLabel.setSize(45, 20);
        add(dLabel);

        dTextField.setLocation(textX + dLabel.getWidth() + 5, 130);
        dTextField.setSize(60, 20);
        dTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased (KeyEvent e) {

                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    startSolution();
                    grabFocus();

                } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    dTextField.setText(dTextField.getText().replace(" ", ""));
                    resetSolution();
                    grabFocus();
                    //@fmt:off
                } else if (e.getKeyCode() >= KeyEvent.VK_0 && e.getKeyCode() <= KeyEvent.VK_9
                        || e.getKeyCode() >= KeyEvent.VK_NUMPAD0 && e.getKeyCode() <= KeyEvent.VK_NUMPAD9) {
                    startSolution();
                }
                //@fmt:on
            }
        });
        add(dTextField);

        initGrid();
        initPainter();

        startSolution();
    }

    private void initGrid () {
        Random rnd = new Random();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (rnd.nextInt(100) >= 30) {
                    field[i][j] = new EmptyCell();
                } else {
                    field[i][j] = new Wall();
                }
            }
        }
        field[rnd.nextInt(width)][rnd.nextInt(height)] = new StartCell();
        field[rnd.nextInt(width)][rnd.nextInt(height)] = new EndCell();

        openedCells = new ArrayList<Point>();
        closedCells = new ArrayList<Point>();
    }

    private void startSolution () {
        solutionThread = new Thread(new Runnable() {
            @Override
            public void run () {
                resetSolution();
                String dText = dTextField.getText();
                if (dText != null && !dText.isEmpty()) {
                    D = Integer.valueOf(dText);
                } else {
                    D = 1;
                    dTextField.setText(String.valueOf(D));
                }

                isPathFind = startAStar();
                if (isPathFind) {
                    countPathLength();
                }
            }
        });

        solutionThread.start();
    }

    private void countPathLength () {
        Point currentCell = getEndCell();
        while (!(field[currentCell.x][currentCell.y] instanceof StartCell)) {
            Point parentCell = field[currentCell.x][currentCell.y].getParent();
            Point paintCurrentCell = new Point(currentCell.x * cellSize, currentCell.y * cellSize);
            Point paintParentCell = getPaintParentCell(currentCell.x, currentCell.y);

            Point currentCenter = getCellCenter(paintCurrentCell);
            Point parentCenter = getCellCenter(paintParentCell);

            float dx = currentCenter.x - parentCenter.x;
            float dy = currentCenter.y - parentCenter.y;
            pathLength += Math.sqrt(dx * dx + dy * dy);

            currentCell = parentCell;
        }
    }

    private void resetSolution () {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (field[i][j] instanceof EmptyCell) {
                    field[i][j] = new EmptyCell();
                }
            }
        }
        openedCells = new ArrayList<Point>();
        closedCells = new ArrayList<Point>();
        isPathFind = false;
        pathLength = 0;
    }

    private void initPainter () {
        ScheduledExecutorService painter = Executors.newSingleThreadScheduledExecutor();
        painter.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run () {
                validate();
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
        int gridInfoX = width * cellSize + 20;

        g2d.drawString(String.format("Mouse: x = %s y = %s", mouseX, mouseY), gridInfoX, 50);

        Point point = getStartCell();
        g2d.drawString(String.format("Start point: i = %s j = %s", point.x, point.y), gridInfoX, 70);

        point = getEndCell();
        g2d.drawString(String.format("End point: i = %s j = %s", point.x, point.y), gridInfoX, 90);

        g2d.drawString(String.format("Path length = %s", pathLength), gridInfoX, 110);
    }

    private void drawCells (Graphics2D g2d) {
        for (int i = 0; i < width; i++) {
            int x = i * cellSize;
            for (int j = 0; j < height; j++) {
                int y = j * cellSize;

                g2d.setColor(field[i][j].getFillColor());
                g2d.fillRect(x, y, cellSize, cellSize);

                g2d.setColor(field[i][j].getBorderColor());
                g2d.drawRect(x, y, cellSize, cellSize);
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
        Point parent = field[i][j].getParent();
        if (parent != null) {
            return new Point(parent.x * cellSize, parent.y * cellSize);
        } else {
            return null;
        }
    }

    private void drawCellText (int i, int j, Point paintCurrentCell, Graphics2D g2d) {
        if (field[i][j] instanceof EmptyCell) {
            g2d.setColor(Color.black);
            g2d.drawString(String.valueOf(((EmptyCell) field[i][j]).getF()), paintCurrentCell.x + 3, paintCurrentCell.y + 16);
            g2d.drawString(String.valueOf(((EmptyCell) field[i][j]).getG()), paintCurrentCell.x + 3, paintCurrentCell.y + cellSize - 3);
            g2d.drawString(String.valueOf(((EmptyCell) field[i][j]).getH()), paintCurrentCell.x + cellSize - 16, paintCurrentCell.y + cellSize - 3);
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
        g2d.setColor(Color.darkGray);
        Point currentCell = getEndCell();
        while (!(field[currentCell.x][currentCell.y] instanceof StartCell)) {
            Point parentCell = field[currentCell.x][currentCell.y].getParent();
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
                if (field[i][j] instanceof StartCell) {
                    return new Point(i, j);
                }
            }
        }

        draggedCell = null;
        field[defaultStartPoint.x][defaultStartPoint.y] = new StartCell();
        return new Point(defaultStartPoint.x, defaultStartPoint.y);
    }

    public Point getEndCell () {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (field[i][j] instanceof EndCell) {
                    return new Point(i, j);
                }
            }
        }

        draggedCell = null;
        field[defaultEndPoint.x][defaultEndPoint.y] = new EndCell();
        return new Point(defaultEndPoint.x, defaultEndPoint.y);
    }

    private Point getPointByLengthOnLine (Point p1, Point p2, int length) {
        int x = (int) (p1.x + length * (p2.x - p1.x) / Math.sqrt((p2.x - p1.x) * (p2.x - p1.x) + (p2.y - p1.y) * (p2.y - p1.y)));
        int y = (int) (p1.y + length * (p2.y - p1.y) / Math.sqrt((p2.x - p1.x) * (p2.x - p1.x) + (p2.y - p1.y) * (p2.y - p1.y)));

        return new Point(x, y);
    }

    @Override
    public void mouseClicked (MouseEvent e) {

    }

    @Override
    public void mousePressed (MouseEvent e) {
        int i = getRowByMouseX(e.getX());
        int j = getColumnByMouseY(e.getY());

        if (field[i][j] instanceof StartCell || field[i][j] instanceof EndCell) {
            draggedCell = field[i][j];
        } else {
            draggedCell = null;
        }
    }

    @Override
    public void mouseReleased (MouseEvent e) {
        resetSolution();
        draggedCell = null;

        int i = getRowByMouseX(e.getX());
        int j = getColumnByMouseY(e.getY());

        if (e.getButton() == MouseEvent.BUTTON1 && !(field[i][j] instanceof StartCell || field[i][j] instanceof EndCell)) {
            field[i][j] = new Wall();
        } else if (e.getButton() == MouseEvent.BUTTON3 && !(field[i][j] instanceof StartCell || field[i][j] instanceof EndCell)) {
            field[i][j] = new EmptyCell();
        }

        startSolution();
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
                if (field[i][j] instanceof Wall) {
                    return;
                }

                field[i][j] = draggedCell;
                if (i != lastI || j != lastJ) {
                    field[lastI][lastJ] = new EmptyCell();
                }

            } else {
                if (!(field[i][j] instanceof StartCell || field[i][j] instanceof EndCell)) {
                    field[i][j] = new Wall();
                }
            }

        } else if (e.getModifiersEx() == MouseEvent.BUTTON3_DOWN_MASK) {
            if (draggedCell != null) {
                if (field[i][j] instanceof Wall) {
                    return;
                }

                field[i][j] = draggedCell;
                if (i != lastI || j != lastJ) {
                    field[lastI][lastJ] = new EmptyCell();
                }
            } else {
                field[i][j] = new EmptyCell();
            }
        }

        mouseMoved(e);
        startSolution();
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

    private boolean startAStar () {
        Point endCell = getEndCell();
        Point startCell = getStartCell();
        int dx = Math.abs(endCell.x - startCell.x);
        int dy = Math.abs(endCell.y - startCell.y);
        lenFromStartToEnd = (float) Math.sqrt(dx * dx + dy * dy);

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
            delay();
        }
    }

    private boolean processAdjoiningCells (Point selectedCell) {
        List<Point> adjoiningCells = getAdjoiningCells(selectedCell);
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
        if (field[cell.x][cell.y] instanceof EmptyCell) {
            field[cell.x][cell.y].setFillColor(OPENED_CELLS_COLOR);
        }
    }

    private void addToClosedCells (Point cell) {
        closedCells.add(cell);
        if (field[cell.x][cell.y] instanceof EmptyCell) {
            Point endCell = getEndCell();
            int dx = endCell.x - cell.x;
            int dy = endCell.y - cell.y;
            float lenColor = (float) Math.sqrt(dx * dx + dy * dy);

            float alpha = 255 - (lenColor / (lenFromStartToEnd / 100)) * (255 / 100);
            alpha = alpha < 0 ? 0 : alpha;

            Color color = new Color(10, 150, 40, (int) alpha);

            field[cell.x][cell.y].setFillColor(color);
        }
    }

    private List<Point> getAdjoiningCells (Point parentCell) {
        List<Point> adjoiningCells = new ArrayList<Point>();

        List<Point> directOffsets = Arrays.asList(new Point(0, -1), new Point(-1, 0), new Point(1, 0), new Point(0, 1));
        List<Point> diagonalOffsets = Arrays.asList(new Point(-1, -1), new Point(1, -1), new Point(-1, 1), new Point(1, 1));

        for (Point offset : directOffsets) {
            addDirectCell(adjoiningCells, new Point(parentCell.x + offset.x, parentCell.y + offset.y), parentCell);
        }

        for (Point offset : diagonalOffsets) {
            addDiagonalCell(adjoiningCells, new Point(parentCell.x + offset.x, parentCell.y + offset.y), parentCell);
        }

        return adjoiningCells;
    }

    private void addDirectCell (List<Point> adjoiningCells, Point adjoiningCell, Point parentCell) {
        if (isCellOutOfField(adjoiningCell) || isCellWall(adjoiningCell) || isCellClosed(adjoiningCell) || isCellOpened(adjoiningCell)) {
            return;
        }
        adjoiningCells.add(adjoiningCell);
        field[adjoiningCell.x][adjoiningCell.y].setParent(parentCell);
    }

    private void addDiagonalCell (List<Point> adjoiningCells, Point adjoiningCell, Point parentCell) {
        if (isCellOutOfField(adjoiningCell) || isCellWall(adjoiningCell) || isCellClosed(adjoiningCell) || isCellOpened(adjoiningCell)) {
            return;
        }

        List<Point> checkCells = new ArrayList<Point>();
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

    private double countF (Point currentOpenCell) {
        if (field[currentOpenCell.x][currentOpenCell.y] instanceof EmptyCell) {
            double localF = countG(currentOpenCell) + countH(currentOpenCell);
            ((EmptyCell) field[currentOpenCell.x][currentOpenCell.y]).setF(localF);
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
            Point parentCell = field[localCell.x][localCell.y].getParent();

            if (localCell.x == parentCell.x || localCell.y == parentCell.y) {
                G += DIRECT_TRANSFER;
            } else {
                G += DIAGONAL_TRANSFER;
            }

            localCell = parentCell;
        }
        ((EmptyCell) field[currentCell.x][currentCell.y]).setG(G);

        return G;
    }

    private double countH (Point currentCell) {
        Point endCell = getEndCell();

        // Манхетонское расстояние
        //int H = Math.abs(endCell.x - currentCell.x) + Math.abs(endCell.y - currentCell.y);

        // Евкилидово расстояние
        double H = Math.sqrt((endCell.x - currentCell.x) * (endCell.x - currentCell.x) + (endCell.y - currentCell.y) * (endCell.y - currentCell.y));

        ((EmptyCell) field[currentCell.x][currentCell.y]).setH(H);
        return H * D;
    }

    private void delay () {
        try {
            if (DELAY_FLAG) {
                Thread.sleep(1);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}