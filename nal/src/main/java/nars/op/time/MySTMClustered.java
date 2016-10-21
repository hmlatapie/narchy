package nars.op.time;

import nars.$;
import nars.NAR;
import nars.Task;
import nars.budget.BudgetFunctions;
import nars.nal.Stamp;
import nars.task.GeneratedTask;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.DefaultTruth;
import nars.truth.TruthFunctions;
import nars.util.data.MutableInteger;
import nars.util.event.DefaultTopic;
import nars.util.event.Topic;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by me on 5/22/16.
 */
public class MySTMClustered extends STMClustered {

    private static final Logger logger = LoggerFactory.getLogger(MySTMClustered.class);

    public final Topic<Task> generate = new DefaultTopic<>();

    private final int maxGroupSize;
    private final int maxInputVolume;
    private final int minGroupSize;
    private final int inputsPerFrame;

    float timeCoherenceThresh = 0.99f; //only used when not in group=2 sequence pairs phase
    float freqCoherenceThresh = 0.9f;
    float confCoherenceThresh = 0.5f;

    float confMin;


    public MySTMClustered(@NotNull NAR nar, int size, char punc, int maxGroupSize) {
        this(nar, size, punc, maxGroupSize, true, 1);
    }

    public MySTMClustered(@NotNull NAR nar, int size, char punc, int maxGroupSize, boolean allowNonInput, int intinputsPerFrame) {
        this(nar, size, punc, maxGroupSize, maxGroupSize,
                Math.round(((float) nar.compoundVolumeMax.intValue()) / (2)) /* estimate */
                , allowNonInput,
                intinputsPerFrame);
    }

    public MySTMClustered(@NotNull NAR nar, int size, char punc, int minGroupSize, int maxGroupSize, int maxInputVolume, boolean allowNonInput, int inputsPerFrame) {
        super(nar, new MutableInteger(size), punc, maxGroupSize);

        this.minGroupSize = minGroupSize;
        this.maxGroupSize = maxGroupSize;
        this.maxInputVolume = maxInputVolume;

        this.inputsPerFrame = inputsPerFrame;
        //this.logger = LoggerFactory.getLogger(toString());

        this.allowNonInput = allowNonInput;

        net.setAlpha(0.05f);
        net.setBeta(0.05f);
        net.setWinnerUpdateRate(0.03f, 0.01f);
    }


    @Override
    public void accept(@NotNull Task t) {

        if (t.punc() == punc && t.volume() <= maxInputVolume) {

            input.put(t, t.budget());
        }

    }

    @Override
    protected boolean iterate() {

        if (super.iterate()) {

            confMin = nar.confMin.floatValue();

            //LongObjectHashMap<ObjectFloatPair<TasksNode>> selected = new LongObjectHashMap<>();

            //clusters where all terms occurr simultaneously at precisely the same time
            //cluster(maxConjunctionSize, 1.0f, freqCoherenceThresh);

            cluster(((int)(inputsPerFrame * nar.exe.load())), minGroupSize, maxGroupSize);

            //clusters where dt is allowed, but these must be of length 2. process any of these pairs which remain
            //if (maxGroupSize != 2)
            //cluster(2);

            return true;
        }

        return false;
    }

    private void cluster(int limit, int minGroupSize, int maxGroupSize) {
        if (limit == 0)
            return;

        net.nodeStream()
                //.parallel()
                //.sorted((a, b) -> Float.compare(a.priSum(), b.priSum()))
                .filter(n -> {
                    if (n.size() < minGroupSize)
                        return false;

                    //TODO wrap all the coherence tests in one function call which the node can handle in a synchronized way because the results could change in between each of the sub-tests:

                    double[] tc = n.coherence(0);

                    if (tc != null && (maxGroupSize == 2 || tc[1] >= timeCoherenceThresh)) {
                        double[] fc = n.coherence(1);
                        if (fc != null && fc[1] >= freqCoherenceThresh) {
                            double[] cc = n.coherence(2);
                            if (cc != null && cc[1] >= confCoherenceThresh) {
                                return true;
                            }
                            //return true;
                        }
                    }
                    return false;
                })
                .limit(limit)
                .map(n -> PrimitiveTuples.pair(n, n.coherence(1)[0]))
                .forEach(nodeFreq -> {

                    TasksNode node = nodeFreq.getOne();
                    float freq = (float) nodeFreq.getTwo();

                    boolean negated;
                    if (freq < 0.5f) {
                        freq = 1f - freq;
                        negated = true;
                    } else {
                        negated = false;
                    }

                    float finalFreq = freq;
                    int maxVol = nar.compoundVolumeMax.intValue();
                    node.chunk(maxGroupSize, maxVol - 1).forEach(tt -> {

                        //Task[] uu = Stream.of(tt).filter(t -> t!=null).toArray(Task[]::new);

                        //get only the maximum confidence task for each term
                        Map<Term, Task> vv = new HashMap(tt.size());
                        tt.forEach(_z -> {
                            Task z = _z.get();
                            if (z != null) {
                                vv.merge(z.term(), z, (prevZ, newZ) -> {
                                    if (prevZ == null || newZ.conf() > prevZ.conf())
                                        return newZ;
                                    else
                                        return prevZ;
                                });
                            }
                        });

                        Collection<Task> uu = vv.values();

                        //float confMin = (float) Stream.of(uu).mapToDouble(Task::conf).min().getAsDouble();
                        float conf = TruthFunctions.confAnd(uu); //used for emulation of 'intersection' truth function
                        if (conf < confMin)
                            return;

                        long[] evidence = Stamp.zip(uu);

                        @Nullable Term conj = group(negated, uu);
                        if (conj.volume() > maxVol)
                            return; //throw new RuntimeException("exceeded max volume");

                        if (!(conj instanceof Compound))
                            return;


                        @Nullable double[] nc = node.coherence(0);
                        if (nc == null)
                            return;

                        long t = Math.round(nc[0]);


                        Task m = new GeneratedTask(conj, punc,
                                new DefaultTruth(finalFreq, conf)) //TODO use a truth calculated specific to this fixed-size batch, not all the tasks combined
                                .time(now, t)
                                .evidence(evidence)
                                .budget(BudgetFunctions.fund(uu, 1f / uu.size()))
                                .log("STMCluster CoOccurr");


                        //logger.debug("{}", m);
                        generate.emit(m);

                        //System.err.println(m + " " + Arrays.toString(m.evidence()));
                        nar.inputLater(m);

                        //node.tasks.removeAll(tt);


                    });


                });
    }

    @Nullable
    private static Term group(boolean negated, @NotNull Collection<Task> uuu) {


        if (uuu.size() == 2) {
            //find the dt and construct a sequence
            Task early, late;

            Task[] uu = uuu.toArray(new Task[2]);

            Task u0 = uu[0];
            Task u1 = uu[1];
            if (u0.occurrence() <= u1.occurrence()) {
                early = u0;
                late = u1;
            } else {
                early = u1;
                late = u0;
            }
            int dt = (int) (late.occurrence() - early.occurrence());


            return $.seq(
                    $.negIf(early.term(), negated),
                    dt,
                    $.negIf(late.term(), negated)
            );

        } else {

            Term[] uu = uuu.stream().map(Task::term).toArray(Term[]::new);

            if (negated)
                $.neg(uu);

            //just assume they occurr simultaneously
            return $.parallel(uu);
            //return $.secte(s);
        }
    }
}
