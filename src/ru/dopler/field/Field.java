package ru.dopler.field;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Field extends JPanel implements Runnable {

    private static final int REFRESH_TIME_MS = 1;
    private static final int BLOCK_WIDTH = 20;
    private static final int BLOCK_HEIGHT = 20;

    int width;
    int height;

    private List<List<Block>> blocks = new ArrayList<List<Block>>();

    public Field (int width, int height) {
        this.width = width;
        this.height = height;

        setBackground(Color.white);
        setLayout(null);
        setFocusable(true);
        requestFocus();
        addMouseListener(new FieldMouseListener());

        generateBlocks();
        initRepaintScheduler();
    }

    private void generateBlocks () {
        int linesNumber = height / BLOCK_HEIGHT;
        int lineBlocksNumber = width / BLOCK_WIDTH;

        for (int i = 0; i < linesNumber; i++) {
            List<Block> blocksRow = new ArrayList<Block>();
            for (int j = 0; j < lineBlocksNumber; j++) {
                Block block = new PassableBlock(j * BLOCK_WIDTH, i * BLOCK_WIDTH, BLOCK_WIDTH, BLOCK_HEIGHT);
                blocksRow.add(block);
            }
            blocks.add(blocksRow);
        }
    }

    private void initRepaintScheduler () {
        ScheduledExecutorService sceneUpdater = Executors.newSingleThreadScheduledExecutor();
        sceneUpdater.scheduleWithFixedDelay(this, 0, REFRESH_TIME_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void run () {
        repaint();
    }

    @Override
    public void paint (Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        super.paint(g2d);
        drawBlocks(g2d);
    }

    private void drawBlocks (Graphics2D g) {
        for (List<Block> blocksRow : blocks) {
            for (Block block : blocksRow) {
                g.setColor(block.getFillColor());
                g.fillRect(block.getX(), block.getY(), block.getWidth(), block.getHeight());

                g.setColor(block.getBorderColor());
                g.drawRect(block.getX(), block.getY(), block.getWidth(), block.getHeight());
            }
        }
    }

    public List<List<Block>> getBlocks () {
        return blocks;
    }

    public void setBlocks (List<List<Block>> blocks) {
        this.blocks = blocks;
    }

    public class FieldMouseListener implements MouseListener {

        @Override
        public void mouseClicked (MouseEvent e) {
            for (int i = 0; i < blocks.size(); i++) {
                for (int j = 0; j < blocks.get(i).size(); j++) {
                    Block block = blocks.get(i).get(j);
                    if (e.getX() > block.getX() && e.getX() < block.getX() + block.getWidth() && e.getY() > block.getY() && e.getY() < block.getY() + block.getHeight()) {
                        System.out.println(block.getX() + " " + block.getY());
                        switch (e.getButton()) {
                            case MouseEvent.BUTTON1:
                                System.out.println("button1");
                                block = new StartBlock(block.getX(), block.getY(), block.getWidth(), block.getHeight());
                                break;
                            case MouseEvent.BUTTON3:
                                System.out.println("button3");
                                block = new EndBlock(block.getX(), block.getY(), block.getWidth(), block.getHeight());
                                break;
                            case MouseEvent.BUTTON2:
                                System.out.println("button2");
                                block = new WallBlock(block.getX(), block.getY(), block.getWidth(), block.getHeight());
                                break;
                        }
                        List<Block> row = blocks.get(i);
                        row.set(j, block);
                        blocks.set(i, row);
                    }
                }
            }
        }

        @Override
        public void mousePressed (MouseEvent e) {

        }

        @Override
        public void mouseReleased (MouseEvent e) {

        }

        @Override
        public void mouseEntered (MouseEvent e) {

        }

        @Override
        public void mouseExited (MouseEvent e) {

        }
    }
}