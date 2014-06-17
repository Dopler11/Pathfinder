package ru.dopler.field;

import java.awt.*;

public class WallBlock extends Block {

    public WallBlock (int x, int y, int width, int height) {
        super(x, y, width, height);
        fillColor = Color.gray;
    }
}
