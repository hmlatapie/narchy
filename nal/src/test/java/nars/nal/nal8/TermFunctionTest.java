package nars.nal.nal8;

import nars.IO;
import nars.Narsese;
import nars.Param;
import nars.nar.Default;
import nars.nar.Terminal;
import nars.term.Term;
import nars.test.TestNAR;
import org.junit.Test;

import static nars.$.$;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class TermFunctionTest {

    @Test
    public void testImmediateTransformOfInput() { //as opposed to deriver's use of it
        Default d = new Default();


        d.log();
        final boolean[] got = {false};
        d.onTask(t -> {
            String s = t.toString();
            if (s.contains("union"))
               assertTrue(false);
            if (s.contains("[a,b]"))
                got[0] = true;
        });
        d.input("union([a],[b]).");

        d.run(1);

        assertTrue(got[0]);
    }

    @Test
    public void testAdd1() {
        Default d = new Default();
        d.log();
        d.input("add(1,2,#x)!");
        d.run(16);
        d.input("add(4,5,#x)!");
        d.run(16);
    }

    @Test
    public void testAdd1Temporal() {
        Default d = new Default();
        d.log();
        d.input("add(1,2,#x)! :|:");
        d.run(16);
        d.input("add(4,5,#x)! :|:");
        d.run(16);
    }

    @Test
    public void testFunctor1() {
        Param.DEBUG = true;

        TestNAR t = new TestNAR(new Default());
        //t.log();
        t.believe("((complexity($1)<->3)==>c3($1))");
        //t.believe("--(2<->3)");
        t.ask("c3(x:y)");
        //t.ask("c3((x))");
        t.mustBelieve(16, "c3(x:y)", 1f, 0.81f);
        t.run(true);
    }

    @Test
    public void testFunctor2() {
        Param.DEBUG = true;

        TestNAR t = new TestNAR(new Default(1024,64,2,2));
        t.log();
        t.believe("(equal(complexity($1),$2) <=> c($1,$2))");
        t.ask("c(x, 1)");
        t.ask("c(x, 2)");
        t.ask("c((y), 1)");
        t.ask("c((y), 2)");
        t.mustBelieve(64, "c(x,1)", 1f, 0.81f);
        t.mustBelieve(64, "c(x,2)", 0f, 0.81f);
        t.mustBelieve(64, "c((y),1)", 0f, 0.81f);
        t.mustBelieve(64, "c((y),2)", 1f, 0.81f);
        t.run(true);
    }

//    @Test
//    public void testExecutionREsultIsCondition() {
//        Default d = new Default();
//        d.log();
//        d.input("(add(#x,1,#y) <=> inc(#x,#y)).");
//        d.input("add(1,2,#x)! :|:");
//        d.run(16);
//        d.input("add(3,4,#x)! :|:");
//        d.run(16);
//    }

    @Test
    public void testJSON1() throws Narsese.NarseseException {
        Term t = IO.fromJSON("{ \"a\": [1, 2], \"b\": \"x\", \"c\": { \"d\": 1 } }");
        assertEquals($("{(\"x\"-->b),a(1,2),({(1-->d)}-->c)}").toString(), t.toString());
    }

    @Test
    public void testJSON2() {
        assertEquals("{a(1,2)}", IO.fromJSON("{ \"a\": [1, 2] }").toString());
    }

    @Test
    public void testParseJSONTermFunction() throws Narsese.NarseseException {
        Term u = new Terminal().inputAndGet("jsonParse(\"{ \"a\": [1, 2] }\").").term();
        assertEquals("{a(1,2)}", u.toString());
    }
    @Test
    public void testToStringJSONTermFunction() throws Narsese.NarseseException {
        Term u = new Terminal().inputAndGet("jsonStringify((x,y,z)).").term();
        assertEquals("json(\"[\"x\",\"y\",\"z\"]\")", u.toString());
    }

}
