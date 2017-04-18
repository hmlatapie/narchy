package jcog.bag;

import com.google.common.base.Joiner;
import jcog.pri.PLink;
import jcog.pri.RawPLink;
import jcog.bag.impl.hijack.DefaultHijackBag;
import jcog.pri.PriMerge;
import org.apache.commons.math3.random.EmpiricalDistribution;
import org.junit.Test;

import java.util.Random;
import java.util.TreeSet;

import static jcog.bag.BagTest.*;
import static jcog.pri.PriMerge.max;
import static jcog.pri.PriMerge.plus;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 2/9/17.
 */
public class HijackBagTest {

    @Test public void testSamplingFlatHijack() {
        testSamplingFlat(new DefaultHijackBag<String>(64, 4, max, rng()), 0.076f);
    }

    @Test public void testRemoveByKey() {
        BagTest.testRemoveByKey(new DefaultHijackBag(2, 3, plus, rng()));
    }


    @Test
    public void testScalePutHija() {
        BagTest.testScalePut(new DefaultHijackBag(2, 1, max, rng()));
        BagTest.testScalePut(new DefaultHijackBag(2, 2, max, rng()));
        BagTest.testScalePut2(new DefaultHijackBag(2, 1, plus, rng()));
        BagTest.testScalePut2(new DefaultHijackBag(2, 2, plus, rng()));
    }

    @Test
    public void testBasicInsertionRemovalHijack() {
        testBasicInsertionRemoval(new DefaultHijackBag(1, 1, max, rng()));
    }

    @Test public void testHijackFlatBagRemainsRandomInNormalizedSampler() {

        int n = 64;

        Bag<String,PLink<String>> a = new DefaultHijackBag<String>(n, 4, max, rng());
        for (int i = 0; i < n; i++) {
            a.put(new RawPLink("x" + Integer.toString(Float.floatToIntBits(1f/i),5), ((float)(i))/(n)));
        }

        a.commit();
        int size = a.size();
        //assertTrue(size >= 20 && size <= 30);

//        TreeSet<String> keys = new TreeSet();
//        Iterators.transform(a.iterator(), x -> x.get()).forEachRemaining(keys::add);
//        System.out.println( keys.size() + " " + Joiner.on(' ').join(keys) );

        TreeSet<String> keys2 = new TreeSet();
        a.forEach((b)->{
            if (!keys2.add(b.get()))
                throw new RuntimeException("duplicate detected");
        });
        System.out.println( keys2.size() + " " + Joiner.on(' ').join(keys2) );

        assertEquals(size, keys2.size());

        int b = 20;
        EmpiricalDistribution e = BagTest.getSamplingPriorityDistribution(a, n * 500, b);

        printDist(e);

        //monotonically increasing:
        assertTrue(e.getBinStats().get(0).getMean() < e.getBinStats().get(b-1).getMean());
        //assertTrue(e.getBinStats().get(0).getMean() < e.getBinStats().get(b/2).getMean());
        //assertTrue(e.getBinStats().get(b/2).getMean() < e.getBinStats().get(b-2).getMean());

        //a.print();
    }


    @Test
    public void testHijackResize() {
        Random rng = rng();
        DefaultHijackBag b = new DefaultHijackBag(0, 7, PriMerge.max, rng);
        BagTest.populate(b, rng, 10, 20, 0f, 1f, 0.5f);
        assertEquals(b.reprobes /*0*/, b.size());


        int dimensionality = 50;
        b.setCapacity(dimensionality * 2);

        BagTest.populate(b, rng, dimensionality*5, dimensionality, 0f, 1f, 0.5f);
        System.out.println("under capacity");
        b.print();
        assertApproximatelySized(b, dimensionality, 0.5f);

        b.setCapacity(dimensionality/2*2);

        System.out.println("half capacity");
        b.print();

        assertApproximatelySized(b, dimensionality/2*2, 0.5f);

        BagTest.populate(b, rng, dimensionality*3, dimensionality, 0f, 1f, 0.5f);
        System.out.println("under capacity, refilled");
        b.print();

        //test


        b.setCapacity(dimensionality*2);

        BagTest.populate(b, rng, dimensionality*3, dimensionality, 0f, 1f, 0.5f);
        System.out.println("under capacity, expanded");
        b.print();

        assertApproximatelySized(b, dimensionality, 0.25f);
        //test


    }

    public void assertApproximatelySized(Bag<String,?> b, int expected, float closeness) {
        int bSize = b.size();
        float error = Math.abs(expected - bSize) / (Math.max(bSize, (float) expected));
        System.out.println(bSize + "  === " + expected + ", diff=" + error);
        assertTrue(error < closeness);
    }
}