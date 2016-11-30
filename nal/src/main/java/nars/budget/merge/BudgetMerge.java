package nars.budget.merge;

import nars.Param;
import nars.budget.Budget;
import nars.budget.Budgeted;
import nars.link.BLink;
import nars.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

import static nars.Param.BUDGET_EPSILON;
import static nars.budget.merge.BudgetMerge.PriMerge.*;
import static nars.util.UtilityFunctions.or;
import static nars.util.Util.lerp;

/**
 * Budget merge function, with input scale factor
 */
@FunctionalInterface
public interface BudgetMerge extends BiFunction<Budget, Budget, Budget> {


    /** merge 'incoming' budget (scaled by incomingScale) into 'existing'
     *  incomingScale is a factor (0 < s <= 1) by which the incoming budget's effect is multiplied,
     *  1.0 being complete merge and 0 being no effect at all.
     *
     * @return any resultng overflow priority which was not absorbed by the target, >=0
     * */
    float merge(Budget existing, Budgeted incoming, float incomingScale);


    @Nullable
    @Override
    default Budget apply(@NotNull Budget existing, @NotNull Budget incoming) {
        return apply(existing, incoming, 1f);
    }

    @Nullable
    default Budget apply(@NotNull Budget target, @NotNull Budget incoming, float scale) {
        merge(target, incoming, scale);
        return target;
    }

    default float merge(BLink prev, BLink next) {
        return merge(prev, next, 1f);
    }

    enum PriMerge {
        PLUS,
        AVERAGE,
        AND,
        OR
    }

    /** srcScale only affects the amount of priority adjusted; for the other components, the 'score'
     * calculations are used to interpolate
     //TODO will this work for a possible negative pri value case?
     * */
    static float blend(@NotNull Budget tgt, @NotNull Budgeted src, float sScale, @NotNull PriMerge priMerge) {

        float tPri = tgt.priIfFiniteElseZero();
        boolean hasTP = tPri >= BUDGET_EPSILON;
        float sPri = src.priIfFiniteElseZero();
        boolean hasSP = sPri >= BUDGET_EPSILON && sScale >= BUDGET_EPSILON;

        if (!hasSP) {
            if (hasTP) {
                return 0; //nothing to do; zero incoming doesnt budge the target budget
            }
        } else {
            if (!hasTP) {
                tgt.setBudget(sPri * sScale, src.qua()); //target has no influence, it becomes set entirely by incoming
                return 0;
            }
        }

        float newPri;
        switch (priMerge) {
            case PLUS:    newPri = tPri + sPri;       break;
            case AND:     newPri = tPri * sPri;       break;
            case OR:      newPri = or(tPri,sPri);     break;
            case AVERAGE: newPri = (tPri + sPri)/2f;  break;
            default:
                throw new UnsupportedOperationException();
        }

        newPri = lerp(newPri, tPri, sScale);


        float overflow;
        if (newPri > 1f) {
            overflow = newPri - 1f;
            newPri = 1f;
        } else {
            overflow = 0;
        }

        float tgtInfluence;
        if (tPri + newPri < Param.BUDGET_EPSILON)
            tgtInfluence = 0.5f;
        else
            tgtInfluence = (tPri / (tPri + newPri));

        tgt.setBudget(
                newPri,
                (tgtInfluence * tgt.qua()) + ((1f-tgtInfluence)  * src.qua()));

        return overflow;
    }

//    static float dqBlendByPri(@NotNull Budget tgt, @NotNull Budgeted src, float srcScale, boolean addOrAvgPri) {
//        float incomingPri = src.priIfFiniteElseZero() * srcScale;
//
//        float currentPri = tgt.priIfFiniteElseZero();
//
//        float sumPri = currentPri + incomingPri;
//
//        float cp = sumPri > 0 ? currentPri / sumPri : 0.5f; // current proportion
//
//        return dqBlend(tgt, src, addOrAvgPri ?
//                sumPri :
//                ((cp * currentPri) + ((1f-cp) * incomingPri)), cp);
//    }
//    static float dqBlendBySummary(@NotNull Budget tgt, @NotNull Budgeted src, float srcScale, boolean addOrAvgPri) {
//        float incomingPri = src.pri() * srcScale;
//        float incomingSummary = src.summary() * srcScale;
//
//        float currentPri = tgt.priIfFiniteElseZero();
//        float currentSummary = tgt.summary();
//
//        float sumSummary = currentSummary + incomingSummary;
//
//        float cp = currentSummary / sumSummary; // current proportion
//
//        return dqBlend(tgt, src, addOrAvgPri ?
//                currentPri + incomingPri :
//                ((cp * currentPri) + ((1f-cp) * incomingPri)), cp);
//    }

    BudgetMerge errorMerge = (x, y, z) -> {
        throw new UnsupportedOperationException();
    };

    BudgetMerge nullMerge = (x, y, z) -> {
        //nothing
        return 0f;
    };

    /** sum priority, LERP other components in proportion to the priorities */
    BudgetMerge plusBlend = (tgt, src, srcScale) -> blend(tgt, src, srcScale, PLUS);

