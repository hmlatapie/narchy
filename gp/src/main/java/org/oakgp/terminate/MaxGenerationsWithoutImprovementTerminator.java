/*
 * Copyright 2015 S. Webber
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
package org.oakgp.terminate;

import org.oakgp.rank.RankedCandidates;

import java.util.function.Predicate;

/**
 * A predicate that returns {@code true} when a specified number of generations has since the last change in the fitness of candidates.
 */
public final class MaxGenerationsWithoutImprovementTerminator implements Predicate<RankedCandidates> {
    private final int maxGenerationsWithoutImprovement;
    private int currentGenerationsWithoutImprovement;
    private double currentBest;

    /**
     * Constructs a new {@code Predicate} that will return {@code true} once the given number of consecutive generations have been run without improvement.
     */
    public MaxGenerationsWithoutImprovementTerminator(int maxGenerationsWithoutImprovement) {
        this.maxGenerationsWithoutImprovement = maxGenerationsWithoutImprovement;
    }

    @Override
    public boolean test(RankedCandidates t) {
        double best = t.best().getFitness();
        if (best == currentBest) {
            return ++currentGenerationsWithoutImprovement >= maxGenerationsWithoutImprovement;
        } else {
            currentGenerationsWithoutImprovement = 0;
            currentBest = best;
            return false;
        }
    }
}
