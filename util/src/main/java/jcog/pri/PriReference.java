package jcog.pri;

import jcog.Util;
import jcog.math.FloatSupplier;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * prioritized reference
 */
public interface PriReference<X> extends Priority, Supplier<X>, FloatSupplier {

    @NotNull
    static float[] histogram(Iterable<? extends PriReference> pp, float[] x) {
        int bins = x.length;
        final double[] total = {0};

        pp.forEach(y -> {
            if (y == null)
                return; // ?
            float p = y.priElseZero();
            if (p > 1f) p = 1f; //just to be safe
            int b = Util.bin(p, bins);
            x[b]++;
            total[0]++;
        });

        double t = total[0];
        if (t > 0) {
            for (int i = 0; i < bins; i++)
                x[i] /= t;
        }
        return x;
    }

    /**
     * double[histogramID][bin]
     */
    @NotNull
    static <X, Y> double[][] histogram(@NotNull Iterable<PriReference<Y>> pp, @NotNull BiConsumer<PriReference<Y>, double[][]> each, @NotNull double[][] d) {

        pp.forEach(y -> {
            each.accept(y, d);
            //float p = y.priElseZero();
            //x[Util.bin(p, bins - 1)]++;
        });

        for (double[] e : d) {
            double total = 0;
            for (int i = 0, eLength = e.length; i < eLength; i++) {
                total += e[i];
            }
            if (total > 0) {
                for (int i = 0, eLength = e.length; i < eLength; i++) {
                    double f = e[i];
                    e[i] /= total;
                }
            }
        }

        return d;
    }

    @Override
    default float asFloat() {
        return pri();
    }
}

