package ru.dopler.cellField;

import java.awt.*;

public class Cell {

    private Point parent;
    protected Color fillColor;
    protected Color borderColor;

    public Color getFillColor () {
        return fillColor;
    }

    public void setFillColor (Color fillColor) {
        this.fillColor = fillColor;
    }

    public Color getBorderColor () {
        return borderColor;
    }

    public void setBorderColor (Color borderColor) {
        this.borderColor = borderColor;
    }

    public Point getParent () {
        return parent;
    }

    public void setParent (Point parent) {
        this.parent = parent;
    }
}
