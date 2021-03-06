package nars.term.compound;

import nars.Narsese;
import nars.term.Compound;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by me on 2/19/17.
 */
public class SerialCompoundTest {

    @Test
    public void test1() throws Narsese.NarseseException {

        //testSerialize("x");
        //testSerialize("#y");
        //testSerialize("\"sdhfdkjsf\"");
        assertEqual("a:b");
        assertEqual("(a ==>+1 b)");
        assertEqual("(&&,(MedicalCode-->MedicalIntangible),(MedicalIntangible-->#1),(SuperficialAnatomy-->#1),label(MedicalCode,MedicalCode),label(MedicalIntangible,MedicalIntangible),label(SuperficialAnatomy,SuperficialAnatomy))");

    }

    static void assertEqual(String x) throws Narsese.NarseseException {
        assertEqual($(x));
    }

    static void assertEqual(Compound x) {
        SerialCompound y = new SerialCompound(x);

        System.out.println(x + " encoded to " + y.length() + " bytes");

        assertTrue(y.length() > 1);

        Compound z = y.build();

        //assertNotSame(x, z); //<- when cached on construction, they may be the same
        assertEquals(x, z);
        assertEquals(x.subs(), z.subs());
        assertEquals(x.volume(), z.volume());

        assertEquals(x.toString(), z.toString());
    }
}