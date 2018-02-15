package nars.derive.time;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import jcog.TODO;
import jcog.Util;
import jcog.data.graph.ImmutableDirectedEdge;
import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.search.Search;
import jcog.list.Cons;
import jcog.list.FasterList;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.atom.Bool;
import org.apache.commons.math3.exception.MathArithmeticException;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.Op.CONJ;
import static nars.Op.IMPL;
import static nars.derive.time.TimeGraph.TimeSpan.TS_ZERO;
import static nars.time.Tense.*;

/**
 * represents a multigraph of events and their relationships
 * calculates unknown times by choosing from the possible
 * pathfinding results.
 * <p>
 * it can be used in various contexts:
 * a) the tasks involved in a derivation
 * b) as a general purpose temporal index, ie. as a meta-layer
 * attached to one or more concept belief tables
 * <p>
 * DTERNAL relationships can be maintained separate
 * from +0.
 */
public class TimeGraph extends MapNodeGraph<TimeGraph.Event, TimeGraph.TimeSpan> {

    private static final boolean dternalAsZero = false;
    private static final boolean autoUnneg = true;

    static class TimeSpan {
        public final long dt;
        //public final float weight;

        public final static TimeSpan TS_ZERO = new TimeSpan(0);
        //        public final static TimeSpan TS_POS_ONE = new TimeSpan(+1);
//        public final static TimeSpan TS_NEG_ONE = new TimeSpan(-1);
        public final static TimeSpan TS_ETERNAL = new TimeSpan(ETERNAL);

        public static TimeSpan the(long dt) {
            assert (dt != TIMELESS);
            assert (dt != XTERNAL) : "probably meant to use TIMELESS";
            assert (dt != DTERNAL) : "probably meant to use ETERNAL";

            if (dt == 0) {
                return TS_ZERO;
            } else if (dt == ETERNAL) {
                return TS_ETERNAL;
            } else {
                return new TimeSpan(dt);
            }
        }

        private TimeSpan(long dt) {
            this.dt = dt;
            //this.weight = weight;
            //this.hash = Long.hashCode(dt);
        }

        @Override
        public int hashCode() {
            return Long.hashCode(dt);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || ((obj instanceof TimeSpan && dt == ((TimeSpan) obj).dt));
        }

        @Override
        public String toString() {
            return (dt == ETERNAL ? "~" : (dt >= 0 ? ("+" + dt) : ("-" + (-dt))));
            //+ (weight != 1 ? "x" + n2(weight) : "");
        }
    }

    protected Random random() {
        return ThreadLocalRandom.current();
    }

    /**
     * index by term
     */
    final Multimap<Term, Event> byTerm = MultimapBuilder
            .linkedHashKeys()
            .linkedHashSetValues() //maybe use TreeSet values and order them by best first
            .build();


    public Event know(Term t) {
        return know(t, TIMELESS);
    }

//    public void know(Absolute a) {
//        know(a.id, a.when);
//    }

    public Event know(Term t, long start) {
        return event(t, start, true);
    }

//    /**
//     * negate if negated, for precision in discriminating positive/negative
//     */
//    static Term polarizedTaskTerm(Task t) {
//        Truth tt = t.truth();
//        return t.term().negIf(tt != null && tt.isNegative());
//    }


    public Event event(Term t, long start) {
        return event(t, start, false);
    }

    public Event event(Term t, long when, boolean add) {
        if (t instanceof Bool)
            return null;


        Event e;
        if (when != TIMELESS) {
            e = new Absolute(t, when);
        } else {
            e = new Relative(t);
        }

        return add ? addNode(e).id : event(e);
    }

    //    protected Event absolute(Term t) {
//        for (Event tx : byTerm.get(t)) {
//            if (tx instanceof Absolute)
//                return tx; //already known absolute
//        }
//        return null;
//    }
    protected Event absolute(Term t) {
        //TODO test for multiple options
        List<Event> aa = new FasterList(1);
        for (Event tx : byTerm.get(t)) {
            if (tx instanceof Absolute) {
                aa.add(tx);
            }
        }
        int as;
        switch (as = aa.size()) {
            case 0:
                return null;
            case 1:
                return aa.get(0);
            default:
                return aa.get(random().nextInt(as));
        }
    }

    protected int absoluteCount(Term t) {
        int c = 0;
        for (Event tx : byTerm.get(t)) {
            if (tx instanceof Absolute)
                c++;
        }
        return c;
    }

    public Event event(Event e) {
        Node<nars.derive.time.TimeGraph.Event,nars.derive.time.TimeGraph.TimeSpan> existing = node(e);
        return existing != null ? existing.id : e;
    }


