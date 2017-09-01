package nars.control;

import com.google.common.util.concurrent.AtomicDouble;
import jcog.list.FasterList;
import jcog.math.RecycledSummaryStatistics;
import nars.Task;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.impl.list.mutable.primitive.ShortArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static nars.Param.CAUSE_CAPACITY;

/**
 * represents a causal influence and tracks its
 * positive and negative gain (separately).  this is thread safe
 * so multiple threads can safely affect the accumulators. it must be commited
 * periodically (by a single thread, ostensibly) to apply the accumulated values
 * and calculate the values
 * as reported by the value() function which represents the effective
 * positive/negative balance that has been accumulated. a decay function
 * applies forgetting, and this is applied at commit time by separate
 * positive and negative decay rates.  the value is clamped to a range
 * (ex: 0..+1) so it doesn't explode.
 */
public class Cause<X> {

    private float value;

    /** value and momentum correspond to the possible values in Purpose enum */
    public static void update(FasterList<Cause> causes, float[] value, RecycledSummaryStatistics[] summary, float[] momentum) {

        for (RecycledSummaryStatistics r : summary)
            r.clear();

        for (int i = 0, causesSize = causes.size(); i < causesSize; i++) {
            causes.get(i).commit(momentum, summary);
        }

        int p = value.length;
        for (int j = 0; j < p; j++) {
            summary[j].bipolarize();
        }

        for (float v : value) assert(v >= 0): "value should be non-negative";

        for (int i = 0, causesSize = causes.size(); i < causesSize; i++) {
            Cause c = causes.get(i);
            float v = 0;
            for (int j = 0; j < p; j++) {
                v += value[j] * summary[j].normPolar( c.purpose[j].current );
            }
            c.setValue(v);
        }



//        System.out.println("WORST");
//        causes.stream().map(x -> PrimitiveTuples.pair(x, x.negTotal())).sorted(
//                (x,y) -> Doubles.compare(y.getTwo(), x.getTwo())
//        ).limit(10).forEach(x -> {
//            System.out.println("\t" + x);
//        });
//        System.out.println();

    }


    public enum Purpose {
        /** neg: accepted for input, pos: activated in concept to some degree */
        Active,

        /** pos: anwers a question */
        Answer,

        /** pos: actuated a goal concept */
        Action,

        /** pos: confirmed a sensor input;  neg: contradicted a sensor input */
        Accurate
    }

    /** the AtomicDouble this inherits holds the accumulated value which is periodically (every cycle) committed  */
    public static class Traffic extends AtomicDouble {
        /** current, ie. the last commited value */
        public float current = 0;

        public double total = 0;

        public void commit(float momentum) {
            double next = getAndSet(0);
            this.total += next;
            this.current = smooth(current, next, momentum);
        }
    }


    public final short id;
    public final Object name;


    public final Traffic[] purpose;

    public Cause(short id, Object name) {
        this.id = id;
        this.name = name;
        purpose = new Traffic[Purpose.values().length];
        for (int i = 0; i < purpose.length; i++) {
            purpose[i] = new Traffic();
        }
    }

    @Override
    public String toString() {
        return name + "[" + id + "]=" + super.toString();
    }


    public static short[] zip(@Nullable Task... e) {
        switch (e.length) {
            case 0: throw new NullPointerException();
            case 1: return e[0].cause();
            case 2: return zip(e[0].cause(), e[1].cause(), CAUSE_CAPACITY);
            default:
                return zip(Stream.of(e)
                        .filter(Objects::nonNull).map(Task::cause).collect(toList()), CAUSE_CAPACITY); //HACK
        }

    }

    static short[] zip(short[] c0, short[] c1, int cap) {
        if (Arrays.equals(c0, c1)) return c0; //no change

        if (c0.length + c1.length < cap) {
            return ArrayUtils.addAll(c0, c1);
        } else {
            return zip(List.of(c0, c1), cap);
        }
    }

    static short[] zip(@NotNull List<short[]> s, int maxLen) {

        int ss = s.size();
        if (ss == 1) {
            return s.get(0);
        }

        ShortArrayList l = new ShortArrayList(maxLen);
        int ls = 0;
        int n = 1;
        boolean remain;
        main: do {
            remain = false;
            for (int i = 0, sSize = s.size(); i < sSize; i++) {
                short[] c = s.get(i);
                int cl = c.length;
                if (cl >= n) {
                    l.add(c[cl - n]);
                    if (++ls >= maxLen)
                        break main;
                    remain |= (cl >= (n + 1));
                }
            }
            n++;
        } while (remain);
        if (ls == 0)
            return ArrayUtils.EMPTY_SHORT_ARRAY;

        short[] ll = l.toArray();
        ArrayUtils.reverse(ll);
        assert(ll.length <= maxLen);
        return ll;
    }

    public void apply(Purpose p, float v) {
        purpose[p.ordinal()].addAndGet(v);
    }

    public void setValue(float nextValue) {
        this.value = nextValue;
    }

    /** scalar value representing the contribution of this cause to the overall valuation of a potential input that involves it */
    public float value() {
        return value;
    }

    public void commit(float[] momentums, RecycledSummaryStatistics[] valueSummary) {
        for (int i = 0, purposeLength = purpose.length; i < purposeLength; i++) {
            Traffic p = purpose[i];
            p.commit(momentums[i]);
            valueSummary[i].accept(p.current);
        }
    }

    /** calculate the value scalar  from the distinctly tracked positive and negative values;
     * any function could be used here. for example:
     *      simplest:           pos - neg
     *      linear combination: x * pos - y * neg
     *      quadratic:          pos*pos - neg*neg
     *
     * pos and neg will always be positive.
     * */
    public float value(float pos, float neg) {
        return pos - neg;
        //return pos * 2 - neg;
        //return Util.tanhFast( pos ) - Util.tanhFast( neg );
    }

    static float smooth(float cur, double next, float momentum) {
        return (float)((cur * momentum) + ((1f - momentum) * next));
    }

}
