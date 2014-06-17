package ru.dopler.field;

import java.awt.*;

public class Block {

    protected int x;
    protected int y;
    protected int width;
    protected int height;

    protected Color fillColor = Color.gray;
    protected Color borderColor = Color.darkGray;

    public Block (int x, int y, int width, int height) {
        this.x = x;
        this.y = y;

        this.width = width;
        this.height = height;
    }

    public int getX () {
        return x;
    }

    public int getY () {
        return y;
    }

    public int getWidth () {
        return width;
    }

    public int getHeight () {
        return height;
    }

    public Color getFillColor () {
        return fillColor;
    }

    public Color getBorderColor () {
        return borderColor;
    }
}
