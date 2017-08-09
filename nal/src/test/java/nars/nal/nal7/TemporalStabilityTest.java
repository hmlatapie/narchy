package nars.nal.nal7;

import nars.NAR;
import nars.Narsese;
import nars.Param;
import nars.Task;
import org.jetbrains.annotations.NotNull;


abstract public class TemporalStabilityTest {

    boolean unstable = false;

    //private final boolean stopOnFirstError = true;

    public void test(int cycles, @NotNull NAR n) throws Narsese.NarseseException {

        Param.DEBUG = true;

        n.log();
        n.onTask(this::validate);
//        n.onCycle(f -> {
//
//            TimeMap m = new TimeMap(n);
//
//            //Set<Between<Long>> times = m.keySetSorted();
//            /*if (times.size() < 3)
//                continue; //wait until the initial temporal model is fully constructed*/
//
//            //m.print();
//            m.forEach(tt -> {
//
//                validate(tt);
//            });
//
//
//        });

        input(n);

        run(cycles, n);

        assert(!unstable);
    }

    public void validate(Task t) {
        if (t.isInput()) {
            System.out.println("in: " + t);
            return;
        }

        if (!validOccurrence(t.start()) || !validOccurrence(t.end()))  {
            //if (irregular.add(t)) { //already detected?
                System.err.println("  instability: " + "\n" + t.proof() + "\n");
                unstable = true;
//                if (stopOnFirstError)
//                    n.stop();
            //}
        }
    }

    private void run(int cycles, NAR n) {

        if (cycles > 0) {

            n.run(cycles);

            //evaluate(n);
        }
    }

//    public void evaluate(@NotNull NAR n) {
//
//        if (!irregular.isEmpty()) {
//
////            TimeMap m = new TimeMap(n);
//
//            irregular.forEach(i -> {
//
//                System.err.println(i.proof());
//            });
//
//            //m.print();
//
//            assertTrue(false);
//        }
//
//    }


    abstract public boolean validOccurrence(long o);

    /**
     * inputs the tasks for a test
     */
    abstract public void input(NAR n) throws Narsese.NarseseException;
}
