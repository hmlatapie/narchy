package nars.term.atom;

import nars.Op;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;


public class AtomicSingleton extends AtomicStringConstant {

    public AtomicSingleton(@NotNull String id) {
        super(id);
    }

    @Override
    public final boolean equals(Object u) {
        return u == this;
    }

    @Override
    public int compareTo(@NotNull Termlike y) {
        if (this == y) {
            return 0;
        } else {
            int c = super.compareTo(y);
            if (c == 0) {
                throw new RuntimeException("AtomicSingleton leak");
            }
            return c;
        }
    }

    @Override
    public @NotNull Op op() {
        return Op.ATOM;
    }

    @Override
    public boolean unify(@NotNull Term y, @NotNull Unify subst) {
        throw new UnsupportedOperationException("AtomicSingleton leak");
    }

}
