package nars.term.compound;

import jcog.Util;
import jcog.data.byt.DynBytes;
import nars.IO;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.eclipse.collections.api.block.function.primitive.ByteFunction0;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.eclipse.collections.api.tuple.primitive.ObjectBytePair;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import static nars.time.Tense.DTERNAL;

/**
 * Annotates a GenericCompound with cached data to accelerate pattern matching
 * TODO not finished yet
 */
abstract public class FastCompound implements Compound /* The */ {

    static public final Op[] ov = Op.values();

    private static final int MAX_LAYERS = 8;

    private final int MAX_LAYER_LEN = 8;

    /** TODO */
    abstract public static class FastCompoundSerializedAtoms extends FastCompound {
        @NotNull
        private final byte[][] atoms;

        protected FastCompoundSerializedAtoms(byte[][] atoms, byte[] shadow, int structure, int hash, byte volume, boolean normalized) {
            super(shadow, structure, hash, volume, normalized);
            this.atoms = atoms;
        }

        @Override
        protected Term atom(byte id) {
            return IO.readAtomic(atoms[id]);
        }

        @Override
        protected int atomCount() {
            return atoms.length;
        }
    }

    public static class FastCompoundInstancedAtoms extends FastCompound {
        @NotNull
        private final Term[] atoms;

        public FastCompoundInstancedAtoms(Term[] atoms, byte[] shadow, int structure, int hash, byte volume, boolean normalized) {
            super(shadow, structure, hash, volume, normalized);
            this.atoms = atoms;
        }

        @Override
        protected int atomCount() {
            return atoms.length;
        }

        @Override
        protected boolean containsAtomic(Atomic x) {
            if (!hasAny(x.op()))
                return false;
            for (Term y : atoms) {
                if (x.equals(y))
                    return true;
            }
            return false;
        }

        @Override
        protected Term atom(byte id) {
            return atoms[id];
        }
    }

    @NotNull
    protected final byte[] shadow;

    final int hash;
    final byte volume;
    protected final int structure;

    protected FastCompound(byte[] shadow, int structure, int hash, byte volume, boolean normalized) {
        this.shadow = shadow;
        this.hash = hash;
        this.volume = volume;
        this.structure = structure;
    }


    @Override
    public int volume() {
        return volume;
    }

    @Override
    public int structure() {
        return structure;
    }

    public static FastCompound get(Compound x) {
        if (x instanceof FastCompound)
            return ((FastCompound) x);

        FastCompound f = get(x.op(), x.subs(), x.subterms());
        return f;
    }

    public static FastCompound get(Op o, List<Term> subterms) {
        return get(o, subterms.size(), subterms);
    }

    public static FastCompound get(Op o, int subs, Iterable<Term> subterms) {

        ObjectByteHashMap<Term> atoms = new ObjectByteHashMap();

        DynBytes shadow = new DynBytes(256);
        //UncheckedBytes shadow = new UncheckedBytes(Bytes.wrapForWrite(new byte[256]));


        shadow.writeUnsignedByte(o.ordinal());
        shadow.writeUnsignedByte(subs);
        final byte[] numAtoms = {0};
        ByteFunction0 nextUniqueAtom = () -> numAtoms[0]++;
        int structure = o.bit, hashCode = 1;
        byte volume = 1;

        for (Term x : subterms) {
            x.recurseTerms((child) -> {
                shadow.writeUnsignedByte((byte) child.op().ordinal());
                if (child.op().atomic) {
                    int aid = atoms.getIfAbsentPut(child, nextUniqueAtom);
                    shadow.writeUnsignedByte((byte) aid);
                } else {
                    shadow.writeUnsignedByte(child.subs());
                    //TODO use last bit of the subs byte to indicate presence or absence of subsequent 'dt' value (32 bits)
                }
            });
            structure |= x.structure();
            hashCode = Util.hashCombine(hashCode, x.hashCode());
            volume += x.volume();
        }

        hashCode = Util.hashCombine(hashCode, o.id);

        assert(volume < 127);
        boolean normalized = false; //TODO calculate normalized by the encountered sequence of variable id's - whether it is monotonically increasing by 1 each time the max value does increase or something

        //TODO sort atoms to canonicalize its dictionary for sharing with other terms

        FastCompound y;
//        {
//            byte[][] a = new byte[atoms.size()][];
//            for (ObjectBytePair<Term> p : atoms.keyValuesView()) {
//                a[p.getTwo()] = IO.termToBytes(p.getOne());
//            }
//            y = new FastCompoundSerializedAtoms(a, shadow.toByteArray(), x.hashCode(), x.hashCodeSubTerms(), (byte) x.volume(), x.isNormalized());
//        }
        {
            Term[] a = new Term[atoms.size()];
            for (ObjectBytePair<Term> p : atoms.keyValuesView()) {
                a[p.getTwo()] = p.getOne();
            }
            y = new FastCompoundInstancedAtoms(a, shadow.toByteArray(), structure, hashCode, volume, normalized);
        }

        return y;
    }

