package nars.nal.meta.match;

import nars.Op;
import nars.nal.meta.PremiseMatch;
import nars.nal.meta.PremiseRule;
import nars.term.Compound;
import nars.term.Term;
import nars.term.transform.VariableNormalization;
import nars.term.variable.Variable;
import org.jetbrains.annotations.NotNull;

/** ellipsis that transforms one of its elements, which it is required to match within */
public class EllipsisTransform extends EllipsisOneOrMore {

    public final Term from;
    public final Term to;

    public EllipsisTransform(@NotNull Variable name, Term from, Term to) {
        super(name, ".." + from + '=' + to + "..+");

//        if (from instanceof VarPattern)
//            this.from = new VarPattern(((VarPattern) from).id);
//        else
          this.from = from;
//
//        if (from instanceof VarPattern)
//            this.to = new VarPattern(((VarPattern) to).id);
//        else
          this.to = to;
    }

    @NotNull
    @Override
    public Variable normalize(int serial) {
        //handled in a special way elsewhere
        return this;
    }

    @NotNull
    @Override
    public Variable clone(@NotNull Variable v, VariableNormalization normalizer) {
        //normalizes any variable parameter terms of an EllipsisTransform
        PremiseRule.PremiseRuleVariableNormalization vnn = (PremiseRule.PremiseRuleVariableNormalization) normalizer;
        return new EllipsisTransform(v,
                from instanceof Variable ? vnn.applyAfter((Variable)from) : from,
                to instanceof Variable ? vnn.applyAfter((Variable)to) : to);
    }

    @NotNull
    public EllipsisMatch collect(@NotNull Compound y, int a, int b, @NotNull PremiseMatch subst) {
        if (from.equals(Op.Imdex) && (y.op().isImage())) {

            int rel = y.relation();
            int n = (b-a)+1;
            int i = 0;
            int ab = 0;
            Term[] t = new Term[n];
            Term to = this.to;
            while (i < n)  {
                t[i++] = ((i == rel) ? subst.resolve(to) : y.term(ab));
                ab++;
            }
            return new EllipsisMatch(t);

        } else {
            return new EllipsisMatch(
                    y, a, b
            );
        }
    }
}
