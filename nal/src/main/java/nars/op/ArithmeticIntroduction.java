package nars.op;

import jcog.Util;
import jcog.list.FasterList;
import nars.$;
import nars.term.Term;
import nars.term.anon.Anom;
import nars.term.anon.Anon;
import nars.term.atom.Int;
import nars.term.var.Variable;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import static nars.Op.*;

/**
 * introduces arithmetic relationships between differing numeric subterms
 */
public class ArithmeticIntroduction {

    public static Term apply(Term x, Random rng) {
        return apply(x, null, rng);
    }

    public static Term apply(Term x, @Nullable Anon anon, Random rng) {
        if (x.complexity() < 3 || (anon==null && !x.hasAny(INT)))
            return x;

        //find all unique integer subterms
        IntHashSet ints = new IntHashSet();
        x.recurseTerms((t) -> {
            Int it = null;
            if (t instanceof Anom) {
                Anom aa = ((Anom) t);
                Term ta = anon.get(aa);
                if (ta.op() == INT)
                    it = ((Int) ta);
            } else if (t instanceof Int) {
                it = (Int) t;
            }
            if (it == null)
                return;

            ints.add((it.id));
        });

        //Set<Term> ints = ((Compound) x).recurseTermsToSet(INT);
        int ui = ints.size();
        if (ui <= 1)
            return x; //nothing to do

        int[] ii = ints.toSortedArray();  //increasing so that relational comparisons can assume that 'a' < 'b'

        //potential mods to select from
        //FasterList<Supplier<Term[]>> mods = new FasterList(1);
        IntObjectHashMap<List<Supplier<Term[]>>> mods = new IntObjectHashMap();

        Variable v = $.varDep("x");

        //test arithmetic relationships
        for (int a = 0; a < ui; a++) {
            int ia = ii[a];
            for (int b = a + 1; b < ui; b++) {
                int ib = ii[b];
                assert(ib > ia);

                if (ib - ia < ia && (ia!=0)) {
                    //Add if the delta < 'ia'
                    mods.getIfAbsentPut(ia, FasterList::new).add(()-> new Term[]{
                        Int.the(ib), $.func("add", v, $.the(ib - ia))
                    });
                } else if ((ia!=0 && ia!=1) && (ib!=0 && ib!=1) && Util.equals(ib/ia, (int)(((float)ib)/ia), Float.MIN_NORMAL)) {

                    mods.getIfAbsentPut(ia, FasterList::new).add(()-> new Term[]{
                            Int.the(ib), $.func("mul", v, $.the(ib/ia))
                    });
                }

            }
        }
        if (mods.isEmpty())
            return x;

        //TODO fair select randomly if multiple of the same length
        IntObjectPair<List<Supplier<Term[]>>> m = mods.keyValuesView().maxBy(e -> e.getTwo().size());
        int base = m.getOne();
        Term baseTerm = Int.the(base);
        if (anon!=null)
            baseTerm = anon.put(baseTerm);

        Term yy = x.replace(baseTerm, v);

        for (Supplier<Term[]> s : m.getTwo()) {
            Term[] mm = s.get();
            if (anon!=null)
                mm[0] = anon.put(mm[0]);
            yy = yy.replace(mm[0], mm[1]);
        }
        Term y = CONJ.the(yy, SIM.the(baseTerm, v));

        if (x.isNormalized()) {
            y = y.normalize();
        }
        return y;
    }

}
