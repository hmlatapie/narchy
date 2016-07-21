package nars.agent;

import nars.util.Util;

import java.util.Random;

/**
 * Created by me on 6/9/16.
 */
public class DecideActionSoftmax implements DecideAction {

    private final float minTemperature;
    private final float temperatureDecay;
    /**
     * normalized motivation
     */
    private float[] motNorm, motProb;


    /** whether to exclude negative values */
    boolean onlyPositive = false;

    float temperature;
    private float decisiveness;

    public DecideActionSoftmax(float initialTemperature, float minTemperature, float decay) {
        this.temperature = initialTemperature;
        this.minTemperature = minTemperature;
        this.temperatureDecay = decay;
    }

    @Override
    public int decideAction(float[] motivation, int lastAction, Random random) {

        temperature = Math.max(minTemperature,temperature * temperatureDecay);

        int actions = motivation.length;
        if (motNorm == null) {
            motNorm = new float[actions];
            motProb = new float[actions];
        }

        if (onlyPositive) {
            for (int i = 0; i < motivation.length; i++)
                motivation[i] = Math.max(0, motivation[i]);
        }

        float[] minmax = Util.minmax(motivation);
        float min = minmax[0];
        float max = minmax[1];
        float sumNorm = 0;
        for (int i = 0; i < actions; i++) {
            float u = Util.normalize(motivation[i], min, max);
            sumNorm += (motNorm[i] = u);
        }

        /* http://www.cse.unsw.edu.au/~cs9417ml/RL1/source/RLearner.java */
        float sumProb = 0;
        for (int i = 0; i < actions; i++) {
            float m;
            motProb[i] = m = (float) Math.exp(motNorm[i] / temperature);
            sumProb += m;
        }


        float r = random.nextFloat() * sumProb;

        int i;
        for (i = actions - 1; i >= 1; i--) {
            float m = motProb[i];
            r -= m;
            if (r <= 0) {
                break;
            }
        }

        decisiveness = motNorm[i] / sumNorm;
        //System.out.println("decisiveness: " + decisiveness );

        return i;
    }
}
