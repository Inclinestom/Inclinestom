package net.minestom.demo.tools;

import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;

class AreaVisualiser {

    private static final int width = 800;

    private static double zoom = 1.5;

    public static void main(String[] args) {
        Random random = new Random(0);
        List<Area> areas = new ArrayList<>();

        Supplier<Double> randomDouble = () -> random.nextDouble() * 1000 - 500;
        Supplier<Vec> randomVec = () -> new Vec(randomDouble.get(), randomDouble.get(), randomDouble.get());

        for (int i = 0; i < 1000; i++) {
            areas.add(Area.fill(randomVec.get(), randomVec.get()));
        }

        Area union = Area.optimize(Area.union(areas));

        window(graphics -> {
            graphics.setColor(new Color(0x40C67432, true));
            drawArea(graphics, union);
        });
    }

    public static void drawArea(Graphics graphics, Area area) {
        Color color = graphics.getColor();

        for (Area fill : area.subdivide()) {
            graphics.setColor(color);
            Point min = fill.min();
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
            zoom *= 1.0 + (evt.getPreciseWheelRotation() * 0.5);
            window.repaint();
        });

        return window;
    }
}