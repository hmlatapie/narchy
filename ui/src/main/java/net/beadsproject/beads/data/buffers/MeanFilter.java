/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.data.buffers;

import jcog.math.tensor.ArrayTensor;
import net.beadsproject.beads.data.WaveFactory;

import java.util.Arrays;

/**
 * Creates a {@link Buffer} of the constant 1/bufferSize over [0,1]. The convolution of the MeanFilter with data gives the mean.
 *
 * @author ben
 */
public class MeanFilter extends WaveFactory {

    @Override
    public ArrayTensor get(int bufferSize) {
        int size = bufferSize;
        ArrayTensor b = new ArrayTensor(size);
        Arrays.fill(b.data, 1.f / bufferSize);
        return b;
    }

    @Override
    public String getName() {
        return "MeanFilter";
    }

}
