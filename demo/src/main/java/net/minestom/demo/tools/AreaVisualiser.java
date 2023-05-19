package net.minestom.demo.tools;

import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

class AreaVisualiser {

    private static final int width = 800;

    private static double zoom = 1;

    public static void main(String[] args) {

        Area larger = Area.union(
                Area.fill(new Vec(-304, -64, -304), new Vec(0, 320, 304)),
                Area.fill(new Vec(0, -64, -304), new Vec(304, 320, 304))
        );

        Area smaller = Area.union(
                Area.fill(new Vec(-128, -64, -128), new Vec(0, 320, 128)),
                Area.fill(new Vec(0, -64, -128), new Vec(128, 320, 128))
        );

        Area overlappedA = larger.overlap(smaller);

        window(graphics -> {
            graphics.setColor(new Color(0x40C67432, true));
            drawArea(graphics, larger);
            graphics.setColor(new Color(0x4059879E, true));
            drawArea(graphics, smaller);
            graphics.setColor(new Color(0x403D8D56, true));
            drawArea(graphics, overlappedA);
        });
    }

    public static void drawArea(Graphics graphics, Area area) {
        Color color = graphics.getColor();

        for (Area fill : area.subdivide()) {
            graphics.setColor(color);
            net.minestom.server.coordinate.Point min = fill.min();
            Point max = fill.max();
            double widthd2 = width / 2.0;
            graphics.fillRect(
                    (int) (widthd2 + min.x() / zoom),
                    (int) (widthd2 + min.z() / zoom),
                    (int) ((max.x() - min.x()) / zoom),
                    (int) ((max.z() - min.z()) / zoom)
            );
            graphics.setColor(Color.WHITE);
            graphics.drawRect(
                    (int) (widthd2 + min.x() / zoom),
                    (int) (widthd2 + min.z() / zoom),
                    (int) ((max.x() - min.x()) / zoom),
                    (int) ((max.z() - min.z()) / zoom)
            );
        }

        graphics.setColor(color);
    }

    public static JFrame window(Consumer<Graphics> paint) {
        // Launch a JFrame that draws a square
        JFrame window = new JFrame() {
            @Override
            public void paint(Graphics graphics) {
                super.paint(graphics);
                graphics.setColor(new Color(0x2B2B2C));
                graphics.fillRect(0, 0, width, width);
                paint.accept(graphics);
            }
        };

        window.pack();
        window.setSize(width, width);
        window.setLocationRelativeTo(null);
        window.setVisible(true);

        window.setFocusable(true);
        window.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if (evt.getButton() == MouseEvent.BUTTON3)
                    window.dispose();
            }
        });
        window.addMouseWheelListener(evt -> {
            zoom = Math.max(1.0, zoom + evt.getPreciseWheelRotation());
            window.repaint();
        });

        return window;
    }
}