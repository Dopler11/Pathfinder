package ru.dopler.field;

import ru.dopler.algorithms.AStar;
import ru.dopler.algorithms.Algorithm;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Field extends JPanel implements MouseListener, MouseMotionListener {

    private static final Color OPENED_CELLS_COLOR = new Color(251, 255, 200);
    private static final Color CLOSED_CELLS_COLOR = new Color(170, 245, 175);

    private static final int REFRESH_TIME_MS = 1;

    private static final boolean DRAW_CELL_INFO_FLAG = true;

    private int mouseX;
    private int mouseY;

    private Algorithm alg;
    private int width = 20;
    private int height = 10;
    private int cellSize = 50;

    private Cell draggedCell = null;

    private JTextField dTextField = new JTextField("5");

    public Field () {
        super();

        alg = new AStar(width, height, Integer.valueOf(dTextField.getText()));

        setLayout(null);
        setFocusable(true);
        requestFocus();

        addMouseListener(this);
        addMouseMotionListener(this);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased (KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                    startSolution();

                } else if (e.getKeyChar() == KeyEvent.VK_SPACE) {
                    alg.reset();
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
                    alg.reset();
                    grabFocus();
                }
            }
        });
        add(dTextField);

        initPainter();
    }

    private void startSolution () {
        alg.reset();
        alg.setWeight(Integer.valueOf(dTextField.getText()));
        new Thread(new Runnable() {
            @Override
            public void run () {
                alg.start();
            }
        }).start();
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
        if (alg.isPathFind()) {
            drawPath(g2d);
        }
    }

    private void drawGridInfo (Graphics2D g2d) {
        int gridInfoX = width * cellSize + 20;

        g2d.drawString(String.format("Mouse: x = %s y = %s", mouseX, mouseY), gridInfoX, 50);

        Point point = alg.getStartCell();
        g2d.drawString(String.format("Start point: i = %s j = %s", point.x, point.y), gridInfoX, 70);

        point = alg.getEndCell();
        g2d.drawString(String.format("End point: i = %s j = %s", point.x, point.y), gridInfoX, 90);

        g2d.drawString(String.format("Path length = %s", alg.getPathLength()), gridInfoX, 110);
    }

    private void drawCells (Graphics2D g2d) {
        for (int i = 0; i < width; i++) {
            int paintX = i * cellSize;
            for (int j = 0; j < height; j++) {
                int paintY = j * cellSize;

                if (isCellOpened(new Point(i, j)) && !(alg.getCell(i, j) instanceof StartCell)) {
                    g2d.setColor(OPENED_CELLS_COLOR);
                    g2d.fillRect(paintX, paintY, cellSize, cellSize);
                    g2d.setColor(alg.getCell(i, j).getBorderColor());
                    g2d.drawRect(paintX, paintY, cellSize, cellSize);

                } else if (isCellClosed(new Point(i, j)) && !(alg.getCell(i, j) instanceof StartCell)) {
                    g2d.setColor(CLOSED_CELLS_COLOR);
                    g2d.fillRect(paintX, paintY, cellSize, cellSize);
                    g2d.setColor(alg.getCell(i, j).getBorderColor());
                    g2d.drawRect(paintX, paintY, cellSize, cellSize);

                } else {
                    g2d.setColor(alg.getCell(i, j).getFillColor());
                    g2d.fillRect(paintX, paintY, cellSize, cellSize);
                    g2d.setColor(alg.getCell(i, j).getBorderColor());
                    g2d.drawRect(paintX, paintY, cellSize, cellSize);
                }
            }
        }
    }

    private boolean isCellOpened (Point cell) {
        List<Point> openedCells = alg.getOpenedCells();
        for (Point openedCell : openedCells) {
            if (cell.equals(openedCell)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCellClosed (Point cell) {
        List<Point> closedCells = alg.getClosedCells();
        for (Point closedCell : closedCells) {
            if (cell.equals(closedCell)) {
                return true;
            }
        }
        return false;
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
        Point parent = alg.getCell(i, j).getParent();
        if (parent != null) {
            return new Point(parent.x * cellSize, parent.y * cellSize);
        } else {
            return null;
        }
    }

    private void drawCellText (int i, int j, Point paintCurrentCell, Graphics2D g2d) {
        if (alg.getCell(i, j) instanceof EmptyCell) {
            g2d.setColor(Color.black);
            int G = ((EmptyCell) alg.getCell(i, j)).getG();
            double H = ((EmptyCell) alg.getCell(i, j)).getH();
            double F = ((EmptyCell) alg.getCell(i, j)).getF();

            g2d.setFont(new Font("Arial", 0, 10));
            g2d.drawString(String.valueOf(Math.round(F)), paintCurrentCell.x + 3, paintCurrentCell.y + 11);
            g2d.drawString(String.valueOf(G), paintCurrentCell.x + 3, paintCurrentCell.y + cellSize - 3);
            g2d.drawString(String.valueOf(Math.round(H)), paintCurrentCell.x + cellSize - 16, paintCurrentCell.y + cellSize - 3);
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
        g2d.setColor(Color.orange);
        g2d.setStroke(new BasicStroke(4));
        List<Point> path = alg.getPath();

        for (Point pathCell : path) {
            Point paintCurrentCell = new Point(pathCell.x * cellSize, pathCell.y * cellSize);
            Point paintParentCell = getPaintParentCell(pathCell.x, pathCell.y);
            if (paintParentCell == null) {
                continue;
            }

            Point currentCenter = getCellCenter(paintCurrentCell);
            Point parentCenter = getCellCenter(paintParentCell);

            g2d.drawLine(currentCenter.x, currentCenter.y, parentCenter.x, parentCenter.y);
        }
    }

    private Point getCellCenter (Point cell) {
        return new Point(cell.x + cellSize / 2, cell.y + cellSize / 2);
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
        if (alg.isProcess()) {
            return;
        }

        alg.reset();

        int i = getRowByMouseX(e.getX());
        int j = getColumnByMouseY(e.getY());

        if (alg.getCell(i, j) instanceof StartCell) {
            draggedCell = new StartCell();

        } else if (alg.getCell(i, j) instanceof EndCell) {
            draggedCell = new EndCell();

        } else {
            draggedCell = null;
        }
    }

    @Override
    public void mouseReleased (MouseEvent e) {
        if (alg.isProcess()) {
            return;
        }
        int i = getRowByMouseX(e.getX());
        int j = getColumnByMouseY(e.getY());

        draggedCell = null;
        alg.reset();

        if (e.getButton() == MouseEvent.BUTTON1 && alg.getCell(i, j) instanceof EmptyCell) {
            alg.setCell(i, j, new Wall());

        } else if (e.getButton() == MouseEvent.BUTTON3 && alg.getCell(i, j) instanceof Wall) {
            alg.setCell(i, j, new EmptyCell());
        }
    }

    @Override
    public void mouseEntered (MouseEvent e) {

    }

    @Override
    public void mouseExited (MouseEvent e) {

    }

    @Override
    public void mouseDragged (MouseEvent e) {
        if (alg.isProcess()) {
            return;
        }
        int i = getRowByMouseX(e.getX());
        int j = getColumnByMouseY(e.getY());

        if (e.getModifiersEx() == MouseEvent.BUTTON1_DOWN_MASK) {
            if (draggedCell != null) {
                dragCell(i, j);

            } else {
                if (alg.getCell(i, j) instanceof EmptyCell) {
                    alg.setCell(i, j, new Wall());
                }
            }

        } else if (e.getModifiersEx() == MouseEvent.BUTTON3_DOWN_MASK) {
            if (draggedCell != null) {
                dragCell(i, j);

            } else {
                if (alg.getCell(i, j) instanceof Wall) {
                    alg.setCell(i, j, new EmptyCell());
                }
            }
        }

        mouseMoved(e);
    }

    private void dragCell (int i, int j) {
        if (!(alg.getCell(i, j) instanceof EmptyCell)) {
            return;
        }

        if (draggedCell instanceof StartCell) {
            alg.setStartCell(new Point(i, j));

        } else if (draggedCell instanceof EndCell) {
            alg.setEndCell(new Point(i, j));
        }
    }

    @Override
    public void mouseMoved (MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
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
}