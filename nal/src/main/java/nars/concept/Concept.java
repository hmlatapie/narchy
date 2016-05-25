/*
 * Concept.java
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
package nars.concept;

import com.google.common.collect.Iterators;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Longs;
import nars.NAR;
import nars.bag.Bag;
import nars.budget.Budgeted;
import nars.budget.policy.ConceptBudgeting;
import nars.concept.table.BeliefTable;
import nars.concept.table.QuestionTable;
import nars.concept.table.TaskTable;
import nars.task.Task;
import nars.term.Termed;
import nars.truth.Truth;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public interface Concept extends Termed, Comparable {

    Ordering<Task> taskCreationTime = new Ordering<Task>() {
        @Override
        public int compare(@NotNull Task left, @NotNull Task right) {
            return Longs.compare(
                    left.creation(),
                    right.creation());
        }
    };

    Bag<Task> tasklinks();

    Bag<Termed> termlinks();

    @Nullable
    Map<Object, Object> meta();

    default boolean hasGoals() {
        return !goals().isEmpty();
    }

    default boolean hasBeliefs() {
        return !beliefs().isEmpty();
    }

    default boolean hasQuestions() {
        return !questions().isEmpty();
    }

    default boolean hasQuests() {
        return !quests().isEmpty();
    }

    @Nullable
    Object put(@NotNull Object key, @Nullable Object value);

    /**
     * like Map.gett for getting data stored in meta map
     */
    @Nullable
    default <C> C get(@NotNull Object key) {
        Map<Object, Object> m;
        return null == (m = meta()) ? null : (C) m.get(key);
    }
    /** follows Map.compute() semantics */
    @NotNull
    <C> C meta(Object key, BiFunction value);

//    /**
//     * belief vs desire metric:
//     * positive = satisfied
//     * negative = desired
//     */
//    default float getSuccess(long now) {
//        //        return hasBeliefs() ?
////                getBeliefs().getMeanProjectedExpectation(now) : 0;
//        //        return hasGoals() ?
////            getGoals().getMeanProjectedExpectation(now) : 0;
//        return (hasBeliefs() && hasGoals())
//                ?
//                (beliefs().top(now).expectation() -
//                        goals().top(now).expectation()) : 0;
//    }

    @Nullable
    BeliefTable beliefs();

    @Nullable
    BeliefTable goals();

    @Nullable QuestionTable questions();


//    /** debugging utility */
//    default public void ensureNotDeleted() {
//        if (isDeleted())
//            throw new RuntimeException("Deleted concept should not activate TermLinks");
//    }

    @Nullable
    QuestionTable quests();

    @Nullable
    Task processBelief(Task task, NAR nar);

    @Nullable
    Task processGoal(Task task, NAR nar);

    @Nullable Task processQuestion(Task task, NAR nar);


//    /** returns the best belief of the specified types */
//    default public Task getStrongestBelief(boolean eternal, boolean nonEternal) {
//        return getBeliefs().top(eternal, nonEternal);
//    }
//
//
//    default public Task getStrongestGoal(boolean eternal, boolean nonEternal) {
//        return getGoals().top(eternal, nonEternal);
//    }

//
//    default Iterator<? extends Termed> getTermedAdjacents(boolean termLinks, boolean taskLinks) {
//        if (termLinks && taskLinks) {
//            return concat(
//                    getTermLinks().iterator(), getTaskLinks().iterator()
//            );
//        }
//        if (termLinks) {
//            return getTermLinks().iterator();
//        }
//        if (taskLinks) {
//            return getTaskLinks().iterator();
//        }
//
//        return null;
//    }

    @Nullable Task processQuest(Task task, NAR nar);

    default void print() {
        print(System.out);
    }

    default void print(@NotNull PrintStream out) {
        print(out, true, true, true, true);
    }


//    default Iterator<Term> adjacentTerms(boolean termLinks, boolean taskLinks) {
//        return transform(adjacentTermables(termLinks, taskLinks), Termed::getTerm);
//    }

