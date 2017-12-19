package nars.derive.op;

import nars.control.Derivation;
import nars.control.ProtoDerivation;
import nars.term.pred.AbstractPred;
import nars.term.pred.PrediTerm;
import nars.truth.Truth;

/**
 * task truth is postiive
 */
abstract public class TaskPolarity extends AbstractPred<ProtoDerivation> {


//    public static final PrediTerm<ProtoDerivation> taskContainsBelief = new TaskPolarity("TaskContainsBelief") {
//        @Override
//        public boolean test(ProtoDerivation m) {
//            return m.taskTerm.contains(m.beliefTerm) || (m.taskTerm.hasAny(Op.NEG) && m.taskTerm.contains(m.beliefTerm.neg()));
//        }
//
//        @Override
//        public float cost() {
//            return 0.5f;
//        }
//    };
    public static final PrediTerm<ProtoDerivation> taskContainsBeliefRecursively = new TaskPolarity("TaskContainsBelief") {
        @Override
        public boolean test(ProtoDerivation m) {
            return m.taskTerm.containsRecursively(m.beliefTerm);
        }

        @Override
        public float cost() {
            return 0.75f;
        }
    };
    public static final PrediTerm<ProtoDerivation> taskPos = new TaskPolarity("TaskPos") {
        @Override
        public boolean test(ProtoDerivation m) {
            Truth t = m.taskTruth;
            return (t != null && t.freq() >= 0.5f);
        }

    };
    public static final PrediTerm<ProtoDerivation> taskNeg = new TaskPolarity("TaskNeg") {
        @Override
        public boolean test(ProtoDerivation m) {
            Truth t = m.taskTruth;
            return (t != null && t.freq() < 0.5f);
        }
    };
    public static final PrediTerm<ProtoDerivation> beliefPos = new TaskPolarity("BeliefPos") {
        @Override
        public boolean test(ProtoDerivation d) {
            Truth B = d.beliefTruth;
            return B != null && B.freq() >= 0.5f;
        }
    };
    public static final PrediTerm<ProtoDerivation> beliefNeg = new TaskPolarity("BeliefNeg") {
        @Override
        public boolean test(ProtoDerivation d) {
            Truth B = d.beliefTruth;
            return B != null && B.freq() < 0.5f;
        }
    };

    @Override
    public float cost() {
        return 0.2f;
    }

    protected TaskPolarity(String x) {
        super(x);
    }
}
