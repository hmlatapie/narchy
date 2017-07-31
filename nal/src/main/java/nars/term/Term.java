/*
 * Term.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.term;


import com.google.common.io.ByteArrayDataOutput;
import nars.$;
import nars.IO;
import nars.Op;
import nars.Param;
import nars.index.term.StaticTermIndex;
import nars.index.term.TermContext;
import nars.term.atom.Atomic;
import nars.term.atom.AtomicToString;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import nars.term.subst.Unify;
import nars.term.transform.CompoundTransform;
import nars.term.var.AbstractVariable;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.list.primitive.ImmutableByteList;
import org.eclipse.collections.api.tuple.primitive.ObjectLongPair;
import org.eclipse.collections.impl.factory.primitive.ByteLists;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;


public interface Term extends Termlike, Comparable<Term> {


    //@NotNull public static final int[] ZeroIntArray = new int[0];
    @NotNull Term[] EmptyArray = new Term[0];
    ImmutableByteList EmptyByteList = ByteLists.immutable.empty();

    @Override
    default Term term() {
        return this;
    }


    @NotNull
    @Override
    Op op();

    @Override
    int volume();

    @Override
    int complexity();

    @Override
    int structure();

    default void append(ByteArrayDataOutput out) {
        Term.append(this, out);
    }

    static void append(Term term, ByteArrayDataOutput out) {

        Op o = term.op();
        out.writeByte(o.id);
        IO.writeTermContainer(out, term.subterms());
        if (o.temporal)
            out.writeInt(term.dt());

    }

    @Override
    default int size() {
        return subterms().size();
    }

    @Override
    boolean equals(Object o);

    @Override
    int hashCode();


    default boolean recurseTerms(BiPredicate<Term, Term> whileTrue) {
        return recurseTerms(whileTrue, null);
    }


    boolean recurseTerms(BiPredicate<Term, Term> whileTrue, @Nullable Term parent);

    default boolean recurseTerms(Predicate<Term> parentsMust, Predicate<Term> whileTrue, Term parent) {
        return whileTrue.test(this);
    }

    @Override
    default boolean containsRecursively(Term t, Predicate<Term> inSubtermsOf) {
        return inSubtermsOf.test(this) && contains(t);
    }

    /**
     * whether any subterms (recursively) have
     * non-DTernal temporal relation
     */
    default boolean isTemporal() {
        return hasAny(Op.TemporalBits) &&
                (op().temporal && (dt() != DTERNAL))
                ||
                (subterms().isTemporal());
    }

    /**
     * returns an int[] path to the first occurrence of the specified subterm
     *
     * @return null if not a subterm, an empty int[] array if equal to this term, or a non-empty int[] array specifying subterm paths to reach it
     */
    @Nullable
    default byte[] pathTo(@NotNull Term subterm) {
        if (subterm.equals(this)) return ArrayUtils.EMPTY_BYTE_ARRAY;
        //if (!containsRecursively(subterm)) return null;
        return pathTo(new ByteArrayList(0), this, subterm);
    }


    @Nullable
    default Term transform(@NotNull ByteList path, Term replacement) {
        return transform(path, 0, replacement);
    }

    @Nullable
    default Term transform(int newDT, @NotNull CompoundTransform t) {
        return this;
    }

    @Nullable
    default Term transform(@NotNull ByteList path, int depth, Term replacement) {
        final Term src = this;
        int ps = path.size();
        if (ps == depth)
            return replacement;
        if (ps < depth)
            throw new RuntimeException("path overflow");

        int n = src.size();
        if (n == 0)
            return src; //path wont continue inside an atom

        Compound csrc = (Compound) src;

        Term[] target = new Term[n];

        for (int i = 0; i < n; i++) {
            Term x = csrc.sub(i);
            if (path.get(depth) != i)
                //unchanged subtree
                target[i] = x;
            else {
                //replacement is in this subtree
                target[i] = x.size() == 0 ? replacement : x.transform(path, depth + 1, replacement);
            }

        }

        return csrc.op().the(csrc.dt(), target);
    }


    @Nullable
    default <X> boolean pathsTo(@NotNull Function<Term, X> subterm, @NotNull BiPredicate<ByteList, X> receiver) {
        X ss = subterm.apply(this);
        if (ss != null) {
            if (!receiver.test(ByteLists.immutable.empty(), ss))
                return false;
        }
        return pathsTo(new ByteArrayList(0), this, subterm, receiver);
    }

    @Nullable
    static byte[] pathTo(@NotNull ByteArrayList p, Term superTerm, @NotNull Term target) {
        if (superTerm.impossibleSubTerm(target))
            return null;

        int n = superTerm.size();
        for (int i = 0; i < n; i++) {
            Term s = superTerm.sub(i);
            if (s.equals(target)) {
                p.add((byte) i);
                return p.toArray();
            }
            if (s.size() > 0) {
                byte[] pt = pathTo(p, s, target);
                if (pt != null) {
                    p.add((byte) i);
                    return pt;
                }

            }
        }

        return null;
    }

    @Nullable
    static <X> boolean pathsTo(@NotNull ByteArrayList p, Term superTerm, @NotNull Function<Term, X> subterm, @NotNull BiPredicate<ByteList, X> receiver) {


        int ppp = p.size();

        int n = superTerm.size();
        for (int i = 0; i < n; i++) {
            Term s = superTerm.sub(i);
            X ss = subterm.apply(s);

            p.add((byte) i);

            if (ss != null) {
                if (!receiver.test(p, ss))
                    return false;
            }
            if (s.size() > 0) {
                if (!pathsTo(p, s, subterm, receiver))
                    return false;
            }
            p.removeAtIndex(ppp);
        }

        return true;
    }


    @Nullable
    default Term commonParent(List<byte[]> subpaths) {
        int subpathsSize = subpaths.size();
        assert (subpathsSize > 0);

        int shortest = Integer.MAX_VALUE;
        for (int i = 0, subpathsSize1 = subpaths.size(); i < subpathsSize1; i++) {
            shortest = Math.min(shortest, subpaths.get(i).length);
        }

        //find longest common prefix
        int i;
        done:
        for (i = 0; i < shortest; i++) {
            byte toMatch = 0;
            for (int j = 0; j < subpathsSize; j++) {
                byte[] p = subpaths.get(j);
                if (j == 0) {
                    toMatch = p[i];
                } else if (toMatch != p[i]) {
                    break done; //first mismatch, done
                } //else: continue downwards
            }
            //all matched, proceed downward to the next layer
        }
        if (i == 0)
            return this;
        else {
            return sub(i, subpaths.get(0));
        }

    }

    @Nullable
    default Term sub(@NotNull ByteList path) {
        Term ptr = this;
        int s = path.size();
        for (int i = 0; i < s; i++)
            if ((ptr = ptr.sub(path.get(i))) == Null)
                return Null;
        return ptr;
    }

    /**
     * extracts a subterm provided by the address tuple
     * returns null if specified subterm does not exist
     */
    @Nullable
    default Term sub(@NotNull byte... path) {
        return sub(path.length, path);
    }

    @Nullable
    default Term sub(int n, @NotNull byte... path) {
        Term ptr = this;
        for (int i = 0; i < n; i++) {
            if ((ptr = ptr.sub((int) path[i])) == Null)
                return Null;
        }
        return ptr;
    }

    /**
     * Commutivity in NARS means that a Compound term's
     * subterms will be unique and arranged in order (compareTo)
     * <p>
     * <p>
     * commutative CompoundTerms: Sets, Intersections Commutative Statements:
     * Similarity, Equivalence (except the one with a temporal order)
     * Commutative CompoundStatements: Disjunction, Conjunction (except the one
     * with a temporal order)
     *
     * @return The default value is false
     */
    boolean isCommutative();


    /**
     * equlity has already been tested prior to calling this
     *
     * @param y     another term
     * @param subst the unification context
     * @return whether unification succeeded
     */
    default boolean unify(@NotNull Term y, @NotNull Unify subst) {
        return equals(y);
    }


    /**
     * true if the operator bit is included in the enabld bits of the provided vector
     */
    default boolean isAny(int bitsetOfOperators) {
        int s = op().bit;
        return (s & bitsetOfOperators) == s;
    }

