package nars.derive.constraint;

import nars.term.Term;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;

import static nars.Op.Null;

/**
 * note: if the two terms are equal, it is automatically invalid ("neq")
 */
public abstract class CommonalityConstraint extends MatchConstraint {

    private final Term other;

    protected CommonalityConstraint(String func, Term target, Term other) {
        super(func, target, other);
        this.other = other;
    }

    @Override
    public final boolean invalid(@NotNull Term y, @NotNull Unify f) {

        Term x = f.xy(other);
        if (x == null) {
            return false; //not invalid until both are present to be compared
        } else if (x.equals(y)) {
            return true;
        } else {
//            if (!invalid(x,y)) {
//                if (x.containsRecursively(y) || y.containsRecursively(x)) {
//                    invalid(x,y);
//                    System.err.println("wtf");
//                }
//                return false;
//            }
//            return true;
            return invalid(x, y);
        }
    }


    /**
     * equality will have already been tested prior to calling this
     */
    @NotNull
    protected abstract boolean invalid(Term x, Term y);


}
