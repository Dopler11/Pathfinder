package ru.dopler.field;

import java.awt.*;

public class Cell {

    private Point parent;
    protected Color fillColor;

    protected boolean isClosed;
    protected boolean isOpened;

    public Cell () {
    }

    public Color getFillColor () {
        return fillColor;
    }

    public Point getParent () {
        return parent;
    }

    public void setParent (Point parent) {
        this.parent = parent;
    }

    public boolean isClosed () {
        return isClosed;
    }

    public void setClosed (boolean isClosed) {
        this.isClosed = isClosed;
    }

    public boolean isOpened () {
        return isOpened;
    }

    public void setOpened (boolean isOpened) {
        this.isOpened = isOpened;
    }
}
