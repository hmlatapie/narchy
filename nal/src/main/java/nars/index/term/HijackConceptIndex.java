package nars.index.term;

import jcog.Util;
import jcog.bag.impl.hijack.PLinkHijackBag;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import nars.NAR;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.control.DurService;
import nars.index.term.map.MaplikeConceptIndex;
import nars.term.Term;
import nars.term.Termed;
import org.HdrHistogram.IntCountsHistogram;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static nars.truth.TruthFunctions.w2c;

/**
 * Created by me on 2/20/17.
 */
public class HijackConceptIndex extends MaplikeConceptIndex {

    private final PLinkHijackBag<Termed> table;
    //private final Map<Term,Termed> permanent = new ConcurrentHashMap<>(1024);

    final IntCountsHistogram conceptScores = new IntCountsHistogram(1000, 2);


    /**
     * current update index
     */
    private int visit;

    /**
     * how many items to visit during update
     */
    private final float updateRate = 1;
    private final float initial = 0.5f;
    private final float getBoost = 0.02f;
    //private final float forget = 0.05f;
    private long now;
    private int dur;
    private DurService onDur;
    //private DurService onDur;

    public HijackConceptIndex(int capacity, int reprobes) {
        super();

        this.table = new PLinkHijackBag<>(capacity, reprobes) {

            {
                resize(capacity); //immediately expand to full capacity
            }

            @NotNull
            @Override
            public Termed key(PriReference<Termed> value) {
                return value.get().term();
            }

            @Override
            protected boolean attemptRegrowForSize(int s) {
                return false;
            }

            @Override
            protected boolean replace(float incoming, PriReference<Termed> existing) {

                boolean existingPermanent = existing.get() instanceof PermanentConcept;

                if (existingPermanent) {
//                    if (incomingPermanent) {
//                        //throw new RuntimeException("unresolvable hash collision between PermanentConcepts: " + incoming.get() + " , " + existing.get());
//                        return false;
//                    }
                    return false; //automatic lose
                }
//                boolean incomingPermanent = incoming.get() instanceof PermanentConcept;
//                if (incomingPermanent)
//                    return true;
                return super.replace(incoming, existing);
            }
//
//            @Override
//            public void onRemoved( @NotNull PLink<Termed> value) {
//                assert(!(value.get() instanceof PermanentConcept));
//            }
        };

    }

    @Override
    public void init(NAR nar) {
        super.init(nar);
        onDur = DurService.on(nar, this::commit);
    }


    public void commit(NAR n) {
        table.commit();
    }

//    @Override
//    public void commit(Concept c) {
//        get(c.term(), false); //get boost
//    }

    @Override
    public @Nullable Termed get(@NotNull Term key, boolean createIfMissing) {
        @Nullable PriReference<Termed> x = table.get(key);
        if (x != null) {
            Termed y = x.get();
            if (y != null) {
                x.priAdd(getBoost);
                return y; //cache hit
            }
        }

        if (createIfMissing) {
            Termed kc = nar.conceptBuilder.apply(key, null);
            if (kc != null) {
                PriReference<Termed> inserted = table.put(new PLink<>(kc, initial));
                if (inserted != null) {
                    return kc;
//                        Termed ig = inserted.get();
//                        if (ig.term().equals(kc.term()))
//                            return ig;
                } else {
                    return null;
                }
            }
        }

        return null;
    }


    @Override
    public void set(@NotNull Term src, Termed target) {
        remove(src);
        PriReference<Termed> inserted = table.put(new PLink<>(target, 1f));
        if (inserted == null && target instanceof PermanentConcept) {
            throw new RuntimeException("unresolvable hash collision between PermanentConcepts: " + target);
        }
    }

    @Override
    public void clear() {
        table.clear();
    }

    @Override
    public void forEach(@NotNull Consumer<? super Termed> c) {
        //TODO make sure this doesnt visit a term twice appearing in both tables but its ok for now
        table.forEachKey(c);
        //permanent.values().forEach(c);
    }

    @Override
    public int size() {
        return table.size(); /** approx since permanent not considered */
    }

    @Override
    public @NotNull String summary() {
        return table.size() + " concepts"; // (" + permanent.size() + " permanent)";
    }

    @Override
    public void remove(@NotNull Term entry) {
        table.remove(entry);
        //permanent.remove(entry);
    }


//    /**
//     * performs an iteration update
//     */
//    private void update(NAR nar) {
//
//        AtomicReferenceArray<PriReference<Termed>> tt = table.map;
//
//        int c = tt.length();
//
//        now = nar.time();
//        dur = nar.dur();
//
//        int visit = this.visit;
//        try {
//            int n = (int) Math.floor(updateRate * c);
//
//            for (int i = 0; i < n; i++, visit++) {
//
//                if (visit >= c) visit = 0;
//
//                PriReference<Termed> x = tt.get(visit);
//                if (x != null)
//                    update(x);
//            }
//        } finally {
//            this.visit = visit;
////            Util.decode(conceptScores, "", 200, (x, v) -> {
////                System.out.println(x + "\t" + v);
////            });
//            conceptScores.reset();
//        }
//
//    }
//
//    protected void update(PriReference<Termed> x) {
//
//        //TODO better update function based on Concept features
//        Termed tc = x.get();
//        if (tc instanceof PermanentConcept)
//            return; //dont touch
//
//        Concept c = (Concept) tc;
//        int score = (int) (score(c) * 1000f);
//
//        float cutoff = 0.25f;
//        if (conceptScores.getTotalCount() > this.table.capacity() / 2) {
//            float percentile = (float) conceptScores.getPercentileAtOrBelowValue(score) / 100f;
//            if (percentile < cutoff)
//                forget(x, c, cutoff * (1 - percentile));
//        }
//
//        conceptScores.recordValue(score);
//    }

    @Override
    public Stream<? extends Termed> stream() {
        return table.stream().map(Supplier::get);
    }

    protected float score(Concept c) {
        float beliefConf = w2c((float) c.beliefs().streamTasks().mapToDouble(t -> t.evi(now, dur)).average().orElse(0));
        float goalConf = w2c((float) c.goals().streamTasks().mapToDouble(t -> t.evi(now, dur)).average().orElse(0));
        float talCap = c.tasklinks().size() / (1f + c.tasklinks().capacity());
        float telCap = c.termlinks().size() / (1f + c.termlinks().capacity());
        return Util.or(((talCap + telCap) / 2f), (beliefConf + goalConf) / 2f) /
                (1 + ((c.complexity() + c.volume()) / 2f) / nar.termVolumeMax.intValue());
    }

    protected void forget(PriReference<Termed> x, Concept c, float amount) {
        //shrink link bag capacity in proportion to the forget amount
        c.tasklinks().setCapacity(Math.round(c.tasklinks().capacity() * (1f - amount)));
        c.termlinks().setCapacity(Math.round(c.termlinks().capacity() * (1f - amount)));

        x.priMult(1f - amount);
    }
}