    public void print() {
//        System.out.println(shadow.toDebugString());
//        System.out.println(shadow.toHexString());
//        System.out.println(shadow.to8bitString());

//        System.out.println("shadow: (" + shadow.length + " bytes)\t");
//        System.out.println(new UncheckedBytes(Bytes.wrapForRead(shadow)).toHexString());
//        System.out.println("atoms:\t");
//        for (Object b : atoms()) {
//            //System.out.println("\t" + (b instanceof byte[] ? (IO.termFromBytes((byte[])b) + + " (" + b.length + " bytes)") : b) );
//
//        }
        System.out.println();
    }

    //abstract public Iterable<Term> atoms();


    @Override
    public boolean containsRecursively(Term t) {
        if (t instanceof Atomic) {
            return containsAtomic((Atomic)t);
        } else {
            return Compound.super.containsRecursively(t);
        }
    }

//TODO
//    @Override
//    public boolean containsRecursively(Term t, boolean root, Predicate<Term> inSubtermsOf) {
//        return inSubtermsOf.test(this) && subterms().containsRecursively(t, root, inSubtermsOf);
//    }

    protected abstract boolean containsAtomic(Atomic t);

    @Override
    public Op op() {
        return ov[shadow[0]];
    }

    @Override
    public int subs() {
        return shadow[1];
    }

    @Override
    public Subterms subterms() {
        return new SubtermView(this, 0);
    }

    public interface ByteIntPredicate {
        boolean test(byte a, int b);
    }

//    public int[] subtermOffsetsAt(int at) {
//        int[] b = new int[subtermCountAt(at)];
//        subtermsAt(at, (i, c) -> {
//            b[i] = c;
//            return true;
//        });
//        return b;
//    }

//    /**
//     * returns byte[] of the offset of each subterm
//     */
//    public void subtermsAt(int at, ByteIntPredicate each /* subterm #, offset # */) {
//        byte[] shadow = this.shadow;
//
//        assert (!ov[shadow[at]].atomic);
//
//        byte[] stack = new byte[MAX_LAYERS];
//
//        byte depth = 0;
//        byte subs0 = stack[0] = subtermCountAt(at);
//
//        at += 2; //skip compound header
//
//        for (byte i = 0; i < subs0; ) {
//            if (depth == 0 && !each.test(i, at))
//                break;
//
//            byte op = shadow[at++]; //get op and skip past it
//
//            if (ov[op].atomic) {
//                at++; //skip past atom id
//            } else {
//                stack[++depth] = shadow[at++]; //store subcount and skip past it
//            }
//
//            if (depth == 0)
//                i++;
//
//            if (--stack[depth] == 0)
//                depth--; //ascend
//        }
//
//    }

    public byte subtermCountAt(int at) {
        return shadow[at + 1];
    }

    /**
     * seeks and returns the offset of the ith subterm
     */
    public int subtermOffsetAt(int subterm, int at) {
        if (subterm == 0) {
            //quick case
            return at + 2;
        }

        final int[] o = new int[1];
        subtermOffsets(at, (sub, offset) -> {
            if (sub == subterm) {
                o[0] = offset;
                return false;
            }
            return true;
        });
        assert(o[0]!=0);
        return o[0];
    }

    public void subtermOffsets(int at, ByteIntPredicate each) {
        byte[] shadow = this.shadow;

        assert (!ov[shadow[at]].atomic);
        
        byte subterms = shadow[at + 1];
        if (subterms == 0)
            return;

        byte depth = 0;
        byte[] stack = new byte[MAX_LAYERS];
        stack[0] = subterms;

        at += 2; //skip compound header

        for (byte i = 0; i < subterms; ) {
            if (depth == 0) {
                if (!each.test(i, at) || i == subterms-1) //tail call early exit we dont need to scan the contents of the last subterm
                    return;
            }

            byte op = shadow[at++]; //get op and skip past it


            if (ov[op].atomic) {
                at++; //skip past atom id
            } else {
                stack[++depth] = shadow[at++]; //store subcount and skip past it
            }



            if (--stack[depth] == 0)
                depth--; //ascend
            if (depth == 0)
                i++;
        }

    }


    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public int dt() {
        return DTERNAL; //TODO
    }

