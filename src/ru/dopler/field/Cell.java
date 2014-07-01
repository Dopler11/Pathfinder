package ru.dopler.field;

import java.awt.*;

public class Cell {

    private Point parent;
    protected Color fillColor;
    protected Color borderColor;

    public Color getFillColor () {
        return fillColor;
    }

    public Color getBorderColor () {
        return borderColor;
    }

    public Point getParent () {
        return parent;
    }

    public void setParent (Point parent) {
        this.parent = parent;
    }
}
