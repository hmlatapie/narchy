package nars.budget;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Created by me on 10/28/16.
 */
@Disabled
public class TaskBudgetingTest {

    /** taskbudgeting - structural deduction test
    decompose large conj, analyze the durability, quality decay */
    @Test
    public void structuralDeduction1() throws Narsese.NarseseException {


        NAR d = NARS.tmp();


        d.log();

        d.believe("(&&, (a),(b),(c),(d),(e) )");

        d.run(100);

    }

}