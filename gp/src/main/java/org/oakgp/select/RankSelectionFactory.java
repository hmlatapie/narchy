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
package org.oakgp.select;

import org.oakgp.rank.RankedCandidates;
import org.oakgp.util.Random;

/**
 * Returns instances of {@code RankSelection}.
 */
public final class RankSelectionFactory implements NodeSelectorFactory {
    private final Random random;

    /**
     * Creates a {@code RankSelectionFactory} that creates {@code RankSelection} instances that use the given {@code Random}.
     */
    public RankSelectionFactory(Random random) {
        this.random = random;
    }

    @Override
    public RankSelection getSelector(RankedCandidates candidates) {
        return new RankSelection(random, candidates);
    }
}
