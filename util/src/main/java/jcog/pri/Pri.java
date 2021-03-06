package jcog.pri;


import jcog.Util;

/**
 * default mutable prioritized implementation
 * float 32 bit
 *
 * TODO make completely atomic
 */
public class Pri implements Priority {

    protected volatile float pri;

    public Pri(Prioritized b) {
        this(b.pri());
    }

    public Pri(float p) {
        priSet(p);
    }

    /** default: pri=0 */
    public Pri() {

    }

    @Override
    public final float pri() {
        return pri;
    }

    @Override
    public boolean delete() {
        float p = pri;
        if (p==p) {
        //if (!isDeleted()) { //dont call isDeleted it may be overridden in a cyclical way
            this.pri = Float.NaN;
            return true;
        }
        //logger.warn("alredy deleted");
//            throw new RuntimeException("Already Deleted");
        return false;
    }




    /**
     * Fully display the BudgetValue
     *
     * @return String representation of the value
     */
    @Override
    public String toString() {
        return getBudgetString();
    }

    @Override
    public final float priSet(float p) {
        return this.pri = Util.unitize(p);
    }

}
