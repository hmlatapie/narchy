package nars.term.var;

import nars.Op;
import org.jetbrains.annotations.NotNull;


/**
 * normalized indep var
 */
public final class VarIndep extends AbstractVariable {

    public VarIndep(int id) {
        super(Op.VAR_INDEP, id);
    }


    @NotNull
    @Override
    public Op op() {
        return Op.VAR_INDEP;
    }

    @Override
    public int vars() {
        return 1;
    }

    @Override
    public int varIndep() {
        return 1;
    }


}