//    /** for multiple Op comparsions, use Op.or to produce an int and call isAny(int vector) */
//    default boolean isA(@NotNull Op otherOp) {
//        return op() == otherOp;
//    }


//    default boolean hasAll(int structuralVector) {
//        final int s = structure();
//        return (s & structuralVector) == s;
//    }
//


    default boolean hasVarIndep() {
        return varIndep() != 0;
    }

//    /** returns the first ellipsis subterm or null if not present */
//    @Nullable
//    @Override default Ellipsis firstEllipsis() {
//        return null;
//    }

    default boolean hasVarDep() {
        return varDep() != 0;
    }

    default boolean hasVarQuery() {
        return varQuery() != 0;
    }


    void append(@NotNull Appendable w) throws IOException;

//    default public void append(Writer w, boolean pretty) throws IOException {
//        //try {
//            name().append(w, pretty);
////        } catch (IOException e) {
////            e.printStackTrace();
////        }
//    }

    //    default public StringBuilder toStringBuilder(boolean pretty) {
//        return name().toStringBuilder(pretty);
//    }

//    @Deprecated
//    String toString();
//    default public String toString(boolean pretty) {
//        return toStringBuilder(pretty).toString();
//    }


    @Override
    default boolean levelValid(int nal) {

        if (nal >= 8) return true;

        int mask = Op.NALLevelEqualAndAbove[nal];
        return (structure() | mask) == mask;
    }

    @NotNull
    default String structureString() {
        return String.format("%16s",
                Integer.toBinaryString(structure()))
                .replace(" ", "0");
    }


    @Override
    default boolean isNormalized() {
        return true;
    }

    /**
     * matches the first occuring event's time relative to this temporal relation, with parameter for a hypothetical dt
     *
     * @param dt the current offset in the search
     * @return DTERNAL if the subterm was not found
     */
    default int subtermTime(@NotNull Term x) {
        if (equals(x))
            return 0;

        if (impossibleSubTerm(x))
            return DTERNAL;

        int dt = dt();
        int idt;
        boolean reverse;

        //TODO do shuffled search to return different equivalent results wherever they may appear

        Op op = op();
        boolean shift;
        if (!op.temporal || dt == DTERNAL || dt == XTERNAL || dt == 0) {
            idt = 0; //parallel or eternal, no dt increment
            reverse = false;
            shift = false;
        } else {
            shift = op == CONJ;
            idt = dt;
            if (idt < 0) {
                idt = -idt;
                reverse = true;
            } else {
                reverse = false;
            }
        }

        @NotNull TermContainer yy = subterms();
        int ys = yy.size();
        int offset = 0;
        for (int yi = 0; yi < ys; yi++) {
            Term yyy = yy.sub(reverse ? ((ys - 1) - yi) : yi);
            int sdt = yyy.subtermTime(x);
            if (sdt != DTERNAL)
                return sdt + offset;
            offset += idt + ((shift && yyy.op() == CONJ) ? yyy.dtRange() : 0);
        }

        return DTERNAL; //not found
    }

    /**
     * total span across time represented by a sequence conjunction compound
     */
    default int dtRange() {
        Op o = op();
        switch (o) {

//            case NEG:
//                return sub(0).dtRange();


            case CONJ:

                if (size() == 2) {
                    int dt = dt();

                    switch (dt) {
                        case DTERNAL:
                        case XTERNAL:
                        case 0:
                            dt = 0;
                            break;
                        default:
                            dt = Math.abs(dt);
                            break;
                    }

                    return sub(0).dtRange() + (dt) + sub(1).dtRange();

                } else {
                    int s = 0;

                    TermContainer tt = subterms();
                    int l = tt.size();
                    for (int i = 0; i < l; i++) {
                        @NotNull Term x = tt.sub(i);
                        s = Math.max(s, x.dtRange());
                    }

                    return s;
                }

            default:
                return 0;
        }

    }


    /**
     * meta is int[] that collects term metadata:
     * 0: patternVar
     * 1: depVars
     * 2: indepVars
     * 3: queryVars
     * 4: volume
     * 5: struct
     * <p>
     * subclasses can override this for more efficient aggregation if certain features are sure to be absent
     */
    @Override
    default void init(@NotNull int[] meta) {

        if (vars() > 0) {
            meta[0] += varDep();
            meta[1] += varIndep();
            meta[2] += varQuery();
        }

        meta[3] += varPattern();
        meta[4] += volume();
        meta[5] |= structure();

    }

