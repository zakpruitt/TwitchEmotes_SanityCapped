package com.zakpruitt.sanitycapped.image;

import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;

@Component
public class ImageInspector {

    private static final int HASH_WIDTH = 9;
    private static final int HASH_HEIGHT = 8;

    private static double[] greyscaleThumbnail(BufferedImage source) {
        // TYPE_INT_RGB starts black, so an emote's transparent padding reads as
        // dark rather than as bright noise.
        BufferedImage small = new BufferedImage(HASH_WIDTH, HASH_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = small.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, HASH_WIDTH, HASH_HEIGHT, null);
        g.dispose();

        double[] grey = new double[HASH_WIDTH * HASH_HEIGHT];
        for (int y = 0; y < HASH_HEIGHT; y++) {
            for (int x = 0; x < HASH_WIDTH; x++) {
                int rgb = small.getRGB(x, y);
                grey[y * HASH_WIDTH + x] = 0.299 * ((rgb >> 16) & 0xFF)
                        + 0.587 * ((rgb >> 8) & 0xFF)
                        + 0.114 * (rgb & 0xFF);
            }
        }
        return grey;
    }

    public Optional<ImageInfo> inspect(byte[] data) {
        Optional<ImageType> type = ImageType.sniff(data);
        if (type.isEmpty()) {
            return Optional.empty();
        }
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                return Optional.empty();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream);
                BufferedImage first = reader.read(0);
                int frames = Math.max(reader.getNumImages(true), 1);
                return Optional.of(new ImageInfo(type.get(), first.getWidth(), first.getHeight(),
                        frames, dhash(first)));
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    /**
     * 64-bit difference hash: squash the first frame to 9x8 over black, then
     * record whether each pixel is brighter than its right-hand neighbour. Near
     * duplicates land within a few bits of the original.
     */
    public String dhash(BufferedImage source) {
        double[] grey = greyscaleThumbnail(source);

        long bits = 0;
        for (int y = 0; y < HASH_HEIGHT; y++) {
            for (int x = 0; x < HASH_HEIGHT; x++) {
                int left = y * HASH_WIDTH + x;
                bits = (bits << 1) | (grey[left] > grey[left + 1] ? 1 : 0);
            }
        }
        return "%016x".formatted(bits);
    }
}
