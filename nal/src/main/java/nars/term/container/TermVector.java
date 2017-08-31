package nars.term.container;

import com.google.common.base.Joiner;
import nars.Op;
import nars.Param;
import nars.term.Term;
import nars.term.Terms;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * what differentiates TermVector from TermContainer is that
 * a TermVector specifically for subterms.  while both
 * can be
 */
public abstract class TermVector implements TermContainer {

    /** normal high-entropy "content" hash */
    public  final int hash;
    /**
     * bitvector of subterm types, indexed by Op.id and OR'd into by each subterm
     * low-entropy, use 'hash' for normal hash operations.
     */
    public  final int structure;
    /** stored as volume+1 as if this termvector were already wrapped in its compound */
    public  final short volume;
    /** stored as complexity+1 as if this termvector were already wrapped in its compound */
    public  final short complexity;
    /**
     * # variables contained, of each type & total
     * this means maximum of 127 variables per compound
     */
    public final byte varQuerys;
    public final byte varIndeps;
    public final byte varPatterns;
    public final byte varDeps;

    protected TermVector(Term... terms) {

        assert(terms.length <= Param.COMPOUND_SUBTERMS_MAX);

//         if (Param.DEBUG) {
//             for (Term x : terms)
//                 if (x == null) throw new NullPointerException();
//         }

        int[] meta = new int[6];
        this.hash = Terms.hashSubterms(terms, meta);

        final int vD = meta[0];  this.varDeps = (byte)vD;
        final int vI = meta[1];  this.varIndeps = (byte)vI;
        final int vQ = meta[2];  this.varQuerys = (byte)vQ;
        final int vP = meta[3];  this.varPatterns = (byte)vP;   //varTot+=NO

        final int vol = meta[4] + 1;
        this.volume = (short)( vol );

        int varTot = vD + vI + vQ ;
        final int cmp = vol - varTot - vP;
        this.complexity = (short)(cmp);

        this.structure = meta[5];
    }


    @NotNull
    public static TermVector1 the(@NotNull Term the) {
        return new TermVector1(the);
    }

    @NotNull
    public static TermVector the2(@NotNull Term[] xy) {
        return new TermVector2(xy);
    }

    @NotNull
    public static TermContainer the(@NotNull Term... t) {
        switch (t.length) {
            case 0:
                return TermContainer.NoSubterms;
            case 1:
                return the(t[0]);
            case 2:
                return the2(t);
            default:
                return new ArrayTermVector(t);
        }
    }

    protected static TermContainer the(@NotNull Collection<? extends Term> t) {
        return Op.subterms(t.toArray(new Term[t.size()]));
    }



    @Override
    public void forEach(@NotNull Consumer<? super Term> action) {
        forEach(action, 0, size());
    }

//    @NotNull
//    @Override public final Term[] terms(@NotNull IntObjectPredicate<Term> filter) {
//        return Terms.filter(terms(), filter);
//    }

    @Override
    public final int structure() {
        return structure;
    }

    @Override
    @NotNull abstract public Term sub(int i);

    @Override
    public final int volume() {
        return volume;
    }

    /**
     * report the term's syntactic complexity
     *
     * @return the complexity value
     */
    @Override
    public final int complexity() {
        return complexity;
    }

    @Override
    public abstract int size();

    @NotNull
    @Override
    public String toString() {
        return '(' + Joiner.on(',').join(toArray()) + ')';
    }

    @Override
    public final int varDep() {
        return varDeps;
    }

    @Override
    public final int varIndep() {
        return varIndeps;
    }

    @Override
    public final int varQuery() {
        return varQuerys;
    }

    @Override
    public final int varPattern() {
        return varPatterns;
    }

    @Override
    public final int vars() {
        return volume-complexity-varPatterns;
    }



    @Override
    public abstract Iterator<Term> iterator();


    @Override
    abstract public boolean equals(@NotNull Object obj);
//        return
//            (this == obj)
//            ||
//            (obj instanceof TermContainer) && equalTerms((TermContainer)obj);
//    }



    @Override
    public final int hashCode() {
        return hash;
    }

//    public final boolean visit(@NotNull BiPredicate<Term,Compound> v, Compound parent) {
//        int cl = size();
//        for (int i = 0; i < cl; i++) {
//            if (!v.test(term(i), parent))
//                return false;
//        }
//        return true;
//    }

//    @NotNull
//    public TermContainer reverse() {
//        if (size() < 2)
//            return this; //no change needed
//
//        return TermVector.the( Util.reverse( toArray().clone() ) );
//    }

}
