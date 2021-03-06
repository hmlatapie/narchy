/*
 * Copyright 2016, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jcog.constraint.discrete.search;

import jcog.constraint.discrete.propagation.PropagationQueue;
import jcog.constraint.discrete.trail.Trail;
import jcog.list.FasterList;
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public class DFSearch {

    private final PropagationQueue pQueue;
    private final Trail trail;

    private final FasterList<BooleanSupplier> decisions = new FasterList<>();
    private final FasterList<Runnable> solutionActions = new FasterList<>();

    private Objective objective;

    public DFSearch(PropagationQueue pQueue, Trail trail) {
        this.pQueue = pQueue;
        this.trail = trail;
    }

    public void addSolutionAction(Runnable action) {
        solutionActions.add(action);
    }

    public void foundSolution(SearchStats stats) {
        stats.nSolutions++;
        solutionActions.forEach(Runnable::run);
        if (objective != null) {
            objective.tighten();
        }
    }

    public void setObjective(Objective obj) {
        this.objective = obj;
    }

    private boolean propagate() {
        // Propagate the objective only if it is not null.
        boolean feasible = objective == null || objective.propagate();
        // Propagate the propagators only if the problem is still feasible.
        return feasible && pQueue.propagate();
    }

    /**
     * Starts the search
     *
     * @param heuristic     the search heursitic used to build the search tree.
     * @param stopCondition a predicate to stop the search.
     * @return A {@code SearchStats} object that contains some metrics related
     * to this tree search.
     */
    public SearchStats search(BooleanFunction<List<BooleanSupplier>> heuristic, Predicate<SearchStats> stopCondition) {
        SearchStats stats = new SearchStats();

        stats.startTime = System.currentTimeMillis();

        // Return if the root node is unfeasible.
        if (!propagate()) {
            stats.completed = true;
            return stats;
        }

        // Return if the root node is already a solution.
        if (heuristic.booleanValueOf(decisions)) {
            foundSolution(stats);
            stats.completed = true;
            return stats;
        }

        // Save the root state.
        trail.newLevel();

        // Start the search. The search terminates if the stack of decisions
        // is empty (meaning that the search tree has been entirely explored) or
        // if the stop condition is met.
        while (!decisions.isEmpty() && !stopCondition.test(stats)) {
            stats.nNodes++;

            // Apply the next decision and propagate. This can result in a failed
            // node in which case we restore the previous state.
            if (!decisions.removeLast().getAsBoolean() || !propagate()) {
                stats.nFails++;
                trail.undoLevel();
                continue;
            }

            // At this point we know that the new node is not failed and we check
            // that it is a solution or not.
            if (heuristic.booleanValueOf(decisions)) {
                foundSolution(stats);
                trail.undoLevel();
                continue;
            }

            // The node is neither a failed node or a solution so we continue to
            // explore the branch.
            trail.newLevel();
        }

        // The search is complete if there's no remaining decisions to be applied.
        stats.completed = decisions.isEmpty();

        // Clear the remaining decisions (if the search is incomplete) and restore
        // the state of the root node.
        trail.undoAll();
        decisions.clear();

        return stats;
    }
}
