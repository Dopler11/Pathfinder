package ru.dopler.cellField;

import java.awt.*;

public class EmptyCell extends Cell {

    private double f = 0;
    private int g = 0;
    private double h = 0;

    public EmptyCell () {
        fillColor = Color.white;
        borderColor = Color.gray;
    }

    public double getF () {
        return f;
    }

    public void setF (double f) {
        this.f = f;
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
