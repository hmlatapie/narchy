package nars.op.data;

import com.gs.collections.api.set.MutableSet;
import nars.nal.op.BinaryTermOperator;
import nars.term.Compound;
import nars.term.Term;
import nars.term.TermIndex;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class intersect extends BinaryTermOperator {

    @Nullable
    @Override public Term apply(@NotNull Term a, Term b, @NotNull TermIndex i) {
        return TermContainer.intersect(i.builder(), (Compound)a, (Compound) b);
    }

}