    public boolean link(Event before, TimeSpan e, Event after) {
        return addEdge(addNode(before), e, addNode(after));
    }

    public void link(@NotNull Event x, long dt, @NotNull Event y) {

        if ((x == y || ((x.id.equals(y.id)) && (x.when() == y.when()))))
            return; //loop

        boolean swap = false;
//        if (dt == ETERNAL || dt == TIMELESS || dt == 0) {
        //lexical order

//        if (dt == 0 && x.id.unneg().equals(y.id/*.unneg()*/))
//            return; //throw new RuntimeException("instantaneous self loop");

        int vc = Integer.compare(x.id.volume(), y.id.volume());
        if (vc == 0) {

            if (x.hashCode() > y.hashCode()) { //TODO write real comparator
                swap = true;
            }
        } else if (vc > 0) {
            swap = true;
        }

        if (swap) {
            if (dt != ETERNAL && dt != TIMELESS && dt != 0) {
                dt = -dt;
            }
            Event z = x;
            x = y;
            y = z;
        }

        link(x, TimeSpan.the(dt), y);
    }

    @Override
    protected void onAdd(Node<nars.derive.time.TimeGraph.Event,nars.derive.time.TimeGraph.TimeSpan> x) {
        Event event = x.id;
        Term eventTerm = event.id;

        if (byTerm.put(eventTerm, event)) {


            if (autoUnneg) {
                //link(event, 0, know(eventTerm.neg())); //WEAK
                link(know(eventTerm), 0, know(eventTerm.neg())); //WEAK
            }
        }

//        Term tRoot = eventTerm.root();
//        if (!tRoot.equals(eventTerm))
//            byTerm.put(tRoot, event);

        int edt = eventTerm.dt(), eventDT;
        if (edt == DTERNAL && dternalAsZero)
            eventDT = 0;
        else
            eventDT = edt;


        switch (eventTerm.op()) {
//            case INH:
//                @Nullable DynamicTruthModel dmt = DefaultConceptBuilder.unroll(eventTerm); //TODO optimize
//                if (dmt != null) {
//                    Term[] c = dmt.components(eventTerm);
//                    if (c != null && c.length > 1) {
//                        for (Term cc : c) {
//                            link(know(cc), 0, event);
//                        }
//                    }
//                }
//                break;

//            case NEG:
//                if (autoUnneg)
//                    link(know(eventTerm.unneg()), 0, event); //lower priority?
//                break;
            case IMPL:

                Term subj = eventTerm.sub(0);
                Event se = know(subj);
                Term pred = eventTerm.sub(1);
                Event pe = know(pred);
                if (eventDT == DTERNAL) {

                    link(se, ETERNAL, pe);

                    subj.eventsWhile((w, y) -> {
                        link(know(y), ETERNAL, pe);
                        return true;
                    }, 0, true, true, false, 0);

                    pred.eventsWhile((w, y) -> {
                        link(se, ETERNAL, know(y));
                        return true;
                    }, 0, true, true, false, 0);

                } else if (eventDT != XTERNAL) {

                    int st = subj.dtRange();


                    link(se, (eventDT + st), pe);

                    subj.eventsWhile((w, y) -> {
                        link(know(y), eventDT + st - w, pe);
                        return true;
                    }, 0, true, true, false, 0);

                    pred.eventsWhile((w, y) -> {
                        link(se, eventDT + st + w, know(y));
                        return true;
                    }, 0, true, true, false, 0);

                }

                //link(se, 0, event); //WEAK

                break;
            case CONJ:
                //Subterms tt = eventTerm.subterms();


                //int s = tt.subs();
//                if (et == TIMELESS) {
//                    //chain the sibling subevents
//                    if (s == 2) {
//                        Term se0 = tt.sub(0);
//                        Event e0 = know(se0);
//                        Term se1 = tt.sub(1);
//                        Event e1 = know(se1);
//                        int dt;
//                        Event earliest;
//                        if (eventDT == DTERNAL) {
//                            dt = DTERNAL;
//                            earliest = e0; //just use the first by default
//                        } else {
//                            long t0 = eventTerm.subTime(se0);
//                            long t1 = eventTerm.subTime(se1);
//                            long ddt = (int) (t1 - t0);
//                            assert (ddt < Integer.MAX_VALUE);
//                            dt = (int) ddt;
//                            earliest = t0 < t1 ? e0 : e1;
//                        }
//                        link(e0, dt, e1);
//                        link(earliest, 0, event);
//
//                    } else {
//                        throw new TODO();
//                    }
//
//                } else

                //locate the events and sub-events absolutely
                long et = event.when();

                switch (eventDT) {
                    case XTERNAL:
                        break;

                    case DTERNAL:

                        Subterms es = eventTerm.subterms();
                        int esn = es.subs();
                        Term prev = es.sub(0);
                        for (int i = 1; i < esn; i++) { //dternal chain
                            Term next = es.sub(i);
                            link(knowComponent(et, 0, prev), ETERNAL, knowComponent(et, 0, next));
                            prev = next;
                        }

                        break;
                    case 0:

                        //  eventTerm.subterms().forEach(this::know); //TODO can these be absolute if the event is?
                        boolean timed = et != ETERNAL;
                        for (Term s : eventTerm.subterms()) {
                            Event t = eventDT == 0 ?
                                    knowComponent(et, 0, s) : //0
                                    (timed ? know(s, et) :  //DTERNAL and TIMED
                                            know(s));
                            if (t!=null) {
                                link(event, (eventDT == 0 || timed) ? 0 : ETERNAL,
                                        t //DTERNAL and TIMELESS
                                );
                            } else  {
                                //WHY?
                            }
                        }
                        break;
                    default:

                        eventTerm.eventsWhile((w, y) -> {

                            link(event, w, knowComponent(et, w, y));

                            return true;
                        }, 0, false, false, false, 0);
                        break;
                }

                break;
        }

    }

