/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.control;

import jcog.pri.Pri;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.derive.PrediTerm;
import nars.derive.time.Event;
import nars.derive.time.Temporalize;
import nars.index.term.TermIndex;
import nars.table.BeliefTable;
import nars.task.DerivedTask;
import nars.task.ITask;
import nars.term.InvalidTermException;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.subst.Unify;
import nars.term.subst.UnifySubst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static nars.Op.BELIEF;
import static nars.time.Tense.ETERNAL;

/**
 * NOTE: this currently isnt input to the NAR like ITask's are even though it inherits
 * from that superclass. this is temporary until the Premise behavior is determined
 * to be either reified or virtual (executed within a conceptfire execution only)
 * <p>
 * Defines the conditions used in an instance of a derivation
 * Contains the information necessary for generating derivation Tasks via reasoning rules.
 * <p>
 * It is meant to be disposable and should not be kept referenced longer than necessary
 * to avoid GC loops, so it may need to be weakly referenced.
 */
public class Premise extends Pri implements ITask {

    static final Logger logger = LoggerFactory.getLogger(Premise.class);

    public final Task taskLink;
    public final Term termLink;
    private final PrediTerm<Derivation> deriver;

    public Premise(@Nullable Task tasklink, @Nullable Term termlink, PrediTerm<Derivation> deriver, float pri) {
        super(pri);
        this.taskLink = tasklink;
        this.termLink = termlink;
        this.deriver = deriver;
        //this.hash = Util.hashCombine(tasklink.hashCode(), termlink.hashCode());
    }

//    @Override
//    public boolean equals(Object obj) {
//        if (this == obj) return true;
//        if (!(obj instanceof Premise)) return false;
//        Premise x = (Premise) obj;
//        if (hash != x.hash) return false;
//        return taskLink.equals(((Premise) obj).taskLink) && termLink.equals(((Premise) obj).termLink);
//    }
//
//    @Override
//    public int hashCode() {
//        return hash;
//    }

    @Override
    public String toString() {
        return taskLink + "," + termLink + "," + deriver;
    }

    /**
     * resolve the most relevant belief of a given term/concept
     * <p>
     * patham9 project-eternalize
     * patham9 depending on 4 cases
     * patham9 https://github.com/opennars/opennars2/blob/a143162a559e55c456381a95530d00fee57037c4/src/nal/deriver/projection_eternalization.clj
     * sseehh__ ok ill add that in a bit
     * patham9 you need  project-eternalize-to
     * sseehh__ btw i disabled immediate eternalization entirely
     * patham9 so https://github.com/opennars/opennars2/blob/a143162a559e55c456381a95530d00fee57037c4/src/nal/deriver/projection_eternalization.clj#L31
     * patham9 especially try to understand the "temporal temporal" case
     * patham9 its using the result of higher confidence
     * <p>
     * returns ttl used, -1 if failed before starting
     */
    @Override
    public @Nullable Iterable<? extends ITask> run(@NotNull NAR n) {

        int ttlMax = n.matchTTL.intValue(); //TODO adjust this, maybe by priority and other factors

        Derivation d = n.derivation(deriver);
        d.nar.emotion.conceptFirePremises.increment();

        //nar.emotion.count("Premise_run");

        Task taskLink = this.taskLink;
        final Task task = taskLink;
        if (task == null)
            return null;

        NAR nar = d.nar;

        Concept taskConcept = task.concept(nar, true);
        if (taskConcept == null) {
            if (Param.DEBUG) {
                logger.warn("{} unconceptualizable", task); //WHY was task even created
                //assert (false) : task + " could not be conceptualized"; //WHY was task even created
            }
            taskLink.delete();
            task.delete();
            delete();
            return null;
        }


        //float taskPri = task.priElseZero();


        int dur = d.dur;
        long now = d.time;


        Term beliefTerm = termLink;

        Term taskTerm = task.term();
        if (beliefTerm.isTemporal()) {
            //try to temporalize the termlink to match what appears in the task
            try {
                Temporalize t = new Temporalize();
                t.knowAmbient(taskTerm);
                Event bs = t.solve(beliefTerm);
                if (bs != null && !(bs.term instanceof Bool)) {
                    beliefTerm = bs.term;

                    if (!(nar.nal() >= 7 || !beliefTerm.isTemporal())) {
                        //HACK HACK HACK this is temporary until Temporalize correctly differnetiates between && and &| etc
                        beliefTerm = TermIndex.retemporalize(beliefTerm, TermIndex.retemporalizeAllToDTERNAL);

//                        Temporalize t2 = new Temporalize();
//                        t2.knowTerm(task.term(), ETERNAL);
//                        t2.solve(rawBeliefTerm);
                        //TEMPORARY for DEBUG

                        //assert (nar.nal() >= 7 || !beliefTerm.isTemporal()) : "non-eternal beliefTerm in premise: " + beliefTerm + " from " + rawBeliefTerm + " to match " + task.term();
                    }

                }

            } catch (InvalidTermException t) {
                if (Param.DEBUG) {
                    logger.error("temporalize failure: {} {} {}", taskTerm, beliefTerm, t.getMessage());
                    //return 0;
                }
            }
        }


        //Terms.equalAtemporally(task.term(), (beliefTerm));


        //if (taskTerm.varQuery() > 0) {


        boolean beliefConceptCanAnswerTaskConcept = false;

        if (!taskTerm.equals(beliefTerm)) {
            boolean beliefHasVars = beliefTerm.vars() > 0;
            if (taskTerm.vars() > 0 || beliefHasVars) {
                int[] matchTTL = {Math.round(ttlMax * Param.BELIEF_MATCH_TTL_FRACTION)};
                Unify u = unify(taskTerm, beliefTerm, nar, matchTTL);
                if (u != null) {
                    if (beliefHasVars) {
                        beliefTerm = u.transform(beliefTerm);
                    }
                    beliefConceptCanAnswerTaskConcept = true;
                }
                assert (matchTTL[0] <= 0);
                ttlMax += matchTTL[0]; //changed if consumed in match (this value will be negative
            }
        }



        //QUESTION ANSWERING and TERMLINK -> TEMPORALIZED BELIEF TERM projection
        Task belief = null;
        Concept beliefConcept = nar.concept(beliefTerm);


        if (beliefConcept != null) { //beliefs/goals will only be in TaskConcepts

            boolean beliefIsTask = beliefConcept.equals(taskConcept);

            Task match;

            if (task.isQuestOrQuestion() && (beliefIsTask || beliefConceptCanAnswerTaskConcept)) {
                final BeliefTable answerTable =
                        (task.isGoal() || task.isQuest()) ?
                                beliefConcept.goals() :
                                beliefConcept.beliefs();

//                            //see if belief unifies with task (in reverse of previous unify)
//                            if (questionTerm.varQuery() == 0 || (unify((Compound)beliefConcept.term(), questionTerm, nar) == null)) {
//
//                            } else {
//
//                            }
                long when = answerFocus(task, now);
                match = answerTable.answer(when, now, dur, task, beliefTerm, beliefConcept, nar);
                if (match != null) {
                    @Nullable Task answered = task.onAnswered(match, nar);
                    if (answered != null) {

                        float effectiveConf =
                                answered.isEternal() ?
                                        answered.conf() :
                                        answered.conf(answered.nearestTimeBetween(task.start(), task.end()), dur);

                        nar.emotion.onAnswer(taskLink, answered, effectiveConf);

                    }
                }
            } else {
                long when = matchFocus(task, now, dur);

                boolean tryMatch = true;
                if (beliefIsTask && task.punc() == BELIEF && task.during(when)) {
                    if (Math.abs(when - now) > 0 /*= dur*/) {
                        //try projecting to now (maybe also a future time) because it will be a different time
                        when = now;
                    } else {
                        //leave belief blank. it already matches itself
                        tryMatch = false;
                    }
                }
                if (tryMatch) {
                    match = beliefConcept.beliefs().match(when, task, beliefTerm, true, nar);
                } else {
                    match = null;
                }
            }

            if (match != null && match.isBelief()) {
                belief = match;
            }
        }


        if (belief != null) {
            if (belief.equals(task)) { //do not repeat the same task for belief
                belief = null; //force structural transform; also prevents potential inductive feedback loop
                beliefTerm = taskTerm; //use the task's term, which may have temporal information
            } else {
                beliefTerm = belief.term(); //use the belief's actual possibly-temporalized term
            }
        }


        Set<DerivedTask> dd = d.run(this, task, belief, beliefTerm, ttlMax);
        nar.emotion.taskDerivations.increment(dd.size());
        return dd;

//        long ds = d.transformsCache.estimatedSize();
//        if (ds >0)
//            System.out.println(ds + " " + d.transformsCache.stats());


    }

