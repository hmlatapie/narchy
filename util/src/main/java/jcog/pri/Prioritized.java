package jcog.pri;


import jcog.Texts;
import jcog.Util;
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * something which has a priority floating point value
 *      stores priority with 32-bit float precision
 *      restricted to 0..1.0 range
 *      NaN means it is 'deleted' which is a valid and testable state
 */
public interface Prioritized extends Deleteable {

    /**
     * default minimum difference necessary to indicate a significant modification in budget float number components
     */
    float EPSILON =             0.0005f;

    static float sum(Prioritized... src) {
        return Util.sum(Prioritized::priElseZero, src);
    }
    static float max(Prioritized... src) {
        return Util.max(Prioritized::priElseZero, src);
    }


    /**
     * returns the local (cached) priority value in range 0..1.0 inclusive.
     * if the value is NaN, then it means this has been deleted
     */
    float pri();


    /** used during periodic async updates to allow implementations to
     *  modify the local value depending on the referent or other condition
     */
    default float priUpdate() {
        return pri();
    }

    /**
     * common instance for a 'Deleted budget'.
     */
    Prioritized Deleted = new PriRO(Float.NaN);
    /**
     * common instance for a 'full budget'.
     */
    Prioritized One = new PriRO(1f);
    /**
     * common instance for a 'half budget'.
     */
    Prioritized Half = new PriRO(0.5f);
    /**
     * common instance for a 'zero budget'.
     */
    Prioritized Zero = new PriRO(0);


    static String toString(Prioritized b) {
        return toStringBuilder(null, Texts.n4(b.pri())).toString();
    }

    @NotNull
    static StringBuilder toStringBuilder(@Nullable StringBuilder sb, String priorityString) {
        int c = 1 + priorityString.length();
        if (sb == null)
            sb = new StringBuilder(c);
        else {
            sb.ensureCapacity(c);
        }

        return sb.append('$').append(priorityString);
    }

    @NotNull
    static Ansi.Color budgetSummaryColor(@NotNull Prioritized tv) {
        int s = (int) Math.floor(tv.priElseZero() * 5);
        switch (s) {
            default:
                return Ansi.Color.DEFAULT;

            case 1:
                return Ansi.Color.MAGENTA;
            case 2:
                return Ansi.Color.GREEN;
            case 3:
                return Ansi.Color.YELLOW;
            case 4:
                return Ansi.Color.RED;

        }
    }

    static <X extends Priority> void normalize(X[] xx, float target) {
        int l = xx.length;
        assert (target == target);
        assert (l > 0);

        float ss = sum(xx);
        if (ss <= Pri.EPSILON)
            return;

        float factor = target / ss;

        for (X x : xx)
            x.priMult(factor);

    }

    default float priElse(float valueIfDeleted) {
        float p = pri();
        return p == p ? p : valueIfDeleted;
    }


    default float priElseZero() {
        float p = pri();
        return p == p ? p : 0;
        //return priElseZero();
    }
    default float priElseNeg1() {
        float p = pri();
        return p == p ? p : -1;
        //return priSafe(-1);
    }

    @Override
    default boolean isDeleted() {
        float p = pri();
        return p!=p; //fast NaN check
    }








//    static void normalizePriSum(@NotNull Iterable<? extends Prioritized> l, float total) {
//
//        float priSum = Prioritized.priSum(l);
//        float mult = total / priSum;
//        for (Prioritized b : l) {
//            b.priMult(mult);
//        }
//
//    }
//
//    /**
//     * randomly selects an item from a collection, weighted by priority
//     */
//    static <P extends Prioritized> P selectRandomByPriority(@NotNull NAR memory, @NotNull Iterable<P> c) {
//        float totalPriority = priSum(c);
//
//        if (totalPriority == 0) return null;
//
//        float r = memory.random.nextFloat() * totalPriority;
//
//        P s = null;
//        for (P i : c) {
//            s = i;
//            r -= s.priElseZero();
//            if (r < 0)
//                return s;
//        }
//
//        return s;
//
//    }

}
