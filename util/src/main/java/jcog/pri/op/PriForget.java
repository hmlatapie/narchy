package jcog.pri.op;

import jcog.bag.Bag;
import jcog.pri.Pri;
import jcog.pri.Priority;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * decreases priority at a specified rate which is diminished in proportion to a budget's quality
 * so that high quality results in slower priority loss
 */
public class PriForget<P extends Priority> implements Consumer<P> {

    public static final float FORGET_TEMPERATURE_DEFAULT = 1f;

    public final float priRemoved;

    public PriForget(float priRemoved) {
        this.priRemoved = priRemoved;
    }


    /**
     * temperature parameter, in the range of 0..1.0 controls the target average priority that
     * forgetting should attempt to cause.
     * <p>
     * higher temperature means faster forgetting allowing new items to more easily penetrate into
     * the bag.
     * <p>
     * lower temperature means old items are forgotten more slowly
     * so new items have more difficulty entering.
     *
     * @return the update function to apply to a bag
     */
    @Nullable
    public static Consumer forget(int s, int c, float pressure, float mass, float temperature, float priEpsilon, FloatToObjectFunction<Consumer> f) {

        if ((s > 0) && (pressure > 0) && (c > 0) && (mass > 0) && temperature > 0) {

            float eachForget = (temperature * pressure)/(mass) * ((float)s)/c;
                    //* (mass/c) /* absolute density factor */
            ;

            if (eachForget > priEpsilon)
                return f.valueOf(eachForget);

        }
        return null;
    }

    @Nullable public static Consumer forget(Bag b, float temperature, float priEpsilon, FloatToObjectFunction f) {
        int size = b.size();
        if (size > 0) {
            return forget(size,
                    b.capacity(),
                    Math.max(Pri.EPSILON, b.depressurize()),
                    Math.max(Pri.EPSILON * size, b.mass()),
                    temperature, priEpsilon, f);
        } else {
            return null;
        }
    }

    @Override
    public void accept(P b) {

        //average constant removed, not fair to low priority items
        //b.priSub(priRemoved);

        //proportional removal, tax rate proportional to priority
        float p = b.pri();
        if (p==p) {
            b.priSet( p * (1-(p * priRemoved)) );
        }



//        b.priSub(avgToBeRemoved
//            ,0.5f //50% retained
////            //,(1f - b.priElseZero())  //retained inversely proportional to existing pri, so higher burden on higher priority
////            //,0.5f * (1f - b.priElseZero())  //retained inversely proportional to existing pri, so higher burden on higher priority
//        );
    }

}