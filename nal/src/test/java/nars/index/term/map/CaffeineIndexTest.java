package nars.index.term.map;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.term.Term;
import nars.term.Termed;
import org.junit.jupiter.api.Test;

class CaffeineIndexTest {

    @Test
    public void testDynamicWeight() throws Narsese.NarseseException {
        StringBuilder log = new StringBuilder();
        CaffeineIndex index;
        NAR n = new NARS().index(
            index = new CaffeineIndex(4000, (w) -> {
                int newWeight = Math.round(1000 *
                        (w.tasklinks().priSum() +
                        w.termlinks().priSum())
                );
                log.append("weigh ").append(w).append(' ').append(newWeight).append('\n');

//                System.err.println(w);
//                Thread.dumpStack();
//                System.err.println();


                return newWeight;
            }) {
                @Override
                public Termed get(Term x, boolean createIfMissing) {
                    log.append("get ").append(x).append(createIfMissing ? " createIfMissing\n" : "\n");
                    return super.get(x, createIfMissing);
                }
            }).get();


        n.believe("(x-->y).");

        n.believe("(x-->y).");

        System.out.println(log);

    }
}