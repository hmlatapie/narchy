package jcog.exe;

import org.apache.commons.lang3.mutable.MutableInt;

import java.util.function.LongConsumer;

/** synchronously triggered executor but executes only if elapsed time exceeds adjustable threshold.
 * milliseconds
 * */
public class Every {

    public static Every Never = new Every((LongConsumer)null, Integer.MAX_VALUE) {
        @Override
        public void next() {
            //nothing
        }
    };

    public final MutableInt periodMS = new MutableInt();


    private final LongConsumer each;
    long last = System.currentTimeMillis();

    public Every(Runnable r, int initialPeriodMS) {
        this((x)->r.run(), initialPeriodMS);
    }

    public Every(LongConsumer r, int initialPeriodMS) {
        this.each = r;
        this.periodMS.set(initialPeriodMS);
    }

    public synchronized void next() {
        long now = System.currentTimeMillis();
        long delta = now - last;
        if (delta > periodMS.intValue()) {
            last = now;
            each.accept(delta);
        }
    }
}
