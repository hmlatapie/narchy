package nars.term.compound;

import nars.Op;
import nars.The;
import nars.term.Term;

import static nars.Op.NEG;


/** 1-element Compound impl */
public class CachedUnitCompound extends UnitCompound implements The {

    private final Op op;

    private final Term sub;

    /** hash including this compound's op (cached) */
    transient private final int chash;

    /** structure including this compound's op (cached) */
    transient private final int cstruct;
    private final short volume;


    public CachedUnitCompound(/*@NotNull*/ Op op, /*@NotNull*/ Term sub) {
        assert(op!=NEG); //makes certain assumptions that it's not NEG op, use Neg.java for that

        this.sub = sub;
        this.op = op;
        this.chash = super.hashCode();
        this.cstruct = super.structure();

        int v = sub.volume() + 1;
        assert(v < Short.MAX_VALUE);
        this.volume = (short) v;
    }

    @Override
    public int volume() {
        return volume;
    }

    @Override
    public final Term unneg() {
        return this;
    }

    @Override
    public final Term sub() {
        return sub;
    }


    @Override
    public final int structure() {
        return cstruct;
    }


    @Override
    public final int hashCode() {
        return chash;
    }


    @Override
    public final /*@NotNull*/ Op op() {
        return op;
    }

    @Override
    public boolean impossibleSubTermVolume(int otherTermVolume) {
        return otherTermVolume > volume-1;
    }


}
