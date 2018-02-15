/*
 * Copyright 2013-2014 Richard M. Hightower
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * __________                              _____          __   .__
 * \______   \ ____   ____   ____   /\    /     \ _____  |  | _|__| ____    ____
 *  |    |  _//  _ \ /  _ \ /    \  \/   /  \ /  \\__  \ |  |/ /  |/    \  / ___\
 *  |    |   (  <_> |  <_> )   |  \ /\  /    Y    \/ __ \|    <|  |   |  \/ /_/  >
 *  |______  /\____/ \____/|___|  / \/  \____|__  (____  /__|_ \__|___|  /\___  /
 *         \/                   \/              \/     \/     \/       \//_____/
 *      ____.                     ___________   _____    ______________.___.
 *     |    |____ ___  _______    \_   _____/  /  _  \  /   _____/\__  |   |
 *     |    \__  \\  \/ /\__  \    |    __)_  /  /_\  \ \_____  \  /   |   |
 * /\__|    |/ __ \\   /  / __ \_  |        \/    |    \/        \ \____   |
 * \________(____  /\_/  (____  / /_______  /\____|__  /_______  / / ______|
 *               \/           \/          \/         \/        \/  \/
 */
package org.boon.cache;

import org.junit.jupiter.api.Test;
import static org.boon.Exceptions.die;

public class SimpleConcurrentLRUCache {

    @Test
    public void test() {
        Cache<Integer, Integer> cache = new SimpleConcurrentCache<>(1, 4, false);
        cache.put(0, 0);
        cache.put(1, 1);
        cache.put(2, 2);
        cache.put(3, 3);
        boolean ok = cache.size() == 4 || die("size" + cache.size());
        cache.put(4, 4);
        cache.put(5, 5);
        ok |= cache.size() == 4 || die("size" + cache.size());
        ok |= cache.getSilent(2) == 2 || die();
        ok |= cache.getSilent(3) == 3 || die();
        ok |= cache.getSilent(4) == 4 || die();
        ok |= cache.getSilent(5) == 5 || die();
        cache.get(2);
        cache.get(3);
        cache.put(6, 6);
        cache.put(7, 7);
        ok |= cache.size() == 4 || die("size" + cache.size());
        ok |= cache.getSilent(2) == 2 || die();
        ok |= cache.getSilent(3) == 3 || die();
        cache.put(8, 8);
        cache.put(9, 9);
        ok |= cache.getSilent(4) == null || die();
        ok |= cache.getSilent(5) == null || die();
        if (!ok)
            die();
    }

    @Test
    public void test2() {
        Cache<Integer, Integer> cache = new SimpleConcurrentCache<>(400);
        cache.put(0, 0);
        cache.put(1, 1);
        cache.put(2, 2);
        cache.put(3, 3);
        for (int index = 0; index < 5_000; index++) {
            cache.get(0);
            cache.get(1);
            cache.put(2, index);
            cache.put(3, index);
            cache.put(index, index);
        }
        boolean ok = cache.getSilent(0) == 0 || die();
        ok |= cache.getSilent(1) == 1 || die();
        ok |= cache.getSilent(2) != null || die();
        ok |= cache.getSilent(3) != null || die();
        ok |= cache.size() < 600 || die();
        if (!ok)
            die();
    }
}
