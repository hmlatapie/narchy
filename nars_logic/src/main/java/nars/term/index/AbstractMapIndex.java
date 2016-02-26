package nars.term.index;

import nars.concept.Concept;
import nars.nal.meta.match.Ellipsis;
import nars.term.*;
import nars.term.atom.Atom;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by me on 12/31/15.
 */
public abstract class AbstractMapIndex implements TermIndex {

    protected final Atoms atoms;
    protected final TermBuilder builder;


    public AbstractMapIndex(TermBuilder termBuilder, Function<Term, Concept> conceptBuilder) {
        super();
        this.builder = termBuilder;
        this.atoms = new Atoms(conceptBuilder);
    }


//    /** get the instance that will be internalized */
//    @NotNull
//    public static Termed intern(@NotNull Op op, int relation, @NotNull TermContainer t) {
//        //return (TermMetadata.hasMetadata(t) || op.isA(TermMetadata.metadataBits)) ?
//                //newMetadataCompound(op, relation, t) :
//        return newInternCompound(op, t, relation);
//    }



//    @Nullable
//    public static Term newMetadataCompound(@NotNull Op op, int relation, @NotNull TermContainer t) {
//        //create unique
//        return $.the(op, relation, t);
//    }

//    @NotNull
//    static Termed newInternCompound(@NotNull Op op, @NotNull TermContainer subterms, int relation) {
//        return new GenericCompound(
//            op, relation, (TermVector) subterms
//        );
//    }

//    @Nullable
//    default Termed getOrAdd(@NotNull Termed t) {
//    }

    final Termed theAtom(Term t) {
        return (t instanceof Atom) ?
                atoms.resolveOrAdd((Atom)t)
                : t;
    }

    abstract protected Termed theCompound(@NotNull Compound x);


    @Nullable
    @Override public Termed the(@NotNull Termed t) {

        if (t instanceof Ellipsis)
            ///throw new RuntimeException("ellipsis not allowed in this index");
            return null;

//        if (!isInternable(x)) {
//            //TODO intern any subterms which can be
//            return x;
//        }

//        Termed y = the(x);
//        if (y == null) {
//            if ((y = the(x)) !=null) {
//                put(y);
//                if (!y.equals(x))
//                    return x; //return original non-anonymized
//            }
//        }

        return t instanceof Compound ? theCompound((Compound) t)
                : theAtom(t.term());
    }

//    @Override
//    public abstract Termed getTermIfPresent(Termed t);

//    @Override
//    public abstract void clear();

//    @Override
//    public abstract int subtermsCount();

//    @Override
//    public abstract int size();


    @Override
    public void print(@NotNull PrintStream out) {

        atoms.print(System.out);
        forEach(out::println);

    }

    @Nullable
    abstract protected TermContainer get(TermContainer subterms);


//    @Override
//    public void print(PrintStream out) {
//        BiConsumer itemPrinter = (k, v) -> System.out.println(v.getClass().getSimpleName() + ": " + v);
//        forEach(d -> itemPrinter);
//        System.out.println("--");
//        subterms.forEach(itemPrinter);
//    }

    @Override
    public abstract void forEach(Consumer<? super Termed> c);


}
