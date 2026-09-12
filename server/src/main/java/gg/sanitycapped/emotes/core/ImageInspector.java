package gg.sanitycapped.emotes.core;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.stereotype.Component;

/**
 * Decodes an upload to find out what it really is: the true type, the pixel
 * size, whether it animates, and a perceptual hash of its first frame.
 */
@Component
public class ImageInspector {

    /** A renamed .exe is not an emote, so the type comes from the bytes. */
    public String sniff(byte[] b) {
        if (starts(b, 0x47, 0x49, 0x46, 0x38)) {
            return "image/gif";
        }
        if (starts(b, 0x89, 0x50, 0x4E, 0x47)) {
            return "image/png";
        }
        if (starts(b, 0xFF, 0xD8, 0xFF)) {
            return "image/jpeg";
        }
        if (starts(b, 0x52, 0x49, 0x46, 0x46) && b.length > 12
                && b[8] == 0x57 && b[9] == 0x45 && b[10] == 0x42 && b[11] == 0x50) {
            return "image/webp";
        }
        return null;
    }

    public String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/gif" -> "gif";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/jpeg" -> "jpg";
            default -> throw new IllegalArgumentException(contentType);
        };
    }

    /** Null when the bytes aren't an image we can read. */
    public ImageInfo inspect(byte[] data) {
        String type = sniff(data);
        if (type == null) {
            return null;
        }
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream);
                int frames = reader.getNumImages(true);
                BufferedImage first = reader.read(0);
                return new ImageInfo(type, extensionFor(type), first.getWidth(), first.getHeight(),
                        Math.max(frames, 1), dhash(first));
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /**
     * 64-bit difference hash: squash the first frame to 9x8 over black, then
     * record whether each pixel is brighter than its right-hand neighbour.
     * Resizes, recompressions and light recolours land within a few bits.
     */
    public String dhash(BufferedImage source) {
        BufferedImage small = new BufferedImage(9, 8, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = small.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        // TYPE_INT_RGB starts black, so transparent padding reads as dark rather
        // than as bright noise -- the same compositing the browser preview does.
        g.drawImage(source, 0, 0, 9, 8, null);
        g.dispose();

        double[] grey = new double[72];
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 9; x++) {
                int rgb = small.getRGB(x, y);
                grey[y * 9 + x] = 0.299 * ((rgb >> 16) & 0xFF)
                        + 0.587 * ((rgb >> 8) & 0xFF)
                        + 0.114 * (rgb & 0xFF);
            }
        }

        long bits = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                bits = (bits << 1) | (grey[y * 9 + x] > grey[y * 9 + x + 1] ? 1 : 0);
            }
        }
        return String.format("%016x", bits);
    }

    private static boolean starts(byte[] b, int... signature) {
        if (b.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((b[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