    /** avg priority, LERP other components in proportion to the priorities */
    BudgetMerge avgBlend = (tgt, src, srcScale) -> blend(tgt, src, srcScale, AVERAGE);

    /** or priority, LERP other components in proportion to the priorities */
    BudgetMerge orBlend = (tgt, src, srcScale) -> blend(tgt, src, srcScale, OR);


    /** AND priority, LERP other components in proportion to the priorities */
    BudgetMerge andBlend = (tgt, src, srcScale) -> blend(tgt, src, srcScale, AND);


//    @Deprecated BudgetMerge plusDQDominant = (tgt, src, srcScale) -> {
//        float nextPriority = src.priIfFiniteElseZero() * srcScale;
//
//        float currentPriority = tgt.priIfFiniteElseZero();
//
//        float sumPriority = currentPriority + nextPriority;
//        float overflow;
//        if (sumPriority > 1) {
//            overflow = sumPriority - 1f;
//            sumPriority = 1f;
//        } else {
//            overflow = 0;
//        }
//
//        boolean currentWins = currentPriority > nextPriority;
//
//        tgt.budget( sumPriority,
//                (currentWins ? tgt.dur() : src.dur()),
//                (currentWins ? tgt.qua() : src.qua()));
//
//        return overflow;
//    };

//    /** add priority, interpolate durability and quality according to the relative change in priority
//     *  WARNING untested
//     * */
//    BudgetMerge plusDQInterp = (tgt, src, srcScale) -> {
//        float dp = src.pri() * srcScale;
//
//        float currentPriority = tgt.priIfFiniteElseZero();
//
//        float nextPri = currentPriority + dp;
//        if (nextPri > 1) nextPri = 1f;
//
//        float currentNextPrioritySum = (currentPriority + nextPri);
//
//        /* current proportion */
//        final float cp = currentNextPrioritySum != 0 ? currentPriority / currentNextPrioritySum : 0.5f;
//
//        /* next proportion = 1 - cp */
//        float np = 1.0f - cp;
//
//
//        float nextDur = (cp * tgt.dur()) + (np * src.dur());
//        float nextQua = (cp * tgt.qua()) + (np * src.qua());
//
//        if (!Float.isFinite(nextDur))
//            throw new RuntimeException("NaN dur: " + src + ' ' + tgt.dur());
//        if (!Float.isFinite(nextQua))
//            throw new RuntimeException("NaN quality");
//
//        tgt.budget( nextPri, nextDur, nextQua );
//    };

    /** add priority, interpolate durability and quality according to the relative change in priority
     *  WARNING untested
     * */
    BudgetMerge max = (tgt, src, srcScaleIgnored) -> {
        tgt.setBudget(
            Util.max(src.priIfFiniteElseZero(), tgt.priIfFiniteElseZero()),
                Util.max(src.qua(), tgt.qua()));
        return 0;
    };


//    /** the max priority, durability, and quality of two tasks */
//    default Budget mergeMax(Budget b) {
//        return budget(
//                Util.max(getPriority(), b.getPriority()),
//                Util.max(getDurability(), b.getDurability()),
//                Util.max(getQuality(), b.getQuality())
//        );
//    }


//    /**
//     * merges another budget into this one, averaging each component
//     */
//    default void mergeAverageLERP(Budget that) {

//    }

//    /* ----------------------- Concept ----------------------- */
//    /**
//     * Activate a concept by an incoming TaskLink
//     *
//     *
//     * @param factor linear interpolation factor; 1.0: values are applied fully,  0: values are not applied at all
//     * @param receiver The budget receiving the activation
//     * @param amount The budget for the new item
//     */
//    public static void activate(final Budget receiver, final Budget amount, final Activating mode, final float factor) {
//        switch (mode) {
//            /*case Max:
//                receiver.max(amount);
//                break;*/
//
//            case Accum:
//                receiver.accumulate(amount);
//                break;
//
//            case Classic:
//                float priority = or(receiver.getPriority(), amount.getPriority());
//                float durability = aveAri(receiver.getDurability(), amount.getDurability());
//                receiver.setPriority(priority);
//                receiver.setDurability(durability);
//                break;
//
//            case WTF:
//
//                final float currentPriority = receiver.getPriority();
//                final float targetPriority = amount.getPriority();
//                /*receiver.setPriority(
//                        lerp(or(currentPriority, targetPriority),
//                                currentPriority,
//                                factor) );*/
//                float op = or(currentPriority, targetPriority);
//                if (op > currentPriority) op = lerp(op, currentPriority, factor);
//                receiver.setPriority( op );
//
//                final float currentDurability = receiver.getDurability();
//                final float targetDurability = amount.getDurability();
//                receiver.setDurability(
//                        lerp(aveAri(currentDurability, targetDurability),
//                                currentDurability,
//                                factor) );
//
//                //doesnt really change it:
//                //receiver.setQuality( receiver.getQuality() );
//
//                break;
//        }
//
//    }
//

//    /**
//     * merges another budget into this one, averaging each component
//     */
//    public void mergeAverage(@NotNull Budget that) {
//        if (this == that) return;
//
//        budget(
//                mean(pri(), that.pri()),
//                mean(dur(), that.dur()),
//                mean(qua(), that.qua())
//        );
//    }

}
