package nars;

import jcog.TODO;
import jcog.data.map.MRUCache;
import jcog.list.FasterList;
import jcog.math.random.XoRoShiRo128PlusRandom;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.concept.util.ConceptBuilder;
import nars.concept.util.DefaultConceptBuilder;
import nars.derive.Deriver;
import nars.derive.Derivers;
import nars.derive.rule.DeriveRuleSet;
import nars.exe.AbstractExec;
import nars.exe.Exec;
import nars.exe.UniExec;
import nars.index.term.ConceptIndex;
import nars.index.term.map.CaffeineIndex;
import nars.index.term.map.MapConceptIndex;
import nars.op.stm.STMLinkage;
import nars.term.Term;
import nars.term.Termed;
import nars.time.CycleTime;
import nars.time.RealTime;
import nars.time.Time;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * NAR builder
 */
public class NARS {

    public NAR get() {
        NAR n = new NAR(index.get(), exe.get(), time, rng.get(), concepts.get());
        init(n);
        derivers.forEach(d -> d.apply(n));
        n.synch();
        postInit.forEach(x -> x.accept(n));
        n.synch();
        return n;
    }

    /**
     * subclasses may override this to configure newly constructed NAR's
     */
    protected void init(NAR n) {

    }

    protected Supplier<ConceptIndex> index;

    protected Time time;

    protected Supplier<Exec> exe;

    protected Supplier<Random> rng;

    protected Supplier<ConceptBuilder> concepts;

    protected List<Function<NAR, Deriver>> derivers;

    /**
     * applied in sequence as final step before returning the NAR
     */
    protected final List<Consumer<NAR>> postInit = new FasterList(0);


    public NARS index(@NotNull ConceptIndex concepts) {
        this.index = () -> concepts;
        return this;
    }

    public NARS time(@NotNull Time time) {
        this.time = time;
        return this;
    }

    public NARS exe(Exec exe) {
        this.exe = () -> exe;
        return this;
    }

    public NARS concepts(ConceptBuilder cb) {
        this.concepts = () -> cb;
        return this;
    }


    /**
     * adds a deriver with the standard rules for the given range (inclusive) of NAL levels
     */
    public NARS deriverAdd(int minLevel, int maxLevel) {
        derivers.add(
                Derivers.deriver(minLevel, maxLevel)
        );
        return this;
    }

    /**
     * adds a deriver with the provided rulesets
     */
    public NARS deriverAdd(String... ruleFiles) {
        deriverAdd(
                Derivers.deriver(1, 9, ruleFiles)
        );
        return this;
    }
    public NARS deriverAddRules(String... rules) {
        deriverAdd(
                n -> new Deriver(new DeriveRuleSet(n, rules), n)
        );
        return this;
    }



    public NARS deriverAdd(Function<NAR, Deriver> dBuilder) {
        this.derivers.add(dBuilder);
        return this;
    }


    /**
     * defaults
     */
    public NARS() {

        index = () ->
                //new CaffeineIndex(new DefaultConceptBuilder(), 8*1024, 16*1024, null)
                new MapConceptIndex(
                        //new /*Linked*/HashMap(256, 0.9f)
                        new MRUCache<>(16*1024) {
                            @Override
                            protected void onEvict(Map.Entry<Term, Termed> entry) {
                                Termed c = entry.getValue();
                                if (c instanceof PermanentConcept) {
                                    throw new TODO("Should not evict " + c); //TODO reinsert
                                } else {
                                    ((Concept)c).delete(null /* HACK */);
                                }
                            }
                        }
                );

        time = new CycleTime();

        exe = () -> new UniExec(96);

        rng = () ->
                new XoRoShiRo128PlusRandom(1);

        concepts = DefaultConceptBuilder::new;

        derivers = new FasterList<>();
    }

    /**
     * temporary, disposable NAR. safe for single-thread access only.
     * full NAL8 with STM Linkage
     */
    public static NAR tmp() {
        return tmp(8);
    }


    /**
     * temporary, disposable NAR. useful for unit tests or embedded components
     * safe for single-thread access only.
     *
     * @param nal adjustable NAL level. level >= 7 include STM (short-term-memory) Linkage plugin
     */
    public static NAR tmp(int nal) {
        return new DefaultNAR(nal, false).get();
    }

    /**
     * single-thread, limited to NAL6 so it should be more compact than .tmp()
     */
    public static NAR tmpEternal() {
        return new DefaultNAR(6, false).get();
    }

    /**
     * single thread but for multithread usage:
     * unbounded soft reference index
     */
    public static NAR threadSafe() {
        return threadSafe(8);
    }
    
    public static NAR threadSafe(int level) {
        NARS d = new DefaultNAR(level, true)
                .time(new RealTime.CS().durFPS(0.01f));

        d.rng = ()->new XoRoShiRo128PlusRandom(System.nanoTime());

         return d.get();
    }


    public static NARS realtime(float durFPS) {
        return new DefaultNAR(0, true).time(new RealTime.CS().durFPS(durFPS));
    }

    /**
     * provides only low level functionality.
     * an empty deriver, but allows any kind of term
     */
    public static NAR shell() {
        return tmp(0);
    }

    public NARS memory(String s) {
        return then(n -> {
            File f = new File(s);

            try {
                n.inputBinary(f);
            } catch (FileNotFoundException ignored) {
                //ignore
            } catch (IOException e) {
                n.logger.error("input: {} {}", s, e);
            }

            Runnable save = () -> {
                try {
                    n.outputBinary(f, false);
                } catch (IOException e) {
                    n.logger.error("output: {} {}", s, e);
                }
            };
            Runtime.getRuntime().addShutdownHook(new Thread(save));
        });
    }

    /**
     * adds a post-processing step before ready NAR is returned
     */
    public NARS then(Consumer<NAR> n) {
        postInit.add(n);
        return this;
    }


    /**
     * generic defaults
     */
    @Deprecated
    public static class DefaultNAR extends NARS {


        public DefaultNAR(int nal, boolean threadSafe) {

            if (nal > 0)
                deriverAdd(1, nal);

            if (threadSafe)
                index = () -> new CaffeineIndex(64 * 1024);

            if (nal >= 7) {
                then((nn)->new STMLinkage(nn, 1, false));
            }
        }

        @Override
        protected void init(NAR nar) {

            nar.activationRate.set(
                1f/Math.sqrt(((AbstractExec)nar.exe).active.capacity())
            );

            nar.termVolumeMax.set(30);
            //nar.confMin.setValue(0.05f);

            nar.beliefPriDefault.set(0.5f);
            nar.questionPriDefault.set(0.5f);
            nar.goalPriDefault.set(0.25f);
            nar.questPriDefault.set(0.25f);

//            nar.emotion.want(Perceive, -0.1f);



        }
    }

}
