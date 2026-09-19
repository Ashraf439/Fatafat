package com.ashraf.seed;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Stitches 2-3 dish photos side by side into one wide JPEG (used for multi-meal restaurants). */
public final class CollageBuilder {

    static { System.setProperty("java.awt.headless", "true"); }

    private static final int WIDTH = 1200;
    private static final int HEIGHT = 600;
    private static final int GAP = 6;

    private CollageBuilder() {}

    /** @return JPEG bytes, or null if none of the inputs could be decoded. */
    public static byte[] collage(List<byte[]> images) throws IOException {
        List<BufferedImage> decoded = new ArrayList<>();
        for (byte[] bytes : images) {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img != null) decoded.add(img);
        }
        if (decoded.isEmpty()) return null;

        BufferedImage canvas = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, WIDTH, HEIGHT);

            int n = decoded.size();
            int panelWidth = (WIDTH - GAP * (n - 1)) / n;
            for (int i = 0; i < n; i++) {
                BufferedImage img = decoded.get(i);
                int x = i * (panelWidth + GAP);
                double scale = Math.max((double) panelWidth / img.getWidth(), (double) HEIGHT / img.getHeight());
                int sw = (int) Math.ceil(img.getWidth() * scale);
                int sh = (int) Math.ceil(img.getHeight() * scale);
                Shape oldClip = g.getClip();
                g.setClip(x, 0, panelWidth, HEIGHT);
                g.drawImage(img, x + (panelWidth - sw) / 2, (HEIGHT - sh) / 2, sw, sh, Color.WHITE, null);
                g.setClip(oldClip);
            }
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(canvas, "jpg", out);
        return out.toByteArray();
    }
}
