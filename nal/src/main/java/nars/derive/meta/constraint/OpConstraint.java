package nars.derive.meta.constraint;

import nars.Op;
import nars.term.Term;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;


public final class OpConstraint implements MatchConstraint {

    @NotNull
    private final Op op;

    public OpConstraint(@NotNull Op o) {
        op = o;
    }


    @Override
    public boolean invalid(@NotNull Term assignee, @NotNull Term value, @NotNull Unify f) {

        return value.op()!=op;
    }

    @NotNull
    @Override
    public String toString() {
        return "op:\"" + op.str + '"';
    }
}
