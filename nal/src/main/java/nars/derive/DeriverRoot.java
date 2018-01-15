package nars.derive;

import jcog.memoize.HijackMemoize;
import jcog.memoize.Memoize;
import nars.control.Derivation;
import nars.control.ProtoDerivation;
import nars.term.pred.PrediTerm;

/** what -> can */
public final class DeriverRoot {

    public final PrediTerm<Derivation> what;
    public final Try can;

    public DeriverRoot(PrediTerm<Derivation> what, Try can) {
        //this.id = ($.p(what, can ));
        this.what = what;
        this.can = can;
    }


    public void printRecursive() {

        what.printRecursive();
        can.printRecursive();

    }
}
