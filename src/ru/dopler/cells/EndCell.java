package ru.dopler.cells;

import java.awt.*;

public class EndCell extends Cell {

    private int g = 0;
    private double h = 0;

    public EndCell () {
        fillColor = Color.blue;
    }

    public double getF () {
        return g + h;
    }

    public int getG () {
        return g;
    }

    public void setG (int g) {
        this.g = g;
    }

    public double getH () {
        return h;
    }

    public void setH (double h) {
        this.h = h;
    }
}
