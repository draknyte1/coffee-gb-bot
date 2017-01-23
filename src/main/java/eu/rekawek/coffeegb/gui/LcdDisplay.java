package eu.rekawek.coffeegb.gui;

import eu.rekawek.coffeegb.gpu.Display;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

public class LcdDisplay extends JPanel implements Display, Runnable {

    public static final int DISPLAY_WIDTH = 160;

    public static final int DISPLAY_HEIGHT = 144;

    private final BufferedImage img;

    private static final int[] COLORS = new int[]{0xe6f8da, 0x99c886, 0x437969, 0x051f2a};

    private final int[] rgb;

    private boolean enabled = true;

    private int scale;

    private boolean doStop;

    private boolean doRefresh;

    public LcdDisplay(int scale) {
        super();
        GraphicsConfiguration gfxConfig = GraphicsEnvironment.
                getLocalGraphicsEnvironment().getDefaultScreenDevice().
                getDefaultConfiguration();
        img = gfxConfig.createCompatibleImage(DISPLAY_WIDTH, DISPLAY_HEIGHT);
        rgb = new int[DISPLAY_WIDTH * DISPLAY_HEIGHT];
        this.scale = scale;
    }

    @Override
    public void setPixel(int x, int y, int color) {
        rgb[x + y * DISPLAY_WIDTH] = COLORS[color];
    }

    @Override
    public synchronized void requestRefresh() {
        doRefresh = true;
        notifyAll();
    }

    @Override
    public synchronized void waitForRefresh() {
        while (doRefresh) {
            try {
                wait(1);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    @Override
    public void enableLcd() {
        enabled = true;
    }

    @Override
    public void disableLcd() {
        enabled = false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();
        if (enabled) {
            g2d.drawImage(img, 0, 0, DISPLAY_WIDTH * scale, DISPLAY_HEIGHT * scale, null);
        } else {
            g2d.setColor(Color.BLACK);
            g2d.drawRect(0, 0, DISPLAY_WIDTH * scale, DISPLAY_HEIGHT * scale);
        }
        g2d.dispose();
    }

    @Override
    public void run() {
        while (!doStop) {
            synchronized (this) {
                try {
                    wait(1);
                } catch (InterruptedException e) {
                    break;
                }
            }

            if (doRefresh) {
                img.setRGB(0, 0, DISPLAY_WIDTH, DISPLAY_HEIGHT, rgb, 0, DISPLAY_WIDTH);
                validate();
                repaint();

                synchronized (this) {
                    doRefresh = false;
                    notifyAll();
                }
            }
        }
    }
}