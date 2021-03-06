package nars.concept.scalar;

import com.google.common.collect.Iterables;
import jcog.Util;
import jcog.math.FloatSupplier;
import jcog.util.AtomicFloat;
import nars.$;
import nars.NAR;
import nars.control.CauseChannel;
import nars.control.NARService;
import nars.task.ITask;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static nars.Op.BELIEF;

/** base class for a multi-concept representation of a real scalar value input */
abstract public class DemultiplexedScalar extends NARService implements Iterable<Scalar>, Consumer<NAR>, FloatSupplier {

    public final AtomicFloat value = new AtomicFloat();

    public final CauseChannel<ITask> in;

    public final FloatSupplier input;

    private final FloatFloatToObjectFunction<Truth> truther;

    public final Term term;

    @Override
    public final float asFloat() {
        return value.floatValue();
    }

    protected DemultiplexedScalar(FloatSupplier input, @Nullable Term id, NAR nar) {
        super(id);

        this.term = id;

        this.input = input;
        this.in = nar.newCauseChannel(id);
        this.truther = (prev,next) -> next==next ? $.t(Util.unitize(next), nar.confDefault(BELIEF)) : null;
    }

    public DemultiplexedScalar resolution(float r) {
        forEach(s -> s.resolution(r));
        return this;
    }


    @Override
    public void accept(NAR n) {
        long now = n.time();
        int dur = n.dur();
        update(now-dur/2, now+dur/2, n);
    }

    public void update(long start, long end, NAR n) {

        value.set(input.asFloat());

        in.input(Iterables.transform(this, x -> x.update(start, end, truther, n)));
    }

    public void pri(FloatSupplier p) {
        forEach(x -> x.pri(p));
    }
}
