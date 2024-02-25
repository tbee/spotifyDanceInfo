package org.tbee.spotifySlideshow;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.WritableRaster;
import java.util.HashMap;
import java.util.Map;

public class ImageUtil {
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
