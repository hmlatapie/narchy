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
package org.oakgp.util;

import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled
public class CacheMapTest {
//    @Test
//    public void testSynchronized() {
//        assertEquals("java.util.Collections$SynchronizedMap", CacheMap.createCache(3).getClass().getName());
//    }

    @Test
    public void testSizeThree() {
        final int maxSize = 3;
        Cache<String, Integer> m = CacheMap.createCache(maxSize);

        m.put("a", 1);
        m.put("b", 1);
        m.put("c", 1);
        m.put("d", 1);
        m.put("e", 1);

        assertEquals(maxSize, m.estimatedSize());
        assertTrue(m.getIfPresent("c")!=null);
        assertTrue(m.getIfPresent("d")!=null);
        assertTrue(m.getIfPresent("e")!=null);

        m.put("a", 1);

        assertEquals(maxSize, m.estimatedSize());
        assertTrue(m.getIfPresent("a")!=null);
        assertTrue(m.getIfPresent("d")!=null);
        assertTrue(m.getIfPresent("e")!=null);

        m.getIfPresent("d");
        m.put("x", 1);
        m.getIfPresent("d");
        m.put("y", 1);
        m.getIfPresent("d");
        m.put("z", 1);

        assertEquals(maxSize, m.estimatedSize());
        assertTrue(m.getIfPresent("d")!=null);
        assertTrue(m.getIfPresent("y")!=null);
        assertTrue(m.getIfPresent("z")!=null);

//        m.get("y");
//        m.get("z");
//        m.put("a", 1);
//        m.get("y");
//        m.get("z");
//        m.put("b", 1);
//        m.get("y");
//        m.get("z");
//        m.put("x", 1);
//
//        assertEquals(maxSize, m.size());
//        assertTrue(m.containsKey("x"));
//        assertTrue(m.containsKey("y"));
//        assertTrue(m.containsKey("z"));
//
//        m.put("q", 1);
//        m.put("r", 1);
//        m.put("y", 1);
//
//        assertEquals(maxSize, m.size());
//        assertTrue(m.containsKey("q"));
//        assertTrue(m.containsKey("r"));
//        assertTrue(m.containsKey("y"));
    }

//    @Test
//    public void testSizeTen() {
//        final int maxSize = 10;
//        Cache<Integer, String> m = CacheMap.createCache(maxSize);
//
//        for (int i = 0; i <= maxSize; i++) {
//            m.put(i, "");
//        }
//        assertEquals(maxSize, m.estimatedSize());
//        assertFalse(m.getIfPresent(0)!=null);
//
//        for (int i = 0; i <= maxSize; i++) {
//            m.put(i, "");
//            m.get(0);
//        }
//        assertEquals(maxSize, m.estimatedSize());
//        assertTrue(m.getIfPresent(0)!=null);
//    }
}
