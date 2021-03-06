package jcog.bloom;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Copyright 2014 Prasanth Jayachandran
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class CountMinSketchTest {

    @Test
    public void testWidth() {
        CountMinSketch cms = new CountMinSketch();
        assertEquals(272, cms.w);
    }

    @Test
    public void testDepth() {
        CountMinSketch cms = new CountMinSketch();
        assertEquals(5, cms.depth());
    }

    @Test
    public void testSizeInBytes() {
        CountMinSketch cms = new CountMinSketch();
        assertEquals(5448, cms.byteSize());
        cms = new CountMinSketch(1024, 10);
        assertEquals(40968, cms.byteSize());
    }

    @Test
    public void testCMSketch() {
        CountMinSketch cms = new CountMinSketch(1024, 10);
        cms.add("Hello".getBytes());
        cms.add("Hello".getBytes());
        cms.add("Hello".getBytes());
        cms.add("Hello".getBytes());
        cms.add("HelloWorld".getBytes());
        assertEquals(4, cms.count("Hello".getBytes()));
        assertEquals(1, cms.count("HelloWorld".getBytes()));

        int[] actualFreq = new int[100];
        Random rand = new Random(123);
        CountMinSketch cms3 = new CountMinSketch();
        for (int i = 0; i < 10000; i++) {
            int idx = rand.nextInt(actualFreq.length);
            cms3.add(idx);
            actualFreq[idx] += 1;
        }

        assertEquals(actualFreq[10], cms3.count(10), 0.01);
        assertEquals(actualFreq[20], cms3.count(20), 0.01);
        assertEquals(actualFreq[30], cms3.count(30), 0.01);
        assertEquals(actualFreq[40], cms3.count(40), 0.01);
        assertEquals(actualFreq[50], cms3.count(50), 0.01);
        assertEquals(actualFreq[60], cms3.count(60), 0.01);
    }

    @Test
    public void testMerge() {
        CountMinSketch cms = new CountMinSketch(1024, 10);
        cms.add("Hello".getBytes());
        cms.add("Hello".getBytes());
        cms.add("Hello".getBytes());
        cms.add("Hello".getBytes());
        CountMinSketch cms2 = new CountMinSketch(1024, 10);
        cms2.add("Hello");
        cms2.add("Hello");
        cms2.add("Hello");
        cms2.add("Hello");
        cms.merge(cms2);
        assertEquals(8, cms.count("Hello"));

        int[] actualFreq = new int[100];
        Random rand = new Random(123);
        CountMinSketch cms3 = new CountMinSketch();
        for (int i = 0; i < 10000; i++) {
            int idx = rand.nextInt(actualFreq.length);
            cms3.add(idx);
            actualFreq[idx] += 1;
        }

        assertEquals(actualFreq[10], cms3.count(10), 0.01);
        assertEquals(actualFreq[20], cms3.count(20), 0.01);
        assertEquals(actualFreq[30], cms3.count(30), 0.01);
        assertEquals(actualFreq[40], cms3.count(40), 0.01);
        assertEquals(actualFreq[50], cms3.count(50), 0.01);
        assertEquals(actualFreq[60], cms3.count(60), 0.01);

        int[] actualFreq2 = new int[100];
        rand = new Random(321);
        CountMinSketch cms4 = new CountMinSketch();
        for (int i = 0; i < 10000; i++) {
            int idx = rand.nextInt(actualFreq2.length);
            cms4.add(idx);
            actualFreq2[idx] += 1;
        }
        cms3.merge(cms4);

        assertEquals(actualFreq[10] + actualFreq2[10], cms3.count(10), 0.01);
        assertEquals(actualFreq[20] + actualFreq2[20], cms3.count(20), 0.01);
        assertEquals(actualFreq[30] + actualFreq2[30], cms3.count(30), 0.01);
        assertEquals(actualFreq[40] + actualFreq2[40], cms3.count(40), 0.01);
        assertEquals(actualFreq[50] + actualFreq2[50], cms3.count(50), 0.01);
        assertEquals(actualFreq[60] + actualFreq2[60], cms3.count(60), 0.01);
    }

    @Test
    public void testIncompatibleMerge() {
        assertThrows(RuntimeException.class, () -> {
            CountMinSketch cms = new CountMinSketch(1024, 10);
            cms.add("Hello".getBytes());
            cms.add("Hello".getBytes());
            cms.add("Hello".getBytes());
            cms.add("Hello".getBytes());
            CountMinSketch cms2 = new CountMinSketch(1024, 11);
            cms2.add("Hello");
            cms2.add("Hello");
            cms2.add("Hello");
            cms2.add("Hello");

            // should throw exception
            cms.merge(cms2);

        });
    }

    @Test
    public void testSerialization() {
        CountMinSketch cms = new CountMinSketch(1024, 10);
        cms.add("Hello".getBytes());
        cms.add("Hello".getBytes());
        cms.add("Hello".getBytes());
        cms.add("Hello".getBytes());
        byte[] serialized = CountMinSketch.serialize(cms);
        assertEquals(cms.byteSize(), serialized.length);

        CountMinSketch cms2 = CountMinSketch.deserialize(serialized);
        cms2.add("Hello");
        cms2.add("Hello");
        cms2.add("Hello");
        cms2.add("Hello");
        cms.merge(cms2);
        assertEquals(cms.byteSize(), serialized.length);

        int[] actualFreq = new int[100];
        Random rand = new Random(123);
        CountMinSketch cms3 = new CountMinSketch();
        for (int i = 0; i < 10000; i++) {
            int idx = rand.nextInt(actualFreq.length);
            cms3.add(idx);
            actualFreq[idx] += 1;
        }

        assertEquals(actualFreq[10], cms3.count(10), 0.01);
        assertEquals(actualFreq[20], cms3.count(20), 0.01);
        assertEquals(actualFreq[30], cms3.count(30), 0.01);
        assertEquals(actualFreq[40], cms3.count(40), 0.01);
        assertEquals(actualFreq[50], cms3.count(50), 0.01);
        assertEquals(actualFreq[60], cms3.count(60), 0.01);

        serialized = CountMinSketch.serialize(cms3);
        CountMinSketch cms4 = CountMinSketch.deserialize(serialized);
        assertEquals(actualFreq[10], cms4.count(10), 0.01);
        assertEquals(actualFreq[20], cms4.count(20), 0.01);
        assertEquals(actualFreq[30], cms4.count(30), 0.01);
        assertEquals(actualFreq[40], cms4.count(40), 0.01);
        assertEquals(actualFreq[50], cms4.count(50), 0.01);
        assertEquals(actualFreq[60], cms4.count(60), 0.01);

        cms4.add(Integer.MAX_VALUE);
        cms4.add(Integer.MAX_VALUE);
        cms4.add(Integer.MAX_VALUE);
        cms4.add(Integer.MIN_VALUE);
        cms4.add(Integer.MIN_VALUE);
        assertEquals(3, cms4.count(Integer.MAX_VALUE), 0.01);
        assertEquals(2, cms4.count(Integer.MIN_VALUE), 0.01);
    }

    @Test
    public void testCountMinRoar1() {
        CountMinRoar c = new CountMinRoar();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < i; j++)
                c.add(i); //i x j
        }
        System.out.println(c.toString() + " " + c.summary());
        assertTrue(c.toString().startsWith("1x1,2x2,3x3,4x4,5x5,6x6,7x7,8x8,9x9"));
    }
}