    /**
     * temporal focus control: determines when a matching belief or answer should be projected to
     */
    static long matchFocus(Task task, long now, int dur) {
        if (now == ETERNAL)
            return ETERNAL;

        return now;
        //return now + dur;
        //return task.nearestTimeTo(now);

//        if (task.isEternal()) {
//            return ETERNAL;
//        } else //if (task.isInput()) {
//            return task.nearestTimeTo(now);

//        } else {
//            if (task.isBelief()) {
//                return now +
//                        nar.dur() *
//                            nar.random().nextInt(2*Param.PREDICTION_HORIZON)-Param.PREDICTION_HORIZON; //predictive belief
//            } else {
//                return Math.max(now, task.start()); //the corresponding belief for a goal or question task
//            }
//        }

        //now;
        //now + dur;
    }

    protected static long answerFocus(Task task, long now) {
        return task.nearestTimeTo(now);
    }


    /**
     * unify any (and only) query variables which may be present in
     * the 'a' term with any non-query terms in the 'q' term
     * returns non-null if unification succeeded and resulted in a transformed 'a' term
     * sets a negative number in the ttl array, which is to be added to the callee's
     * ttl.  if zero, then no TTL was consumed
     */
    private static UnifySubst unify(@NotNull Term q, @NotNull Term a, NAR nar, int[] ttl) {

        final int startTTL = ttl[0];
        ttl[0] = 0;

        if (q.op() != a.op() /*|| q.size() != a.size()*/)
            return null; //fast-fail: no chance

        final boolean[] result = {false};
        UnifySubst u = new UnifySubst(null /* any var type */, nar, (aa) -> {

            result[0] = true;
            return false;

//            if (!aa.equals(a)) {
//
//                aa = aa.eval(nar.terms);
//                if (aa!=null) {
//                    result[0] = ((Compound) aa);
//                    return false; //only this match
//                }
//            }
//
//
//            return true; //keep trying

        }, startTTL);
        u.unifyAll(q, a);

        ttl[0] = -(startTTL - u.ttl); //how much consumed

        if (result[0])
            return u;
        else
            return null;

//        if (Terms.equal(q, a, false, true /* no need to unneg, task content is already non-negated */))
//            return q;
//        else
//            return null;
    }

}