    private Event knowComponent(long et, long w, Term y) {
        return (et != TIMELESS) ?
                know(y, (et == ETERNAL) ? ETERNAL : (w + et)) :
                know(y);
    }


    boolean solveDT(Term x, Predicate<Event> each) {
        assert (x.dt() == XTERNAL);

        Subterms xx = x.subterms();
//        FasterList<Event> events = new FasterList<>(byTerm.get(x.root()));
//        for (int i = 0, eventsSize = events.size(); i < eventsSize; i++) {
//            Event r = events.get(i);
//            if (r instanceof Absolute) {
//                if (r.id.subterms().equals(xx)) {
//                    if (!each.test(r))
//                        return false; //done
//                }
//            }
//
//        }


        int subs = xx.subs();
        if (subs == 2) {
            Term a = xx.sub(0);
            Term b = xx.sub(1);

            boolean aEqB = a.equals(b);

            if (!a.hasXternal() && !b.hasXternal() && (aEqB || !commonSubEventsWithMultipleOccurrences(a, b))) {
                UnifiedSet<Event> ae = new UnifiedSet(2);
                solveOccurrence(event(a, TIMELESS), ax -> {
                    if (ax instanceof Absolute) ae.add(ax);
                    return true;
                });
                int aes = ae.size();
                if (aes > 0) {
                    if (aEqB) {

                        //same term, must have >1 absolute timepoints
                        if (aes > 1) {

                            Event[] ab = ae.toArray(new Event[aes]);
                            //Arrays.sort(ab, Comparator.comparingLong(Event::when));
                            for (int i = 0; i < ab.length; i++) {
                                Event abi = ab[i];
                                for (int j = 0; j < ab.length; j++) {
                                    if (i==j) continue;
                                    if (!solveDT(x, abi.when(), dt(x, abi, ab[j]), each))
                                        return false;
                                }
                            }
                        }

                    } else {
                        UnifiedSet<Event> be = new UnifiedSet(2);
                        solveOccurrence(event(b, TIMELESS), bx -> {
                            if (bx instanceof Absolute) be.add(bx);
                            return true;
                        });
                        int bes = be.size();
                        if (bes > 0) {
                            //search only if one to N; there may be incorrect possibilities among N to N comparisons
                            //if (aes == 1 || bes == 1) {
                            if (!ae.allSatisfy(ax ->
                                    be.allSatisfyWith((bx, axx) ->
                                            solveDT(x, axx.when(), dt(x, axx, bx), each), ax)))
                                return false;
                            //}
                        }
                    }
                }
            }


//            UnifiedSet<Event>[] abs = new UnifiedSet[2]; //exact occurrences of each subterm


            FasterList<Event> rels = new FasterList<>(4);
            int[] phase = new int[]{0};
            Consumer<Event> collect = z -> {
//                int p = phase[0];
//                if (z instanceof Absolute) {
//                    if (abs[p] == null) abs[p] = new UnifiedSet(2);
//                    abs[p].add(z);
//                    //}
//                }
                    rels.add(z);
            };

            byTerm.get(a).forEach(collect);
//            if (abs[0] == null)
//                byTerm.get(a.neg()).forEach(collect); //if nothing, look for negations

            if (aEqB) {
//                abs[1] = abs[0];
            } else {
                phase[0] = 1;
                byTerm.get(b).forEach(collect);
//                if (abs[1] == null)
//                    byTerm.get(b.neg()).forEach(collect);  //if nothing, look for negations
            }

//            if (abs[0] != null && abs[1] != null) {
                //known exact occurrences for both subterms
                //iterate all possibilities
                //TODO order in some way
                //TODO other simple cases: 1 -> N
//                if (abs[0].size() == 1 && abs[1].size() == 1) {
//                    //simple case:
//                    Event aa = abs[0].iterator().next();
//                    Event bb = abs[1].iterator().next();
//                    if (!solveDT(x, each, aa, bb))
//                        return false;
//                } else {
//                    if (!abs[0].allSatisfy(ae ->
//                            abs[1].allSatisfyWith((be, aaee) ->
//                                    solveDT(x, each, aaee, be), ae)))
//                        return false;
//                }

//            }


            int ns = rels.size();
            if (ns > 0) {

                if (ns > 1) {
                    //sort by volume
                    rels.sortThisByInt(s -> s.id.volume());

                }

                //            boolean repeat = a.unneg().equals(b.unneg()); //if true, then we must be careful when trying this in a commutive-like result which would collapse the two terms

                return bfs(rels, new CrossTimeSolver() {
                    @Override
                    protected boolean next(BooleanObjectPair<ImmutableDirectedEdge<Event, TimeSpan>> move, Node<nars.derive.time.TimeGraph.Event,nars.derive.time.TimeGraph.TimeSpan> next) {

                        //System.out.println(path);

                        long[] startDT = pathDT(next, a, b, path);
                        if (startDT == null)
                            return true; //nothing at this step

                        long start = startDT[0];
                        long ddt = startDT[1];
                        return TimeGraph.this.solveDT(x, start, ddt, each);
                    }
                });

            }

//        } else {
//            assert (x.op() == CONJ);
//            List<LongObjectPair<Term>> when = $.newArrayList();
//            for (int ix = 0; ix < subs; ix++) {
//                //assert(!z.hasXternal());
//                solveOccurrence(event(xx.sub(ix), TIMELESS), (ze) -> {
//                    if (ze.when() == TIMELESS)
//                        return true; //keep trying
//                    when.add(pair(ze.when(), ze.id));
//                    return false; //just one, for now //TODO see if there are any others
//                });
//            }
//            if (when.size() == subs) {
//                when.sort(Comparator.comparingLong(LongObjectPair::getOne));
//                long base = when.get(0).getOne();
//                Term zz = when.get(0).getTwo();
//                for (int i = 1; i < subs; i++) {
//                    LongObjectPair<Term> wgi = when.get(i);
//                    zz = Op.conjMerge(zz, 0, wgi.getTwo(), wgi.getOne() - base);
//                    if (zz instanceof Bool)
//                        return true; //failure
//                }
//                return each.test(event(zz, base));
//            }
        }


//        if (x.subs() == 2) {
//            Term a = xx.sub(0);
//            Set<Event<Term>> ae = absolutes(a);
//            if (!ae.isEmpty()) {
//                Term b = xx.sub(1);
//                Set<Event<Term>> be = absolutes(b);
//                if (!be.isEmpty()) {
//                    //cartesian product of the two, maybe prioritized by least distance?
//                    LazyIterable<Pair<Event<Term>, Event<Term>>> matrix = Sets.cartesianProduct(ae, be);
//                    matrix.allSatisfy(ab -> {
//
//                        long bt = ab.getTwo().start();
//                        long at = ab.getOne().end();
//
//                        int dt;
//                        if (bt == ETERNAL || at == ETERNAL) {
//                            dt = DTERNAL;
//                        } else {
//                            long ddt = bt - at;
//                            assert (Math.abs(ddt) < Integer.MAX_VALUE);
//                            dt = (int) ddt;
//                        }
//
//                        Term tt = x.dt(dt);
//                        if (tt.op().conceptualizable)
//                            return each.test(tt);
//                        else
//                            return true;
//                    });
//                }
//            }
//        }

        return each.test(event(x, TIMELESS)); //last resort
    }

