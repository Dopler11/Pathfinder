package ru.dopler.cellField;

import java.awt.*;

public class EmptyCell extends Cell {

    private int f = 0;
    private int g = 0;
    private int h = 0;

    public EmptyCell () {
        fillColor = Color.white;
        borderColor = Color.gray;
    }

    public int getF () {
        return f;
    }

    public void setF (int f) {
        this.f = f;
    }

    public int getG () {
        return g;
    }

    public void setG (int g) {
        this.g = g;
    }

    public int getH () {
        return h;
    }

    public void setH (int h) {
        this.h = h;
    }


}