//    default Iterator<Concept> adjacentConcepts(boolean termLinks, boolean taskLinks) {
//        final Iterator<Concept> termToConcept = transform(adjacentTerms(termLinks, taskLinks), new Function<Termed, Concept>() {
//            @Override
//            public Concept apply(final Termed term) {
//                return getMemory().concept(term.getTerm());
//            }
//        });
//        return filter(termToConcept, Concept.class); //should remove null's (unless they never get included anyway), TODO Check that)
//    }

    /**
     * prints a summary of all termlink, tasklink, etc..
     */
    default void print(@NotNull PrintStream out, boolean showbeliefs, boolean showgoals, boolean showtermlinks, boolean showtasklinks) {

        out.println("concept: " + toString());

        String indent = "  \t";

        Consumer<Task> printTask = s -> {
            out.print(indent);
            out.print(s);
            out.print(" ");
            out.print(s.lastLogged());
            out.println();
        };

        if (showbeliefs) {
            out.print(" Beliefs:");
            if (beliefs().isEmpty()) out.println(" none");
            else {
                out.println(); beliefs().forEach(printTask);
            }
            out.print(" Questions:");
            if (questions().isEmpty()) out.println(" none");
            else {
                out.println(); questions().forEach(printTask);
            }
        }

        if (showgoals) {
            out.print(" Goals:");
            if (goals().isEmpty()) out.println(" none");
            else {
                out.println();                goals().forEach(printTask);
            }
            out.print(" Quests:");
            if (questions().isEmpty()) out.println(" none");
            else {
                out.println();                quests().forEach(printTask);
            }
        }

        if (showtermlinks) {
            //out.print("TermLinkTemplates: ");
            //out.println(termlinkTemplates());

            out.println("\n TermLinks:");
            termlinks().forEach(b -> {
                out.print(indent);
                out.print(b.get() + " " + b.toBudgetString());
                out.print(" ");
            });
        }

        if (showtasklinks) {
            out.println("\n TaskLinks:");
            tasklinks().forEach(b -> {
                out.print(indent);
                out.print(b.get() + " " + b.toBudgetString());
                out.print(" ");
            });
        }

        out.println('\n');
    }

    @NotNull
    default Iterator<Task> iterateTasks(boolean onbeliefs, boolean ongoals, boolean onquestions, boolean onquests) {

        TaskTable beliefs = onbeliefs ? beliefs() : null;
        TaskTable goals = ongoals ? goals() : null;
        TaskTable questions = onquestions ? questions() : null;
        TaskTable quests = onquests ? quests() : null;

        Iterator<Task> b1 = beliefs != null ? beliefs.iterator() : Iterators.emptyIterator();
        Iterator<Task> b2 = goals != null ? goals.iterator() : Iterators.emptyIterator();
        Iterator<Task> b3 = questions != null ? questions.iterator() : Iterators.emptyIterator();
        Iterator<Task> b4 = quests != null ? quests.iterator() : Iterators.emptyIterator();
        return Iterators.concat(b1, b2, b3, b4);

    }


    /**
     * process a task in this concept
     *
     * @return true if process affected the concept (ie. was inserted into a belief table)
     */
    @Nullable
    Task process(@NotNull Task task, @NotNull NAR nar);

    /**
     * attempt insert a tasklink into this concept's tasklink bag
     * return true if successfully inserted
     */
    boolean link(@NotNull Budgeted task, float scale, float minScale, @NotNull NAR nar, @Nullable MutableFloat conceptOverflow);

    //void linkTemplates(Budget budget, float scale, NAR nar);


    default boolean link(@NotNull Budgeted b, float initialScale, @NotNull NAR nar, @Nullable MutableFloat conceptOverflow) {
        if (initialScale <= 0 || b.isDeleted())
            //throw new Budget.BudgetException();
            return false;

        float minScale =
                nar.taskLinkThreshold.floatValue() / b.pri();

        return (minScale==minScale /* fast NaN test */) && link(b, initialScale, minScale, nar, conceptOverflow);
    }

    /**
     * @param thisTask  task with a term equal to this concept's
     * @param otherTask task with a term equal to another concept's
     * @return number of links created (0, 1, or 2)
     */
    default void crossLink(@NotNull Task thisTask, @NotNull Task otherTask, float scale, @NotNull NAR nar) {

        Concept other = nar.concept(otherTask);
        if (other == null || other.equals(this))
            return; //null or same concept

        float halfScale = scale / 2f;

        link(otherTask, halfScale, nar, null);

        if (other != null)
            other.link(thisTask, halfScale, nar, null);

    }

    @Nullable
    default Truth belief(long when, long now) {
        return hasBeliefs() ? beliefs().truth(now, when) : Truth.Null;
    }

    @Nullable
    default Truth desire(long when, long now) {
        return hasGoals() ? goals().truth(now, when) : Truth.Null;
    }

    @Nullable
    default Truth belief(long now) {
        return belief(now, now);
    }

    @Nullable
    default Truth desire(long now) {
        return desire(now, now);
    }

    void capacity(ConceptBudgeting cold);

    boolean contains(Task t);

    /** produce a merge projected belief/goal given the two temporal inputs, both of which are expected
     * to be of this concept's term.
     * @param x may be a belief/goal OR question (truth is null)
     * @param y must be belief/goal
     * @return
     */
    @Nullable Task merge(Task x, Task y, long when, NAR nar);


//    public Task getTask(boolean hasQueryVar, long occTime, Truth truth, List<Task>... lists);
//
//    default public Task getTask(Sentence query, List<Task>... lists) {
//        return getTask(query.hasQueryVar(), query.getOccurrenceTime(), query.getTruth(), lists);
//    }

}
