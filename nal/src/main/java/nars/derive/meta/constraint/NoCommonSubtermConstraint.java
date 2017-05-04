package nars.derive.meta.constraint;

import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import static nars.term.container.TermContainer.isSubtermOfTheOther;

/** containment test of x to y's subterms and y to x's subterms */
public final class NoCommonSubtermConstraint extends CommonalityConstraint {

    public final boolean recurse;

    /**
     * @param recurse
     *   true: recursive
     *   false: only cross-compares the first layer of subterms.
     */
    public NoCommonSubtermConstraint(Term target, @NotNull Term x, boolean recurse) {
        super(recurse ? "neqRCom" : "neqCom",
                target, x );
        this.recurse = recurse;
    }

    @Override
    public int cost() {
        return recurse ? 10 : 5;
    }

    @Override
    protected @NotNull boolean invalid(Term x, Term y) {
        return false;
    }

    /** comparison between two compounds */
    @Override
    @NotNull protected boolean invalid(@NotNull Compound x, @NotNull Compound y) {
        return invalid((Term)x, y);
    }

    @NotNull @Override protected boolean invalid(Term x, Compound y) {
        return isSubtermOfTheOther(x, y, recurse, true);
    }

    //commonSubtermsRecurse((Compound) B, C, true, new HashSet())
                    //commonSubterms((Compound) B, C, true, scratch.get())



}