    /** tests whether the two terms refer to the same sub-events,
     *  which have known multiple occurrences
     *  which would cause incorrect results if interpreted literally
     *  this prevents separate instances of events from being welded together or arranged in the incorrect temporal order
     *  across time when there should be some non-zero dt */
    boolean commonSubEventsWithMultipleOccurrences(Term a, Term b) {
        UnifiedSet<Term> eventTerms = new UnifiedSet();
        a.eventsWhile((w,aa)->{
            eventTerms.add(aa);
           return true;
        }, 0, true, true, true, 0);

        final boolean[] ambiguous = {false};
        b.eventsWhile((w,bb)->{
            if (eventTerms.remove(bb)) {
                if (absoluteCount(bb) > 1) {
                    ambiguous[0] = true;
                    return false;
                }
            }
            return true;
        }, 0, true, true, true, 0);

        return ambiguous[0];
    }

    /**
     * since CONJ will be constructed with conjMerge, if x is conj the dt between events must be calculated from start-start. otherwise it is implication and this is measured internally
     */
    static long dt(Term x, Event aa, Event bb) {

        long aWhen = aa.when();
        long bWhen;
        if (aWhen == ETERNAL || (bWhen = bb.when()) == ETERNAL)
            return DTERNAL;
        else {
            assert (aWhen != XTERNAL);
            assert (bWhen != XTERNAL);
            return bWhen - aWhen;
        }
        //return bb.start() - (x.op() == CONJ ? aa.start() : aa.end());
    }

