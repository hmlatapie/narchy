package nars.video;

import java.awt.image.BufferedImage;
import java.util.function.Supplier;

import static nars.video.Bitmap2D.*;

/**
 * exposes a buffered image as a camera video source
 */
public class BufferedImageBitmap2D implements Bitmap2D, Supplier<BufferedImage> {

    Supplier<BufferedImage> source;
    public BufferedImage out;

    @Override
    public int width() {
        return out.getWidth();
    }

    @Override
    public int height() {
        return out.getHeight();
    }


    @Override
    public void update(float frameRate) {
        if (this.source!=null) //get next frame
            out = source.get();
    }

    public void see(EachPixelRGB p) {
        final BufferedImage b = this.out;
        if (b == null)
            return;

        int height = height();
        int width = width();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                p.pixel(x, y, b.getRGB(x, y));
            }
        }
    }

    @Override public float brightness(int xx, int yy) {
        if (out!=null) {
            int rgb = out.getRGB(xx, yy);
            return (decodeRed(rgb) + decodeGreen(rgb) + decodeBlue(rgb)) / 3f;
        }
        return Float.NaN;
    }

//    public void updateBuffered(EachPixelRGBf m) {
//        see(
//                (x, y, p) -> {
//                    intToFloat(m, x, y, p);
//                }
//        );
//    }

    public float red(int x, int y) {
        return outsideBuffer(x, y) ? Float.NaN : decodeRed(out.getRGB(x, y));
    }
    public float green(int x, int y) {
        return outsideBuffer(x, y) ? Float.NaN : decodeGreen(out.getRGB(x, y));
    }
    public float blue(int x, int y) { return outsideBuffer(x, y) ? Float.NaN : decodeBlue(out.getRGB(x,y)); }

    public boolean outsideBuffer(int x, int y) {
        return out == null || (x < 0) || (y < 0) || (x >= out.getWidth()) || (y >= out.getHeight());
    }

    /** for chaining these together */
    @Override public BufferedImage get() {
        update(1);
        return out;
    }
}