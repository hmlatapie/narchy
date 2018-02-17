/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;

/**
 * For each sample, outputs the minimum of its two inputs.
 *
 * @author Benito Crawford
 * @version 0.9
 * @beads.category lowlevel
 */
public class Minimum extends UGen {

    /**
     * Constructor with no assigned inputs.
     *
     * @param context The audio context.
     */
    public Minimum(AudioContext context) {
        super(context, 2, 1);
    }

    /**
     * Constructor for 1 UGen input and a static minimum value.
     *
     * @param context The audio context.
     * @param ugen    The input UGen.
     * @param minVal  The minimum value.
     */

    public Minimum(AudioContext context, UGen ugen, float minVal) {
        super(context, 2, 1);
        addInput(0, ugen, 0);
        addInput(1, new Static(context, minVal), 0);
    }

    /**
     * Constructor for 2 UGen inputs.
     *
     * @param context The AudioContext.
     * @param ugen1   The first UGen input.
     * @param ugen2   The second UGen input.
     */
    public Minimum(AudioContext context, UGen ugen1, UGen ugen2) {
        super(context, 2, 1);
        addInput(0, ugen1, 0);
        addInput(1, ugen2, 0);
    }

    @Override
    public void gen() {
        float[] bi1 = bufIn[0];
        float[] bi2 = bufIn[1];
        float[] bo = bufOut[0];

        for (int i = 0; i < bufferSize; i++) {
            bo[i] = bi1[i] < bi2[i] ? bi1[i] : bi2[i];
        }

    }

}