    private boolean solveDT(Term x, long start, long ddt, Predicate<Event> each) {
        assert (ddt != TIMELESS && ddt != XTERNAL);
        int dt;
        if (ddt == ETERNAL) {
            dt = DTERNAL;
        } else {
            assert (ddt < Integer.MAX_VALUE) : ddt + " dt calculated";
            dt = (int) ddt;
        }
        Term y = dt(x, dt);

        if (y instanceof Bool)
            return true;

        if (start != ETERNAL && start != TIMELESS && dt != DTERNAL && dt < 0 && y.op() == CONJ) {
            start += dt; //shift to left align
        }

        return start != TIMELESS ?
                each.test(
                        event(y, start, false)
                )
                :
                solveOccurrence(event(y, TIMELESS), each);


    }

    /**
     * preprocess the dt used to construct a new term.
     * ex: dithering
     */
    protected Term dt(Term x, int dt) {

        assert(dt!=XTERNAL);

        if (dt == DTERNAL) {
            return x.dt(DTERNAL);
        }

        //CONSTRUCT NEW TERM
        Op xo = x.op();
        if (xo == IMPL) {
            return x.dt(dt != XTERNAL ? dt - x.sub(0).dtRange() : dt);
        } else if (xo == CONJ) {
            int early = Op.conjEarlyLate(x, true);
            if (early == 1)
                dt = -dt;

            Term xEarly = x.sub(early);
            Term xLate = x.sub(1 - early);

            return Op.conjMerge(
                    xEarly, 0,
                    xLate, dt);
        } else {
            //?
        }


        throw new UnsupportedOperationException();
    }


//    final static LongSet EMPTY_LONG_SET = LongSets.immutable.empty();

//    public void solve(Term x, Predicate<Event> each) {
//        solve(x, true, each);
//    }

    /** main entry point to the solver */
    public final void solve(Term x, boolean filterTimeless, Predicate<Event> target) {
        solve(x, filterTimeless, new HashSet<>(), target);
    }

    /** main entry point to the solver
     * @seen callee may need to clear the provided seen if it is being re-used
     * */
    public void solve(Term x, boolean filterTimeless, HashSet<Event> seen, Predicate<Event> target) {

        Predicate<Event> each = y -> {
            if (seen.add(y)) {
//                if (!x.equalsRoot(y.id))
//                    return true; //potentially degenerate solution
                if (y.when() == TIMELESS && (filterTimeless || x.equals(y.id)))
                    return true;

                return target.test(y);
            } else {
                return true; //filtered
            }
        };

        //test for existing exact solutions to the exact term
        boolean kontinue = solveExact(x, each) && solveAll(x, each);
        //each.test(event(x, TIMELESS)); //as a last resort: does this help?


    }

    private boolean solveExact(Term x, Predicate<Event> each) {
        for (Event e : byTerm.get(x)) {
            if (e instanceof Absolute && !each.test(e))
                return false;
        }
        return true;
    }


