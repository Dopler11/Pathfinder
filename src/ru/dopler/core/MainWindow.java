package ru.dopler.core;

import ru.dopler.painter.FieldPainter;

import javax.swing.*;

public class MainWindow extends JFrame {

    public MainWindow (int width, int height) {
        super("Pathfinder 0.0.1");
        setSize(width, height);

    }

    public void exec () {
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        FieldPainter fieldPainter = new FieldPainter(getWidth(), getHeight());
        add(fieldPainter);

        setVisible(true);
    }
}