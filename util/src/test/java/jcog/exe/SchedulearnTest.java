package jcog.exe;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SchedulearnTest {

    @Test
    public void test1() {

        Can a = new Can();
        Can b = new Can();
        Can c = new Can();

        a.update(1, 0.5f, 1f);
        b.update(20, 5f, 2f);
        c.update(20, 5f, 3f); //same as 'a' but bigger observed supply

        float timeSlice = 10f;

        Schedulearn s = new Schedulearn();
        s.solve(List.of(a, b, c), timeSlice);

        System.out.println(a);
        System.out.println(b);
        System.out.println(c);

        assertTrue(b.iterations() > a.iterations());
        assertTrue(b.iterations() > c.iterations());
        assertTrue(c.iterations() > a.iterations());

        //overdemand
        assertTrue(a.iterations() > a.supply());

        double te = Schedulearn.estimatedTimeTotal(List.of(a, b, c));
        assertEquals(te, timeSlice, timeSlice/4f /* within 25% of the target */);

    }
}