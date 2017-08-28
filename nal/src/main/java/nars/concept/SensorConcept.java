package nars.concept;

import jcog.math.FloatSupplier;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.table.BeliefTable;
import nars.task.SignalTask;
import nars.term.Term;
import nars.truth.Truth;
import nars.util.signal.ScalarSignal;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.LongSupplier;


/**
 * primarily a collector for believing time-changing input signals
 */
public class SensorConcept extends WiredConcept implements FloatFunction<Term>, FloatSupplier {

    public final ScalarSignal sensor;
    private FloatSupplier signal;
    protected float currentValue = Float.NaN;

    static final Logger logger = LoggerFactory.getLogger(SensorConcept.class);


    public SensorConcept(@NotNull Term c, @NotNull NAR n, FloatSupplier signal, FloatToObjectFunction<Truth> truth) {
        super(c,
                //new SensorBeliefTable(n.conceptBuilder.newTemporalBeliefTable(c)),
                null,
                null, n);

        this.sensor = new ScalarSignal(n, c, this, truth, resolution) {
            @Override
            protected LongSupplier stamp(Truth currentBelief, @NotNull NAR nar) {
                return SensorConcept.this.nextStamp(nar);
            }
        };
        //((SensorBeliefTable)beliefs).sensor = sensor;

        this.signal = signal;

    }

    /**
     * returns a new stamp for a sensor task
     */
    protected LongSupplier nextStamp(@NotNull NAR nar) {
        return nar.time::nextStamp;
    }


    public void setSignal(FloatSupplier signal) {
        this.signal = signal;
    }


    @Override
    public float floatValueOf(Term anObject /* ? */) {
        return this.currentValue = signal.asFloat();
    }


    @Override
    public float asFloat() {
        return currentValue;
    }


    /**
     * should only be called if autoupdate() is false
     */
    @Nullable
    public Task update(long time, int dur, NAR nar) {

        Task x = sensor.update(nar, time, dur);

        if (x != null) {
            feedback(x, beliefs(), time, nar);
        }

        return x;
    }

    public static void feedback(Task x, @NotNull BeliefTable beliefs, long time, NAR nar) {
        float xFreq = x.freq();
        float xConf = x.conf();


        float fThresh = 1f - Math.max(0, Math.min(1f, (Param.SENSOR_FEEDBACK_FREQ_THRESHOLD * nar.truthResolution.floatValue())));


        int dur = nar.dur();

        //sensor feedback
        //punish any non-signal beliefs at the current time which contradict this sensor reading, and reward those which it supports
        beliefs.forEachTask(false, time - dur / 2, time + dur / 2, (y) -> {
            if (y instanceof SignalTask)
                return; //ignore previous signaltask

            float coherence = 1f - Math.abs(y.freq() - xFreq);

            float confidence = y.conf() / xConf; //allow > 1

            if (coherence > fThresh) {
                //reward
                nar.emotion.value(y.cause(), confidence);
            } else {
                //punish
                nar.emotion.value(y.cause(), -confidence * (1f - coherence));
                y.delete();
            }
        });
    }

    public SensorConcept resolution(float r) {
        resolution.setValue(r);
        return this;
    }

}
