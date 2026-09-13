package com.jsirgalaxybase.ui2.demo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.jsirgalaxybase.ui2.lab.UiLabPage;
import com.jsirgalaxybase.ui2.lab.UiLabScene;

public final class UiLabApplication {
    private UiLabApplication() {}

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("UI Lab interactive mode requires a graphical environment; use renderGolden for headless output.");
            return;
        }
        SwingUtilities.invokeLater(new Runnable() { @Override public void run() { open(); } });
    }

    private static void open() {
        final Preview preview = new Preview();
        final JComboBox<UiLabPage> pages = new JComboBox<UiLabPage>(UiLabPage.values());
        final JComboBox<String> sizes = new JComboBox<String>(new String[] { "350×193", "620×340" });
        final JCheckBox debug = new JCheckBox("调试层");
        final JCheckBox reduced = new JCheckBox("减少动效");
        ActionListener refresh = new ActionListener() {
            @Override public void actionPerformed(ActionEvent event) {
                preview.page = (UiLabPage) pages.getSelectedItem();
                preview.large = sizes.getSelectedIndex() == 1;
                preview.debug = debug.isSelected();
                preview.reducedMotion = reduced.isSelected();
                preview.refresh();
            }
        };
        pages.addActionListener(refresh); sizes.addActionListener(refresh); debug.addActionListener(refresh); reduced.addActionListener(refresh);
        JPanel tools = new JPanel();
        tools.add(new JLabel("页面")); tools.add(pages); tools.add(new JLabel("视口")); tools.add(sizes); tools.add(debug); tools.add(reduced);

        JFrame frame = new JFrame("Galaxy UI 2 Lab");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(tools, BorderLayout.NORTH); frame.add(preview, BorderLayout.CENTER);
        preview.refresh(); frame.pack(); frame.setLocationRelativeTo(null); frame.setVisible(true);
    }

    private static final class Preview extends JPanel {
        private final UiLabScene scene = new UiLabScene();
        private final Java2dRenderer renderer = new Java2dRenderer();
        private UiLabPage page = UiLabPage.OVERVIEW;
        private boolean large;
        private boolean debug;
        private boolean reducedMotion;
        private BufferedImage image;

        private void refresh() {
            int width = large ? 620 : 350, height = large ? 340 : 193;
            image = renderer.render(scene.render(page, width, height, debug, reducedMotion), width, height);
            setPreferredSize(new Dimension(width * 2, height * 2));
            revalidate(); repaint();
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (image == null) return;
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(image, 0, 0, getWidth(), getHeight(), null);
            g2.dispose();
        }
    }
}
