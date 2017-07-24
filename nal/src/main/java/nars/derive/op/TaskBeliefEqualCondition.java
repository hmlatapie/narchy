package nars.derive.op;

import nars.control.premise.Derivation;
import nars.derive.AtomicPred;
import org.jetbrains.annotations.NotNull;

/** matches the possibility that one half of the premise must be contained within the other.
 * this would in theory be more efficient than performing a complete match for the redundancies
 * which we can determine as a precondition of the particular task/belief pair
 * before even beginning the match. */
final class TaskBeliefEqualCondition extends AtomicPred<Derivation> {

    @Override
    public boolean test(@NotNull Derivation m) {
        return m.taskTerm.equals(m.beliefTerm);
    }

    @Override
    public String toString() {
        return "taskbeliefEq";
    }
}