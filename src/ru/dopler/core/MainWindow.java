package ru.dopler.core;

import ru.dopler.field.Field;

import javax.swing.*;

public class MainWindow extends JFrame {

    public MainWindow (int width, int height) {
        super("Pathfinder 0.0.1");
        setSize(width, height);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        add(new Field(width, height));
        setVisible(true);
    }
}
