package nars.table;

import nars.Task;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

import static nars.time.Tense.ETERNAL;

/** query object used for selecting tasks */
public interface TaskMatch {

    /** max values to return */
    int limit();
    
    long start();
    long end();

    /** value ranking function */
    FloatFunction<Task> value();

    /** the RNG used if to apply random sampling, null to disable sampling (max first descending) */
    default @Nullable Random sample() {
        return null;
    }

    /** answer the strongest available task within the specified range */
    static TaskMatch best(long start, long end) {
        return new Best(start, end);
    }

    static TaskMatch best(long start, long end, FloatFunction<Task> factor) {
        return new BestWithFactor(start, end, factor);
    }

    /** gets one task, sampled fairly from the available tasks in the
     * given range according to strength */
    static TaskMatch sampled(long start, long end, Random random) {
        return new Sampled(start, end, random);
    }

    class Best implements TaskMatch, FloatFunction<Task> {
        private final long start;
        private final long end;
        private final FloatFunction<Task> value;

        public Best(long start, long end) {
            this.start = start;
            this.end = end;

            if (start == ETERNAL) {
                //DEFAULT ETERNAL VALUE FUNCTION

                value = (Task t) -> (t.isBeliefOrGoal() ?
                        //BELIEFS/GOALS
                        //t.evi()
                        t.conf()
                        :
                        //QUESTIONS/QUESTS
                        t.pri() * t.originality()
                );
            } else {
                //DEFAULT TEMPORAL VALUE FUNCTION

                value = (Task t) -> (t.isBeliefOrGoal() ?
                        //BELIEFS/GOALS
                        TemporalBeliefTable.value(t, start, end, 1)
                        :
                        //QUESTIONS/QUESTS
                        t.pri() / (1 + t.minDistanceTo(start, end))
                );
            }
        }

        @Override
        public float floatValueOf(Task x) {
            return value.floatValueOf(x);
        }

        @Override
        public final FloatFunction<Task> value() {
            return this;
        }

        @Override
        public long start() {
            return start;
        }

        @Override
        public long end() {
            return end;
        }

        @Override
        public int limit() {
            return 1;
        }

    }

    /** TODO add custom value function */
    class Sampled extends Best {
        private final Random random;

        public Sampled(long start, long end, Random random) {
            super(start, end);
            this.random = random;
        }

        @Override
        public @Nullable Random sample() {
            return random;
        }
    }

    class BestWithFactor extends Best {

        private final FloatFunction<Task> factor;
        private float max;

        public BestWithFactor(long start, long end, FloatFunction<Task> factor) {
            super(start, end);
            this.factor = factor;
            max = Float.NEGATIVE_INFINITY;
        }

        @Override
        public float floatValueOf(Task x) {

            float p = super.floatValueOf(x);
            if (limit()==1) {
                if (p > max)
                    max = p;
                else if (p < max) {
                    //already below the current, elide call to the additional factor since this is simple multiplication
                    return Float.NEGATIVE_INFINITY;
                }
            }

            return p * factor.floatValueOf(x);
        }
    }
}
