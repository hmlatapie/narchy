package nars.concept.scalar;

import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.concept.Sensor;
import nars.concept.dynamic.ScalarBeliefTable;
import nars.concept.util.ConceptBuilder;
import nars.control.DurService;
import nars.task.signal.SignalTask;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.LongSupplier;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;


/**
 * primarily a collector for belief-generating time-changing input scalar (1-d real value) signals
 */
public class Scalar extends Sensor implements FloatFunction<Term>, FloatSupplier {

    /** update directly with next value */
    public static Function<FloatSupplier,FloatFloatToObjectFunction<Truth>> SET = (conf)->
            ((p,n) -> n==n ? $.t(n, conf.asFloat()) : null);
    /** first order difference */
    public static Function<FloatSupplier,FloatFloatToObjectFunction<Truth>> DIFF = (conf)->
            ((p,n) -> (n==n) ? ((p==p) ? $.t((n-p)/2f + 0.5f, conf.asFloat()) : $.t(0.5f, conf.asFloat())) : $.t(0.5f, conf.asFloat()));
    public FloatSupplier signal;

    private volatile float currentValue = Float.NaN;

    public Scalar(Term c, FloatSupplier signal, NAR n) {
        this(c, BELIEF, signal, n);
    }

    public Scalar(Term c, byte punc, FloatSupplier signal, NAR n) {
        this(c, punc, signal, n.conceptBuilder);
        pri(() -> n.priDefault(punc));
        ((ScalarBeliefTable)beliefs()).res(resolution);
        n.on(this);
    }

    private Scalar(Term term, byte punc, FloatSupplier signal, ConceptBuilder b) {
        super(term,
                punc == BELIEF ? new ScalarBeliefTable(term, true, b) : b.newTable(term, true),
                punc == GOAL ? new ScalarBeliefTable(term, false, b) : b.newTable(term, false),
                b);

        this.signal = signal;

    }

    public final DurService auto(NAR n) {
        return auto(n, 1);
    }

    public DurService auto(NAR n, float durs) {
        FloatFloatToObjectFunction<Truth> truther =
            (prev, next) -> $.t(next, n.confDefault(BELIEF));

        return DurService.on(n, nn->
                nn.input(update(truther, n))).durs(durs);
    }

    /**
     * returns a new stamp for a sensor task
     */
    protected LongSupplier nextStamp(NAR nar) {
        return nar.time::nextStamp;
    }



    @Override
    public float floatValueOf(Term anObject /* ? */) {
        return this.currentValue = signal.asFloat();
    }


    @Override
    public float asFloat() {
        return currentValue;
    }

    public final Task update(FloatFloatToObjectFunction<Truth> truther, NAR n) {
        return update(truther, n.time(), n.dur(), n);
    }

    @Nullable
    @Deprecated public Task update(FloatFloatToObjectFunction<Truth> truther, long time, int dur, NAR n) {
        return update(time-dur/2, time+dur/2, truther, n);
    }

    @Nullable
    public Task update(long start, long end, FloatFloatToObjectFunction<Truth> truther, NAR n) {

        Truth nextTruth = truther.value(currentValue, floatValueOf(term));
        if (nextTruth!=null) {
            SignalTask x = ((ScalarBeliefTable) beliefs()).add(nextTruth,
                    start, end, n);


            return x;
        } else {
            return null;
        }
    }


    public Scalar resolution(float r) {
        resolution.set(r);
        return this;
    }

    public Scalar pri(FloatSupplier pri) {
        ((ScalarBeliefTable)beliefs()).pri(pri);
        return this;
    }

}