//    default boolean equalsIgnoringVariables(@NotNull Term other, boolean requireSameTime) {
//        return (this instanceof Variable) || (other instanceof Variable) || equals(other);
//    }


//    default public boolean hasAll(final Op... op) {
//        //TODO
//    }
//    default public boolean hasAny(final Op... op) {
//        //TODO
//    }

    @NotNull
    default ByteList structureKey() {
        return structureKey(new ByteArrayList(volume() * 2 /* estimate */));
    }

    @NotNull
    default ByteList structureKey(@NotNull ByteArrayList appendTo) {
        appendTo.add(op().id);
        return appendTo;
    }

    @NotNull
    default List<byte[]> pathsTo(Term subterm) {
        return pathsTo(subterm, 0);
    }

    @NotNull
    default List<byte[]> pathsTo(Term subterm, int minLengthOfPathToReturn) {
        List<byte[]> list = $.newArrayList(0);
        pathsTo(
                (x) -> x.equals(subterm) ? x : null,
                (l, t) -> {
                    if (l.size() >= minLengthOfPathToReturn) list.add(l.toArray());
                    return true;
                }
        );
        return list;
    }

    default boolean pathsTo(@NotNull Term subterm, @NotNull BiPredicate<ByteList, Term> receiver) {
        return pathsTo((x) -> subterm.equals(x) ? x : null, receiver);
    }


    /**
     * operator extended:
     * operator << 8 | sub-operator type rank for determing compareTo ordering
     */
    int opX();

    /**
     * GLOBAL TERM COMPARATOR FUNCTION
     */
    @Override
    default int compareTo(@NotNull Term y) {
        if (this.equals(y)) return 0;

        //order first by volume. this is important for conjunctions which rely on volume-dependent ordering for balancing
        //left should be heavier
        //compareTo semantics state that a -1 value means left is less than right. we want the opposite
        int diff2 = Integer.compare(y.volume(), volume());
        if (diff2 != 0)
            return diff2;

        int d = Integer.compare(this.opX(), y.opX());
        if (d != 0)
            return d;

        if (this instanceof Atomic) {

            assert (y instanceof Atomic) : "because volume should have been determined to be equal";

            if ((this instanceof AbstractVariable) /*&& (y instanceof AbstractVariable)*/) {
                //hashcode serves as the ordering too
                return Integer.compare(hashCode(), y.hashCode());
            } else if (this instanceof Int) {
                return Integer.compare(((Int)this).id, ((Int)y).id);
            } else if (this instanceof Int.IntRange) {
                return Long.compareUnsigned(((Int.IntRange)this).hash64(), ((Int.IntRange)y).hash64());
            } else if (this instanceof AtomicToString) {
//                boolean gx = this instanceof UnnormalizedVariable;
//                boolean gy = y instanceof UnnormalizedVariable;
//                if (gx && !gy)
//                    return -1;
//                if (!gx && gy)
//                    return +1;

                //if the op is the same, it is required to be a subclass of Atomic
                //which should have an ordering determined by its toString()
                return this.toString().compareTo((/*(Atomic)*/y).toString());
                //return Hack.compare(toString(), y.toString());
            } else {
                throw new UnsupportedOperationException("unimplemented comparison: " + this + " " + y);
            }


        } else {

            int c = TermContainer.compare(subterms(), y.subterms());
            if (c != 0)
                return c;

            return Integer.compare(dt(), y.dt());

        }

        //throw new RuntimeException("ordering exception: " + this + ", " + y);
    }

    @Override
    default TermContainer subterms() {
        return TermVector.NoSubterms;
    }


    /**
     * unwraps any negation superterm
     */
    @NotNull
    @Override
    default Term unneg() {
        if (op() == NEG) {
            Term x = sub(0);
            if (x instanceof Compound && isNormalized()) { //the unnegated content will also be normalized if this is
                ((Compound) x).setNormalized();
            }
            return x;
        }
        return this;
    }

    default Term eval(TermContext index) {
        return evalSafe(index, Param.MAX_EVAL_RECURSION);
    }

    @NotNull
    default Term evalSafe(TermContext index, int remain) {
        if (!(index instanceof StaticTermIndex)) {
            Termed t = index.get(this, false); //resolve
            if (t != null)
                return t.term();
        }
        return this;
    }

    /* collects any contained events */
    default void events(List<ObjectLongPair<Term>> events) {
        events(events, 0);
    }

    default void events(List<ObjectLongPair<Term>> events, long dt) {
        events.add(PrimitiveTuples.pair(this, dt));
    }

    default void printRecursive() {
        printRecursive(System.out);
    }

    default void printRecursive(@NotNull PrintStream out) {
        Terms.printRecursive(out, this);
    }

    @NotNull
    static Term nullIfNull(@Nullable Term maybeNull) {
        return (maybeNull == null) ? Null : maybeNull;
    }

