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


import nars.$;
import nars.Op;
import nars.term.atom.Atomic;
import nars.term.variable.AbstractVariable;
import nars.term.variable.Variable;
import nars.util.data.array.IntArrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import static nars.nal.Tense.DTERNAL;


public interface Term extends Termed, Termlike, Comparable<Term> {




    static boolean equalAtemporally(@NotNull Termed a, @NotNull Termed<Compound> b) {

        //Term t = $.unNeg(term.term());
        //Term b = $.unNeg(beliefConcept.term());

        if (a.op() == b.op()) {
            if (a.structure() == b.structure() && a.volume() == b.volume()) {
                return b.equals($.terms.atemporalize((Compound) a));
            }
        }
        return false;
    }

    @NotNull
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

//    @Override
//    int size();

    @Override
    boolean equals(Object o);

    @Override
    int hashCode();

    void recurseTerms(@NotNull SubtermVisitor v);

    default void recurseTerms(@NotNull SubtermVisitorX v) {
        recurseTerms(v, null);
    }

    void recurseTerms(@NotNull SubtermVisitorX v, @Nullable Compound parent);


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


    //boolean hasVar(final Op type);


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

    default int subtermTime(Term x) {
        return subtermTime(x, this instanceof Compound ? ((Compound) this).dt() : DTERNAL);
    }
//    default long subtermTimeOrZero(Term x, long offset) {
//        int e = subtermTime(x, this instanceof Compound ? ((Compound)this).dt() : DTERNAL);
//        return e == DTERNAL ? DTERNAL : e + offset;
//    }

    /**
     * matches the first occuring event's time relative to this temporal relation, with parameter for a hypothetical dt
     */
    default int subtermTime(Term x, int dt) {

        if (this.equals(x))
            return 0;

        if (!this.op().temporal || dt == DTERNAL)
            return DTERNAL;

        Compound c = ((Compound) this);


        if (dt == 0) {
            if (this.containsTerm(x))
                return 0; //also handles &| multi-arg case
        } else if (this.size() == 2) {

            int firstIndex, lastIndex;

            if (isCommutative()) {
                //use the normalized order of the terms so that the first is always @ 0

                if (dt < 0) {
                    dt = -dt;

                    firstIndex = 1;
                    lastIndex = 0;
                } else {

                    firstIndex = 0;
                    lastIndex = 1;
                }
            } else {
                firstIndex = 0;
                lastIndex = 1;
            }

            Term first = c.term(firstIndex);
            if (first.equals(x))
                return 0;

            Term last = c.term(lastIndex);
            if (last.equals(x))
                return dt;

            int withinSubj = first.subtermTime(x);
            if (withinSubj != DTERNAL)
                return withinSubj;
            int withinPred = last.subtermTime(x);
            if (withinPred != DTERNAL)
                return dt + withinPred;

        } else {
            //throw new RuntimeException("invalid temporal type: " + this);
            return 0;
        }

        return DTERNAL;
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
    default int init(@NotNull int[] meta) {

        if (vars() > 0) {
            meta[0] += varDep();
            meta[1] += varIndep();
            meta[2] += varQuery();
        }

        meta[3] += varPattern();
        meta[4] += volume();
        meta[5] |= structure();

        return hashCode();
    }

    default boolean equalsIgnoringVariables(@NotNull Term other) {
        return (this instanceof Variable) || (other instanceof Variable) || equals(other);
    }


//    default public boolean hasAll(final Op... op) {
//        //TODO
//    }
//    default public boolean hasAny(final Op... op) {
//        //TODO
//    }

    /** returns an int[] path to the first occurrence of the specified subterm
     * @return null if not a subterm, an empty int[] array if equal to this term, or a non-empty int[] array specifying subterm paths to reach it
     */
    @Nullable default int[] pathTo(Term subterm) {
        if (subterm.equals(this))
            return IntArrays.EMPTY_ARRAY;
        return null;
    }


    /** GLOBAL TERM COMPARATOR FUNCTION */
    @Override
    default int compareTo(Term y) {
        if (this.equals(y)) return 0;

        int d = this.op().compareTo(y.op());
        if (d!=0)
            return d;

        if (this instanceof Compound) {

            Compound cthis = (Compound)this;
            Compound cy = (Compound)y;

            int diff3 = Integer.compare(cthis.dt(), cy.dt());
            if (diff3 != 0)
                return diff3;

            return cthis.subterms().compareTo(cy.subterms());

        } else if (this instanceof AbstractVariable) {
            //hashcode serves as the ordering too
            return Integer.compare(this.hashCode(), y.hashCode());
        } else if (this instanceof Atomic) {
            //if the op is the same, it is required to be a subclass of Atomic
            //which should have an ordering determined by its toString()
            return this.toString().compareTo((/*(Atomic)*/y).toString());
        }

        throw new RuntimeException("ordering ethisception: " + this + ", " + y);
    }
}

