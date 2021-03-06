package nars.task.util;

import nars.Task;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * dimensions:
 * 0: long time start..end
 * 1: float freq min..max
 * 2: float conf min..max
 */
public final class TasksRegion implements TaskRegion {




    public final long start;
    public final long end; //allow end to stretch for ongoing tasks

    public final float freqMin, freqMax, confMin, confMax;

//    static private volatile int SERIAL = 0;
//    public final int serial = SERIAL++;

    @Override
    public final long start() {
        return start;
    }

    @Override
    public final long end() {
        return end;
    }

//    @Override
//    public boolean equals(Object obj) {
//        return obj != null && (this == obj || (task != null && Objects.equals(task, ((TaskRegion) obj).task())));
//    }

//    @Override
//    public int hashCode() {
//        return serial;
//    }

    @Override
    public String toString() {
         return Arrays.toString(new double[]{start, end, freqMin, freqMax, confMin, confMax});
    }

    public TasksRegion(long start, long end, float freqMin, float freqMax, float confMin, float confMax) {
        this.start = start;
        this.end = end;
        this.freqMin = freqMin;
        this.freqMax = freqMax;
        this.confMin = confMin;
        this.confMax = confMax;
    }

    //private static final AtomicInteger serials = new AtomicInteger(0);




//        /**
//         * all inclusive time region
//         */
//        TaskRegion(long a, long b) {
//            this(a, b, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
//        }


    @Override
    public float coordF(boolean maxOrMin, int dimension) {
        if (maxOrMin) {
            switch (dimension) {
                case 0: return end;
                case 1: return freqMax;
                case 2: return confMax;
            }
        } else {
            switch (dimension) {
                case 0: return start;
                case 1: return freqMin;
                case 2: return confMin;
            }
        }
//        switch (dimension) {
//            case 0:
//                return maxOrMin ? end : start;
//            case 1:
//                return maxOrMin ? freqMax : freqMin;
//            case 2:
//                return maxOrMin ? confMax : confMin;
//        }
        throw new UnsupportedOperationException();
    }



    @Override
    public @Nullable Task task() {
        return null;
    }

}
