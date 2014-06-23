package ru.dopler.cellField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CellField extends JPanel implements MouseListener, MouseMotionListener {

    private int width = 40;
    private int height = 25;

    private final int defaultStartX = 10;
    private final int defaultStartY = height / 2;

    private final int defaultEndX = width - 10;
    private final int defaultEndY = height / 2;

    private int cellSize = 20;

    private int mouseX;
    private int mouseY;

    private int lastI;
    private int lastJ;

    private Element draggedElement = null;

    private Element[][] grid = new Element[width][height];

    public CellField () {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                grid[i][j] = new EmptyCell();
            }
        }

        grid[defaultStartX][defaultStartY] = new StartPoint();
        grid[defaultEndX][defaultEndY] = new EndPoint();

        addMouseListener(this);
        addMouseMotionListener(this);

        initPainter();
    }

    private void initPainter () {
        ScheduledExecutorService painter = Executors.newSingleThreadScheduledExecutor();
        painter.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run () {
                repaint();
            }
        }, 0, 20, TimeUnit.MILLISECONDS);
    }

    @Override
    public void paint (Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        super.paint(g2d);

        g2d.drawString(String.format("Mouse: x = %s y = %s", mouseX, mouseY), 820, 50);

        Point point = getStartPoint();
        g2d.drawString(String.format("Start point: i = %s j = %s", (int) point.getX(), (int) point.getY()), 820, 70);

        point = getEndPoint();
        g2d.drawString(String.format("End point: i = %s j = %s", (int) point.getX(), (int) point.getY()), 820, 90);

        for (int i = 0; i < width; i++) {
            int x = i * cellSize;
            for (int j = 0; j < height; j++) {
                int y = j * cellSize;

                g2d.setColor(grid[i][j].getColor());
                g2d.fillRect(x, y, cellSize, cellSize);

                g2d.setColor(Color.gray);
                g2d.drawRect(x, y, cellSize, cellSize);
            }
        }
    }

    public Point getStartPoint () {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (grid[i][j] instanceof StartPoint) {
                    return new Point(i, j);
                }
            }
        }

        grid[defaultStartX][defaultStartY] = new StartPoint();
        return new Point(defaultStartX, defaultStartY);
    }

    public Point getEndPoint () {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (grid[i][j] instanceof EndPoint) {
                    return new Point(i, j);
                }
            }
        }

        grid[defaultEndX][defaultEndY] = new EndPoint();
        return new Point(defaultEndX, defaultEndY);
    }

    @Override
    public void mouseClicked (MouseEvent e) {
        int i = e.getX() / cellSize;
        int j = e.getY() / cellSize;
        if (e.getButton() == MouseEvent.BUTTON1) {
            grid[i][j] = new Wall();
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            grid[i][j] = new EmptyCell();
        }
    }

    @Override
    public void mousePressed (MouseEvent e) {
        int i = e.getX() / cellSize;
        int j = e.getY() / cellSize;
        if (grid[i][j] instanceof StartPoint || grid[i][j] instanceof EndPoint) {
            draggedElement = grid[i][j];
        } else {
            draggedElement = null;
        }
    }

    @Override
    public void mouseReleased (MouseEvent e) {
        draggedElement = null;
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
            if (draggedElement != null) {
                grid[i][j] = draggedElement;
                if (i != lastI || j != lastJ) {
                    grid[lastI][lastJ] = new EmptyCell();
                }

            } else {
                if (!(grid[i][j] instanceof StartPoint || grid[i][j] instanceof EndPoint)) {
                    grid[i][j] = new Wall();
                }
            }

        } else if (e.getModifiersEx() == MouseEvent.BUTTON3_DOWN_MASK) {
            if (draggedElement != null) {
                grid[i][j] = draggedElement;
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

        if (i >= width) {
            i = width - 1;
        }

        if (i < 0) {
            i = 0;
        }

        return i;
    }

    private int getColumnByMouseY (int y) {
        int j = y / cellSize;

        if (j >= height) {
            j = height - 1;
        }

        if (j < 0) {
            j = 0;
        }

        return j;
    }
}
