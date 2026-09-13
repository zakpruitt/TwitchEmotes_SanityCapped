package com.zakpruitt.sanitycapped;

import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

final class TestImages {

    private TestImages() {
    }

    static MockMultipartFile png(Color color) {
        return new MockMultipartFile("file", "emote.png", "image/png", bytes(color));
    }

    static byte[] bytes(Color color) {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, 32, 32);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 16, 8);
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }
}