//    /** https://google.github.io/guava/releases/snapshot/api/docs/com/google/common/collect/TreeTraverser.html */
//    default TreeTraverser<Term> termverse() {
//        return TreeTraverser.using(x -> x instanceof Compound ? ((Compound)x).subterms() : Collections.emptyList());
//    }

    /**
     * opX function
     */
    private static int opX(Op o, byte subOp) {
        return o.id << 8 | subOp;
    }

    /**
     * for convenience, delegates to the byte function
     */
    static int opX(@NotNull Op o, int subOp) {
        return opX(o, (byte) subOp);
    }

    /**
     * if filterTrueFalse is false, only filters Null's
     */
    static boolean invalidBoolSubterms(@NotNull Term u, boolean filterTrueFalse) {
        return u instanceof Bool && (filterTrueFalse || (u == Null));
    }

    default Term dt(int dt) {
//        if (dt!=DTERNAL)
//            throw new UnsupportedOperationException("temporality not supported");
        return this;
    }

    /**
     * return null if none, cheaper than using an empty iterator
     */
    @Nullable
    default Set<Term> varsUnique(@Nullable Op type) {
        return null;
    }

    @Nullable
    default Set<Term> varsUnique(@Nullable Op type, @Nullable Set<Term> exceptIfHere) {
        return null;
    }

    /**
     * returns the term which identifies the concept that this term would be categorized by.
     * ex: any temporality is atemporalized.
     */
    default @NotNull Term root() {
        return this;
    }

    /**
     * returns this term in a form which can identify a concept, or Null if it can't
     */
    default @NotNull Term conceptual() {
        return root();
    }

    /**
     * erases temporal information, for use in computing root()
     */
    default @NotNull Term eternal() {
        return this;
    }

    default int dt() {
        return DTERNAL;
    }

    default Term normalize(int offset) {
        return this; //no change
    }

    @Nullable
    default Term normalize() {
        return normalize(0);
    }


}