    @NotNull
    @Override
    public String toString() {
        return Compound.toString(this);
    }


    /**
     * subterm, or sub-subterm, etc.
     */
    public Term term(int offset) {
        Op opAtSub = ov[shadow[offset]];
        if (opAtSub.atomic) {
            //return IO.termFromBytes(atoms[shadow[offset + 1]]);
            return atom(shadow[offset + 1]);

        } else {
            //TODO sub view
            //return opAtSub.the(DTERNAL, subs(subOffset));
            return CachedCompound.the(opAtSub,
                    Subterms.subterms(new SubtermView(this, offset))
                    //new SubtermView(this, offset)
            );
        }
    }

    protected abstract Term atom(byte id);

    public Term sub(byte i, int containerOffset) {

        return term(subtermOffsetAt(i, containerOffset));

    }

//    public Term[] subs(int offset) {
//        //new SubtermView(this, offset).theArray()
//        int[] b = subtermOffsetsAt(offset);
//        Term[] t = new Term[b.length];
//        for (int i = 0; i < b.length; i++) {
//            t[i] = term(b[i]);
//        }
//        return t;
//    }

    @Override
    public boolean equals(@Nullable Object that) {
        if (this == that) return true;

        if (!(that instanceof Term) || hash != that.hashCode())
            return false;

        if (that instanceof FastCompound) {
            FastCompound f = (FastCompound) that;
            int aa = atomCount();
            if (aa == f.atomCount()) {
                if (Arrays.equals(shadow, f.shadow)) {
                    for (byte i = 0; i < aa; i++)
                        if (!atom(i).equals(f.atom(i))) //TODO for byte[]
                            return false;
                    return true;
                }
            }
        } else {
            return Compound.equals(this, (Term) that);
        }
        return false;
    }

    protected abstract int atomCount();

    private static class SubtermView extends AbstractList<Term> implements Subterms {
        private final FastCompound c;

        private int offset = 0;
        int _hash = 0;

        public SubtermView(FastCompound terms, int offset) {
            this.c = terms;
            if (offset!=0)
                go(offset);
        }


        @Override
        public Term get(int index) {
            return sub(index);
        }

        @Override
        public boolean equals(Object obj) {
            return
                    (this == obj)
                            ||
                            (obj instanceof Subterms)
                                    && hashCodeSubterms() == ((Subterms) obj).hashCodeSubterms()
                                    && equalTerms(((Subterms) obj).arrayShared());
        }

        @Override
        public int intifyShallow(IntObjectToIntFunction<Term> reduce, int v) {
            int o = offset;
            final int[] vv = {v};
            c.subtermOffsets(o, (subterm, at)->{
                Term t = c.term(at);
                //System.out.println(subterm + " " + at + " " + t);
                vv[0] = reduce.intValueOf(vv[0], t);
                return true;
            });
            return vv[0];
        }

        @Override
        public int hashCode() {
            //TODO maybe cache this value while offset doesnt change
            int h = _hash;
            if (h == 0) {
                return _hash = intifyShallow((i, t) -> Util.hashCombine(i, t.hashCode()), 1);
            } else {
                return h;
            }
        }

        @Override
        public int hashCodeSubterms() {
            return this.hashCode();
        }

        public SubtermView go(int offset) {
            int o = this.offset;
            if (o != offset) {
                this.offset = offset;
                _hash = 0;
            }
            return this;
        }

        @Override
        public Term sub(int i) {
            return c.sub((byte) i, offset);
        }

        @Override
        public int subs() {
            int offset = this.offset;
            byte[] s = c.shadow;
            Op op = ov[s[offset]];
            if (op.atomic) {
                return 0;
            } else {
                return s[offset + 1];
            }
        }

        @Override
        public int size() {
            return subs();
        }
    }

    /**
     * for use in: Builder.Compound.the
     */
    public static final BiFunction<Op, List<Term>, Term> FAST_COMPOUND_BUILDER = (op, terms) -> {
        //HACK creating an intermediate GenericCompound should not be necessary
        CachedCompound g = CachedCompound.the(op, Subterms.subterms(terms));
        try {

            if (!g.isTemporal())
                return get(g);
            else
                return g;

        } catch (Throwable t) {
            return g;
        }
    };

}
