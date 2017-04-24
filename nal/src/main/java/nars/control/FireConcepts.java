package nars.control;

import jcog.data.FloatParam;
import jcog.data.MutableIntRange;
import jcog.data.MutableInteger;
import jcog.data.sorted.SortedArray;
import jcog.event.On;
import jcog.pri.PLink;
import jcog.pri.PriMerge;
import jcog.pri.Prioritized;
import nars.Focus;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.derive.Deriver;
import nars.premise.Derivation;
import nars.premise.DerivationBudgeting;
import nars.premise.Premise;
import nars.premise.PremiseBuilder;
import nars.task.DerivedTask;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;


/**
 * controls an active focus of concepts
 */
abstract public class FireConcepts implements Consumer<DerivedTask>, Runnable {


    public final AtomicBoolean clear = new AtomicBoolean(false);

    public final DerivationBudgeting budgeting;
    public final Deriver deriver;


    /**
     *
     */
    public final @NotNull FloatParam rate = new FloatParam(1f);
    /**
     * size of each sampled concept batch that adds up to conceptsFiredPerCycle.
     * reducing this value should provide finer-grained / higher-precision concept selection
     * since results between batches can affect the next one.
     */
    public final MutableIntRange taskLinksFiredPerConcept = new MutableIntRange(1, 1);
    public final MutableIntRange termLinksFiredPerTaskLink = new MutableIntRange(1, 1);

    //    public final MutableInteger derivationsInputPerCycle;
//    this.derivationsInputPerCycle = new MutableInteger(Param.TASKS_INPUT_PER_CYCLE_MAX);
    protected final NAR nar;
    private final On on;
    public final Focus source;

//    class PremiseVectorBatch implements Consumer<BLink<Concept>>{
//
//        public PremiseVectorBatch(int batchSize, NAR nar) {
//            nar.focus().sample(batchSize, c -> {
//                if (premiseVector(nar, c.get(), FireConcepts.this)) return true; //continue
//
//                return true;
//            });
//        }
//
//        @Override
//        public void accept(BLink<Concept> conceptBLink) {
//
//        }
//    }

    /**
     * returns # of derivations processed
     */
    int premiseVector(PLink<Concept> pc, Derivation d) {

        Concept c = pc.get();
        float cPri = pc.priSafe(0);
        int numLinksSqr = taskLinksFiredPerConcept.lerp(cPri); //TODO see if there is a sqr/sqrt relationship that can be made

        List<PLink<Task>> tasklinks = c.tasklinks().commit().sampleToList(numLinksSqr);
        List<PLink<Term>> termlinks = c.termlinks().commit().sampleToList(numLinksSqr);
        final int[] count = {0};

        long now = nar.time();
        for (int i = 0, tasklinksSize = tasklinks.size(); i < tasklinksSize; i++) {
            PLink<Task> tasklink = tasklinks.get(i);
            for (int i1 = 0, termlinksSize = termlinks.size(); i1 < termlinksSize; i1++) {
                Premise p = PremiseBuilder.premise(c, tasklink, termlinks.get(i1), now, nar, -1f);
                if (p != null) {
                    if (deriver.test(d.restart(p, Param.UnificationTTL)))
                        count[0]++;
                }
            }
        }

        return count[0];

    }


    /**
     * directly inptus each result upon derive, for single-thread
     */
    public static class FireConceptsDirect extends FireConcepts {

        private MutableInteger maxInputTasksPerDerivation = new MutableInteger(-1);

        public FireConceptsDirect(Deriver deriver, DerivationBudgeting budgeting, @NotNull NAR nar) {
            this(nar.focus(), deriver, budgeting, nar);
        }

        public FireConceptsDirect(Focus focus, Deriver deriver, DerivationBudgeting budgeting, @NotNull NAR nar) {
            super(focus, deriver, budgeting, nar);
        }