    /**
     * each should only receive Event or Unsolved instances, not Relative's
     */
    boolean solveAll(Term x, Predicate<Event> each) {

        //collect XTERNAL terms that will need to be solved
        if (x.hasAny(Op.Temporal)) {
            final TreeSet<Term> xternalsToSolve = new TreeSet<>();
            if (x.dt() == XTERNAL)
                xternalsToSolve.add(x);

            x.subterms().recurseTerms(Term::isTemporal, y -> {
                if (y.dt() == XTERNAL)
                    xternalsToSolve.add(y);
                return true;
            }, null);

            int xs = xternalsToSolve.size();


            //solve the XTERNAL from simplest to most complex. the canonical sort order of compounds will naturally descend this way
            if (xs > 0 && !Util.andReverse(xternalsToSolve.toArray(new Term[xs]), u -> solveDT(u, v -> {

                        Term y;
                        if (!u.equals(v.id)) {
                            y = x.replace(u, v.id);
                            if (y == null || y instanceof Bool)
                                return true; //continue
                        } else {
                            y = x;
                        }

                        boolean ye = y.equals(x);
                        if (!ye) {

                            if (!solveExact(y, each))
                                return false;

                            boolean yx = y.hasXternal();
                            if (u.equals(x) && !yx) {
                                if (v.when() != TIMELESS) {
                                    return each.test(v); //shortcut to finish
                                } else {
                                    return solveOccurrence(v, each); //only need to solve occurrence
                                }
                            }

                            return yx ?
                                    solveAll(y, each) : //recurse to solve remaning xternal's
                                    solveOccurrence(event(y, TIMELESS), each); //just need to solve occurrence now
                        } else {
                            //term didnt change...
                            if (v.when() != TIMELESS) {
                                if (!each.test(v)) //but atleast it somehow solved an occurrence time
                                    return false;
                            }
                        }


                        return true; //keep trying
                    })
            ))
                return false;
        }


        return solveOccurrence(event(x, TIMELESS), each);
    }

//    protected LinkedHashMap<Term, LongSet> absolutes(Term x) {
//        LinkedHashMap<Term, LongSet> m = new LinkedHashMap<>();
//        absolutes(x, m);
//        return m;
//    }

//    protected void absolutes(Term x, Map<Term, LongSet> absolute) {
//
//        if (absolute.putIfAbsent(x, EMPTY_LONG_SET) != null)
//            return; //already processed
//
//        switch (x.op()) {
//            case CONJ:
//                x.eventsWhile((w, xx) -> {
//                    if (x != xx)
//                        absolutes(xx, absolute);
//                    return true;
//                }, 0, true, false, true, 0);
//                break;
//            case IMPL:
//                absolutes(x.sub(0), absolute);
//                absolutes(x.sub(1), absolute);
//                break;
//            case NEG:
//                absolutes(x.unneg(), absolute);
//                break;
//        }
//
//        Collection<Event> xe = byTerm.get(x);
//        if (xe != null) {
//            LongHashSet l = new LongHashSet();
//            xe.forEach(e -> {
//                if (e.absolute())
//                    l.add(e.start());
//            });
//
//            if (!l.isEmpty()) {
//                absolute.put(x, l.toImmutable());
//            }
//        }
//
//    }


    /**
     * solves the start time for the given Unsolved event.  returns whether callee should continue iterating
     */
    private boolean solveOccurrence(Event x, Predicate<Event> each) {

        return bfs(x, new CrossTimeSolver() {
            @Override
            protected boolean next(BooleanObjectPair<ImmutableDirectedEdge<Event, TimeSpan>> move, Node<nars.derive.time.TimeGraph.Event,nars.derive.time.TimeGraph.TimeSpan> n) {

                if (n.id instanceof Absolute) {

                    long pathEndTime = n.id.when();
                    //BooleanObjectPair<Edge<Event, TimeSpan>> pathStart = path.get(0);
//                    Term pathStartTerm = pathStart.getTwo().from(pathStart.getOne()).id.id;

                    long startTime;
                    if (pathEndTime == ETERNAL) {
                        startTime = ETERNAL;
                    } else {
                        long pathStartTime = pathTime(path, false);
                        if (pathStartTime == ETERNAL)
                            startTime = ETERNAL;
                        else
                            startTime = pathEndTime - pathStartTime;
                    }

                    return each.test(event(x.id, startTime));
                }

                return true;

            }
        }) && each.test(x) /* last resort */;

    }


    abstract protected class TimeSolver extends Search<Event, TimeSpan> {


        @Nullable Iterator<ImmutableDirectedEdge<Event, TimeSpan>> dynamicLink(Node<nars.derive.time.TimeGraph.Event,nars.derive.time.TimeGraph.TimeSpan> n) {
            return dynamicLink(n, x -> true);
        }

