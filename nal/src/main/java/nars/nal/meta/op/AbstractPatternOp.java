package nars.nal.meta.op;

import nars.Op;
import nars.nal.meta.AtomicBoolCondition;
import nars.nal.meta.PremiseEval;
import org.jetbrains.annotations.NotNull;

/**
 * a condition on the op of a pattern term (task=0, or belief=1)
 */
public enum AbstractPatternOp  {
    ;

    static String name(Class c, int subterm, String param) {
        return c.getSimpleName() + "(p" + Integer.toString(subterm) + ",\"" + param + "\")";
    }

    public static class PatternOp extends AtomicBoolCondition {

        public final int subterm;
        public final int opOrdinal;

        @NotNull private final transient String id;

        public PatternOp(int subterm, @NotNull Op op) {

            this.subterm = subterm;
            this.opOrdinal = op.ordinal();
            this.id = name(getClass(), subterm, "\"" + op + "\"");

        }

        @NotNull
        @Override
        public String toString() {
            return id;
        }

        @Override
        public boolean booleanValueOf(@NotNull PremiseEval ff) {
            return (subterm == 0 ? ff.termSub0op : ff.termSub1op) == opOrdinal;
        }

    }

    /** tests op membership in a given vector */
    public static final class PatternOpNot extends AtomicBoolCondition {

        public final int subterm;
        public final int opBits;

        @NotNull private final transient String id;


        public PatternOpNot(int subterm, int structure) {
            this.subterm = subterm;
            this.opBits = structure;
            this.id = name(getClass(), subterm, Integer.toString(structure,2));
        }

        @Override
        public @NotNull String toString() {
            return id;
        }

        @Override
        public boolean booleanValueOf(@NotNull PremiseEval ff) {
            //the bit must not be set in the structure
            return (opBits & (subterm == 0 ? ff.termSub0opBit : ff.termSub1opBit)) == 0;
        }
    }


}
