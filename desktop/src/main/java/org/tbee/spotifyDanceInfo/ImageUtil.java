package org.tbee.spotifyDanceInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ImageUtil {

    private static final Logger logger = LoggerFactory.getLogger(ImageUtil.class);

    public static byte[] read(URL url) {
        byte[] bytes = new byte[]{};
        try (
                InputStream inputStream = url.openStream();
        ) {
            bytes = inputStream.readAllBytes();
        }
        catch (IOException e) {
            logger.error("Error loading image ", e);
        }
        return bytes;
    }

    static public BufferedImage resizeFilling(BufferedImage image, Dimension targetSize) {
        // Read image
        double imageHeight = (double)image.getHeight();
        double imageWidth = (double)image.getWidth();

        // Resize to fill (probably overflow) the target size, but maintain aspect ratio
        double widthScaleFactor = targetSize.getWidth() / imageWidth;
        double heightScaleFactor = targetSize.getHeight() / imageHeight;
        double scaleFactor = Math.max(widthScaleFactor, heightScaleFactor);
        int newWidth = (int)(imageWidth * scaleFactor);
        int newHeight = (int)(imageHeight * scaleFactor);

        // Paint full size (possibly overflowing the target)
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resizedImage.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(image, 0, 0, newWidth, newHeight, null); // draw and scale image
        g2.dispose();

        // clip to target size
        int targetWidth = (int) targetSize.getWidth();
        int targetHeight = (int) targetSize.getHeight();
        int clipX = Math.max(0, (newWidth - targetWidth) / 2);
        int clipY = Math.max(0, (newHeight - targetHeight) / 2);
        int clipWidth = Math.min(resizedImage.getWidth(), targetWidth);
        int clipHeight = Math.min(resizedImage.getHeight(), targetHeight);
        return resizedImage.getSubimage(clipX, clipY, clipWidth, clipHeight);
    }

    static public BufferedImage resizeFitting(BufferedImage image, Dimension targetSize) {
        // Read image
        double imageHeight = (double)image.getHeight();
        double imageWidth = (double)image.getWidth();

        // Resize to fit inside the target size, but maintain aspect ratio
        double widthScaleFactor = targetSize.getWidth() / imageWidth;
        double heightScaleFactor = targetSize.getHeight() / imageHeight;
        double scaleFactor = Math.min(widthScaleFactor, heightScaleFactor);
        int newWidth = (int)(imageWidth * scaleFactor);
        int newHeight = (int)(imageHeight * scaleFactor);

        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resizedImage.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(image, 0, 0, newWidth, newHeight, null); // draw and scale image
        g2.dispose();

        return resizedImage;
    }

    static public BufferedImage addGaussianBlur(BufferedImage image, double radius) {
        return getGaussianBlurOp(radius).filter(image, null);
    }

    static public ConvolveOp getGaussianBlurOp(final double stddev) {
        // calculate the kernel matrix
        int radius = (int) Math.ceil(4.0 * stddev);
        int size = 2 * radius + 1;

        float[] kernel = new float[size * size];

        double r, s = 2.0 * stddev * stddev;
        double total = 0.0;

        int i = 0;
        for (int y = -radius; y <= radius; ++y) {
            for (int x = -radius; x <= radius; ++x, ++i) {
                r = Math.sqrt(x * x + y * y);
                kernel[i] = (float) Math.exp(-r * r / s);

                total += kernel[i];
            }
        }

        // Normalize the data
        for (i = 0; i < kernel.length; i++) kernel[i] /= total;

        // calculate the size
        int stddevr = (int) Math.ceil(4.0 * stddev);
        int stddevsize = 2 * stddevr + 1;

        // done
        return new ConvolveOp(new java.awt.image.Kernel(stddevsize, stddevsize, kernel), ConvolveOp.EDGE_NO_OP, new RenderingHints(getHighQualityRenderingHints()));
    }

    static public Map<RenderingHints.Key, Object> getHighQualityRenderingHints() {
        Map<RenderingHints.Key, Object> highQualityRenderingHints = new HashMap<RenderingHints.Key, Object>();
        highQualityRenderingHints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        highQualityRenderingHints.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        highQualityRenderingHints.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        highQualityRenderingHints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        highQualityRenderingHints.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        highQualityRenderingHints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        return highQualityRenderingHints;
    }

    static public BufferedImage addNoise(double noiseFactor, BufferedImage image) {
        return addNoise(noiseFactor, 0, 0, image.getWidth(), image.getHeight(), image);
    }

    static public BufferedImage addNoise(double noiseFactor, int x, int y, int width, int height, BufferedImage image) {
        WritableRaster out = image.getRaster();

        int currVal; // the current value
        double newVal; // the new "noisy" value
        double gaussian; // gaussian number
        int bands = out.getNumBands(); // number of bands
        java.util.Random randGen = new java.util.Random();

        for (int j = y; j < y + height; j++) {
            for (int i = x; i < x + width; i++) {
                gaussian = randGen.nextGaussian();

                for (int b = 0; b < bands; b++) {
                    newVal = noiseFactor * gaussian;
                    currVal = out.getSample(i, j, b);
                    newVal = newVal + currVal;
                    if (newVal < 0) newVal = 0.0;
                    if (newVal > 255) newVal = 255.0;

                    out.setSample(i, j, b, (int) (newVal));
                }
            }
        }
        return image;
    }
}