        @Override
        public void fire() {
            ConceptBagFocus csrc = (ConceptBagFocus) source;
            int count = /*Math.min(csrc.active.size(), */(int) Math.ceil(rate.floatValue() * csrc.active.capacity());
            if (count == 0)
                return; //idle

            if (nar.exe.concurrent()) {
                int remain = count;
                float granularity = 8;
                float batchDivisor = nar.exe.concurrency() * granularity;
                int batchSize = (int) Math.ceil(remain / batchDivisor);
                while (remain > 0) {
                    int nextBatchSize = Math.min(remain, batchSize);
                    nar.runLater(() -> {

                        fire(csrc, nextBatchSize);

                    });
                    remain -= nextBatchSize;
                }

            } else {
                fire(csrc, count);
            }
        }

        private final ThreadLocal<MyDerivation> derivation = ThreadLocal.withInitial(() ->
                new MyDerivation(budgeting, nar));

        private class MyDerivation extends Derivation {
            final Map<Task, Task> buffer = new LinkedHashMap();

            public MyDerivation(DerivationBudgeting b, NAR nar) {
                super(nar, b, Param.UnificationStackMax, 0);
            }

            @Override
            public void derive(Task x) {
                buffer.merge(x, x, (prev, next) -> {
                    if (prev != null) {
                        PriMerge.max.merge(prev, next);
                        return prev;
                    } else {
                        return next;
                    }
                });
            }

            void commit(int derivations) {
                if (!buffer.isEmpty()) {
                    int mPerDeriv = maxInputTasksPerDerivation.intValue();
                    int max = mPerDeriv * derivations;
                    if (mPerDeriv==-1 || buffer.size() <= max) {
                        nar.input(buffer.values());
                    } else {
                        nar.input(top(buffer, max));
                    }


                    buffer.clear();
                }
            }
        }

        static Iterable<Task> top(Map<Task, Task> m, int max) {
            SortedArray<Task> sa = new SortedArray<>(new Task[max]);
            m.values().forEach(x -> {
                if (sa.size() >= max) {
                    if (sa.last().pri() > x.pri()) {
                        return; //too low priority
                    } else {
                        sa.removeLast(); //remove current last
                    }
                }
                sa.add(x, z -> -z.pri());
//                while (sa.size() > max)
//                    sa.removeLast();
            });
            assert(sa.size() <= max);
            return sa;
        }

        //long start = nanoTime();
        //long end = nanoTime();
        //double dt = (end - start) / ((float)nextBatchSize);
        //rate.hitNano(dt);

        public void fire(ConceptBagFocus csrc, int count) {
            MyDerivation d = derivation.get();

            final int[] derivations = {0};
            csrc.active.sample(count, p -> {
                derivations[0] += premiseVector(p, d);
            });
            d.commit(derivations[0]);
        }

        @Override
        public void accept(DerivedTask derivedTask) {
            nar.input(derivedTask);
        }

    }

    public FireConcepts(@NotNull Focus source, Deriver dderiver, DerivationBudgeting bbudgeting, NAR nar) {

        this.deriver = dderiver;
        this.budgeting = bbudgeting;

        this.nar = nar;
        this.source = source;

        this.on = nar.onCycle(this);
    }

    @Override
    public void run() {
        ConceptBagFocus f = (ConceptBagFocus) this.source;

        //while clear is enabled, keep active clear
        if (clear.get()) {
            f.active.clear();
            clear.set(false);
        } else {
            f.active.commit();
        }

        fire();
    }

    abstract protected void fire();
}
//    class PremiseMatrixBatch implements Consumer<NAR> {
//        private final int _tasklinks;
//        private final int batchSize;
//        private final MutableIntRange _termlinks;
//
//        public PremiseMatrixBatch(int batchSize, int _tasklinks, MutableIntRange _termlinks) {
//            this.batchSize = batchSize;
//            this._tasklinks = _tasklinks;
//            this._termlinks = _termlinks;
//        }
//
//        @Override
//        public void accept(NAR nar) {
//            source.sample(batchSize, c -> {
//                premiser.newPremiseMatrix(c.get(),
//                        _tasklinks, _termlinks,
//                        FireConcepts.this, //input them within the current thread here
//                        nar
//                );
//                return true;
//            });
//        }
//
//    }
