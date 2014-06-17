package ru.dopler.field;

import java.awt.*;

public class PassableBlock extends Block {

    public PassableBlock (int x, int y, int width, int height) {
        super(x, y, width, height);
        fillColor = Color.lightGray;
    }

}