        @Nullable Iterator<ImmutableDirectedEdge<Event, TimeSpan>> dynamicLink(Node<nars.derive.time.TimeGraph.Event,nars.derive.time.TimeGraph.TimeSpan> n, Predicate<Event> preFilter) {
            Iterator<Event> x = byTerm.get(n.id.id).iterator();
            return x.hasNext() ? Iterators.transform(Iterators.filter(Iterators.transform(
                    Iterators.filter(x, preFilter::test),
                    TimeGraph.this::node),
                    e -> e != n && !log.hasVisited(e)),
                    that -> new ImmutableDirectedEdge<>(n, that, TS_ZERO) //co-occurring
            ) : null;
        }


//        public long startTime(FasterList<BooleanObjectPair<Edge<Event<Term>, TimeSpan>>> path) {
//            BooleanObjectPair<Edge<Event<Term>, TimeSpan>> firstStep = path.get(0);
//            boolean outOrIn = firstStep.getOne();
//            return firstStep.getTwo().from(outOrIn).id.start();
//        }

        /**
         * computes the length of time spanned from start to the end of the given path
         */
        long pathTime(List<BooleanObjectPair<ImmutableDirectedEdge<Event, TimeSpan>>> path, boolean eternalAsZero) {

            long t = 0;
            //compute relative path
            for (BooleanObjectPair<ImmutableDirectedEdge<Event, TimeSpan>> r : path) {
//            for (int i = 0, pathSize = path.size(); i < pathSize; i++) {
//                BooleanObjectPair<Edge<Event, TimeSpan>> r = path.get(i);
                ImmutableDirectedEdge<Event, TimeSpan> event = r.getTwo();

                long spanDT = event.id.dt;

                if (spanDT == ETERNAL) {
                    //no change, crossed a DTERNAL step. this may signal something
                    if (!eternalAsZero)
                        return ETERNAL; //short-circuit to eternity
                    //else: continue

                } else if (spanDT != 0) {
                    t += (spanDT) * (r.getOne() ? +1 : -1);
                }

            }

            return t;
        }

        protected Event pathStart(List<BooleanObjectPair<ImmutableDirectedEdge<Event, TimeSpan>>> path) {
            BooleanObjectPair<ImmutableDirectedEdge<Event, TimeSpan>> step = path.get(0);
            return step.getTwo().from(step.getOne()).id;
        }

        protected Event pathEnd(List<BooleanObjectPair<ImmutableDirectedEdge<Event, TimeSpan>>> path) {
            BooleanObjectPair<ImmutableDirectedEdge<Event, TimeSpan>> step = path instanceof Cons ?
                    ((Cons<BooleanObjectPair<ImmutableDirectedEdge<Event,TimeSpan>>>)path).tail : path.get(path.size()-1);
            return step.getTwo().to(step.getOne()).id;
        }

        /**
         * assuming the path starts with one of the end-points (a and b),
         * if the path ends at either one of them
         * this computes the dt to the other one,
         * and (if available) the occurence startTime of the path
         * returns (startTime, dt) if solved, null if dt can not be calculated.
         */
        @Nullable
        protected long[] pathDT(Node<nars.derive.time.TimeGraph.Event,nars.derive.time.TimeGraph.TimeSpan> n, Term a, Term b, List<BooleanObjectPair<ImmutableDirectedEdge<Event, TimeSpan>>> path) {
            Term endTerm = n.id.id;
            int adjEnd;
            boolean endA = ((adjEnd = a.subTimeSafe(endTerm))==0); //TODO use offset for the endTerm if endTermRelB!=0 and !=DTERNAL (not sub event)
            boolean endB = !endA &&
                    ((adjEnd = b.subTimeSafe(endTerm))==0); //TODO use offset for the endTerm if endTermRelB!=0 and !=DTERNAL (not sub event)

            if (adjEnd == DTERNAL)
                return null;

            if (endA || endB) {
                Event startEvent = pathStart(path);

                Term startTerm = startEvent.id;

                boolean fwd = endB && (startTerm.equals(a)||a.subTimeSafe(startTerm)==0);
                boolean rev = !fwd && (
                        endA && (startTerm.equals(b)||b.subTimeSafe(startTerm)==0));//TODO use offset for the endTerm if endTermRelB!=0 and !=DTERNAL (not sub event)
                if (fwd || rev) {

                    long startTime = startEvent.when();

                    Event endEvent = pathEnd(path);
                    long endTime = endEvent.when();


                    long dt;
                    if (startTime != TIMELESS && startTime != ETERNAL && endTime != TIMELESS && endTime != ETERNAL) {
                        //use the two endpoints and subtract the dt

                        dt = endTime - startTime;
                    } else {

                        //TODO more rigorous traversal of the dt chain
                        //compute from one end to the other, summing dt in the correct direction along the way
                        //special handling for encountered absolute terms and DTERNAL

                        dt = pathTime(path, true);
                    }
                    if (dt == TIMELESS)
                        return null;

                    if (dt == ETERNAL) {
                        long w;
                        if (startTime == TIMELESS) {
                            w = endTime;
                        } else {
                            if (startTime == ETERNAL)
                                w = endTime;
                            else {
                                w = startTime;
                            }
                        }

                        return new long[]{w, ETERNAL};
                    } else {

                        if (a.equals(b))
                            rev = random().nextBoolean(); //equal chance for each direction

                        if (rev) {
                            dt = -dt; //reverse
                            long s = startTime;
                            startTime = endTime;
                            endTime = s;
                        }

                        //TODO may need to subtract from dt any inner events with dtRange


                        return new long[]{
                                (startTime != TIMELESS || endTime == TIMELESS) ?
                                        startTime :
                                        (endTime != ETERNAL ? endTime - dt : ETERNAL)
                                , dt};
                    }
                }
            }
            return null;
        }

    }


