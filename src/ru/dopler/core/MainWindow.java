package ru.dopler.core;

import ru.dopler.field.Field;

import javax.swing.*;

public class MainWindow extends JFrame {

    public MainWindow (int width, int height) {
        super("Pathfinder 0.0.1");
        setSize(width, height);

    }

    public void exec () {
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        Field field = new Field(getWidth(), getHeight());
        add(field);

        setVisible(true);
    }
}