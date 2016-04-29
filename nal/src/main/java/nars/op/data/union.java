package nars.op.data;

import nars.nal.op.BinaryTermOperator;
import nars.term.Compound;
import nars.term.Term;
import nars.term.TermIndex;
import nars.term.container.TermContainer;
import nars.term.container.TermSet;
import org.jetbrains.annotations.NotNull;

public class union extends BinaryTermOperator {
    
    @NotNull
    @Override public Term apply(@NotNull Term a, Term b, @NotNull TermIndex i) {
        return TermSet.union(i.builder(), (Compound) a, (Compound) b );
    }

}