    /**
     * supplies additional virtual edges to other points in time for the given node
     */
    abstract protected class CrossTimeSolver extends TimeSolver {

        @Override
        protected Iterable<ImmutableDirectedEdge<Event, TimeSpan>> next(Node<nars.derive.time.TimeGraph.Event,nars.derive.time.TimeGraph.TimeSpan> n) {
            Iterable<ImmutableDirectedEdge<Event, TimeSpan>> e = n.edges(true, true);

            //must be cached to avoid concurrent modification exception
            Iterator<ImmutableDirectedEdge<Event, TimeSpan>> d = dynamicLink(n);

            return (d != null && d.hasNext()) ? Iterables.concat(e, new FasterList<>(d)) : e;
        }

    }

//    /**
//     * TODO not ready yet
//     */
//    protected class DTCommutiveSolver extends TimeSolver {
//        private final Set<Term> targets;
//        private final LongLongPredicate each;
//
//        public DTCommutiveSolver(Set<Term> targets, LongLongPredicate each) {
//            this.targets = targets;
//            this.each = each;
//        }
//
//        @Override
//        protected boolean visit(Node<Event, TimeSpan> n, FasterList<BooleanObjectPair<Edge<Event, TimeSpan>>> path) {
//            if (path.isEmpty())
//                return true;
//
//
//            //System.out.println(path);
//
//            //long[] dt = pathDT(n, a, b, path);
//
////            if (dt!=null)
////                if (!each.accept(dt[0], dt[1]))
////                    return false;
//
//            return true;
//        }
//    }

    /**
     * absolutely specified event
     */
    public abstract static class Event implements LongObjectPair<Term> {

        public final Term id;
        private final int hash;

        Event(@NotNull Term id, long start) {
            this.id = id;
            this.hash = Util.hashCombine(id.hashCode(), Long.hashCode(start));
        }

        abstract public long when();

        @Override
        public final int hashCode() {
            return hash;
        }

        @Override
        public final boolean equals(Object obj) {
            if (this == obj) return true;
            Event e = (Event) obj;
            return hash == e.hash && when() == e.when() && id.equals(e.id);
        }

        //        public float pri() {
//            return 1f;
//        }


        @Override
        public final String toString() {
            long s = when();

            if (s == TIMELESS) {
                return id.toString();
            } else if (s == ETERNAL) {
                return id + "@ETE";
            } else {
                return id + "@" + s;
            }
        }

        @Override
        public long getOne() {
            return when();
        }

        @Override
        public Term getTwo() {
            return id;
        }

        @Override
        public int compareTo(@NotNull LongObjectPair<Term> o) {
            throw new TODO();
        }
    }

    public static class Absolute extends Event {
        protected final long when;

        static final long SAFETY_PAD = 32 * 1024;

        protected Absolute(Term t, long when) {
            super(t, when);

            //validation
            assert (when != TIMELESS);
            if (!((when == ETERNAL || when > 0 || when > ETERNAL + SAFETY_PAD))) //for catching time calculation bugs
                throw new MathArithmeticException();
            if (!((when < 0 || when < TIMELESS - SAFETY_PAD))) //for catching time calculation bugs
                throw new MathArithmeticException();

            this.when = when;
        }

        @Override
        public final long when() {
            return when;
        }

    }


    /**
     * floating, but potentially related to one or more absolute event
     */
    public static class Relative extends Event {

        Relative(Term id) {
            super(id, TIMELESS);
        }

        @Override
        public final long when() {
            return TIMELESS;
        }

    }


}
