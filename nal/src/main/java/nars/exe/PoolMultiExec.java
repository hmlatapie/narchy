package nars.exe;

import jcog.math.MutableInteger;
import nars.NAR;

import java.util.concurrent.Semaphore;

/**
 * uses a common forkjoin pool for execution
 */
public class PoolMultiExec extends AbstractExec {

    private final Revaluator revaluator;

    public final MutableInteger threads = new MutableInteger();
    private Focus focus;
    Semaphore ready;

    public PoolMultiExec(Revaluator revaluator, int capacity) {
        this(revaluator, Runtime.getRuntime().availableProcessors() - 1, capacity);
    }

    protected PoolMultiExec(Revaluator r, int threads, int capacity) {
        super(capacity);
        this.revaluator = r;
        this.threads.set(threads);
    }

    @Override
    public synchronized void start(NAR nar) {
        this.focus = new Focus(nar, revaluator);
        this.ready = new Semaphore(threads.intValue());
        super.start(nar);
    }

    @Override
    public synchronized void stop() {
        super.stop();
        ready = null;
        focus = null;
    }

    @Override
    public boolean concurrent() {
        return true;
    }


    @Override
    public void cycle() {
        super.cycle();

        int loopMS = nar.loop.periodMS.intValue();
        int dutyMS = Math.round(nar.loop.throttle.floatValue() * loopMS);
        if (dutyMS > 0) {
            long runUntil = System.currentTimeMillis() + dutyMS;
            double t = nar.loop.jiffy.doubleValue() * dutyMS/1000.0;
            while (ready.tryAcquire()) {
                execute(() -> {
                    try {
                        focus.runDeadline(
                                t,
                                () -> (System.currentTimeMillis() <= runUntil),
                                nar.random(), nar);
                    } finally {
                        ready.release();
                    }
                });
            }

        }
    }
}