/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGenChain;
import net.beadsproject.beads.data.DataAuvent;
import net.beadsproject.beads.data.DataBeadReceiver;

/**
 * A basic reverb unit with adjustable room size, high-frequency damping, and
 * early reflections and late reverb levels. If specified, creates a
 * de-correlated multi-channel effect.
 *
 * @author Benito Crawford
 * @version 0.9.5
 * @beads.category effect
 */
public class Reverb extends UGenChain implements DataBeadReceiver {
    private float size, damping, earlyLevel, lateLevel;
    private final Gain earlyGain;
    private final Gain lateGain;
    private final AllpassFilter eAPF1;
    private final AllpassFilter eAPF2;
    private final AllpassFilter eAPF3;
    private final AllpassFilter lAPF1;
    private final AllpassFilter lAPF2;
    private final AllpassFilter lAPF3;
    private final AllpassFilter lAPF4;
    private final AllpassFilter[] apfOuts;
    private final float[] outDelayScale;
    private final OnePoleFilter lpf;
    private final OnePoleFilter src;
    private final RandomPWM delayModulator;
    private float lateDelay1, lateDelay2, lateDelay3, lateDelay4;
    private final float sampsPerMS;
    private final TapIn earlyTapIn;
    private final TapOut earlyTapOut;

    /**
     * Constructor for a reverb unit with one output channel.
     *
     * @param context The audio context.
     */
    public Reverb(AudioContext context) {
        this(context, 1);
    }

    /**
     * Constructor for a reverb unit with the specified number of output
     * channels.
     *
     * @param context     The audio context.
     * @param outChannels The number of output channels.
     */
    public Reverb(AudioContext context, int outChannels) {
        super(context, 1, outChannels);

        sampsPerMS = (float) context.msToSamples(1);

        // start with a minor low-pass filter.
        src = new OnePoleFilter(context, 4000);

        // Early reflections unit: start with a delay, then 3 allpass filters in
        // series. Takes input from the source filter src.
        earlyTapIn = new TapIn(context, 125);
        earlyTapOut = new TapOut(context, earlyTapIn, 10);
        eAPF1 = new AllpassFilter(context, (int) (12.812 * sampsPerMS), 113,
                .3f);
        eAPF2 = new AllpassFilter(context, (int) (12.812 * sampsPerMS * 3),
                337, .4f);
        eAPF3 = new AllpassFilter(context, (int) (12.812 * sampsPerMS * 9.4),
                1051, .5f);
        Gain earlyGainEcho = new Gain(context, 1, -.3f);
        // The early reflections output gets fed back in at the top...

        // Late reverb unit: 4 allpass filters in series.
        // Takes input from the source filter src, the early reflections output
        // earlyGainEcho, and an echo of itself
        lAPF1 = new AllpassFilter(context, (int) (140f * sampsPerMS), 19, .72f);
        lAPF2 = new AllpassFilter(context, (int) (140f * sampsPerMS), 23, .7f);
        lAPF3 = new AllpassFilter(context, (int) (140f * sampsPerMS), 29, .65f);
        lAPF4 = new AllpassFilter(context, (int) (140f * sampsPerMS), 37, .6f);
        lpf = new OnePoleFilter(context, 1000);
        TapIn lateTapIn = new TapIn(context, 1000);
        TapOut lateTapOut1 = new TapOut(context, lateTapIn, 10);
        TapOut lateTapOut2 = new TapOut(context, lateTapIn, 31.17f);
        Gain lateGainEcho = new Gain(context, 1, -.25f);
        // double tap - this gets put back in at the top of the filter series.

        // Collect the early reflections and the late reverb here
        earlyGain = new Gain(context, 1, 1);
        lateGain = new Gain(context, 1, 1);
        Gain collectedGain = new Gain(context, 1, 1);

        // used to modulate the delay times, to help reduce ringing.
        delayModulator = new RandomPWM(context, RandomPWM.RAMPED_NOISE, 4000,
                15000, 1);

        drawFromChainInput(src);
        earlyTapIn.in(src);
        earlyTapIn.in(earlyGain);
        eAPF1.in(earlyTapOut);
        eAPF2.in(eAPF1);
        eAPF3.in(eAPF2);
        earlyGainEcho.in(eAPF3);
        earlyGain.in(earlyGainEcho);
        lAPF1.in(earlyGainEcho);
        lAPF1.in(lateGainEcho);
        lAPF1.in(src);
        lAPF2.in(lAPF1);
        lAPF3.in(lAPF2);
        lAPF4.in(lAPF3);
        lpf.in(lAPF4);
        lateTapIn.in(lpf);
        lateGainEcho.in(lateTapOut1);
        lateGainEcho.in(lateTapOut2);
        lateGain.in(lateGainEcho);
        collectedGain.in(earlyGain);
        collectedGain.in(lateGain);

        apfOuts = new AllpassFilter[outChannels];
        outDelayScale = new float[outChannels];
        for (int i = 0; i < outChannels; i++) {
            float g = .3f + ((float) i / (i + 1)) * .1f + (float) Math.sin(i)
                    * .05f;
            outDelayScale[i] = (3f * i + 5) / (5f * i + 5);
            apfOuts[i] = new AllpassFilter(context, (int) (60f * sampsPerMS),
                    20, g);
            apfOuts[i].in(collectedGain);
            addToChainOutput(i, apfOuts[i]);
        }

        setSize(.5f).setDamping(.7f).setEarlyReflectionsLevel(1)
                .setLateReverbLevel(1);
    }

    @Override
    protected void preFrame() {
        delayModulator.update();
        int m = (int) (delayModulator.getValue() * .3f * sampsPerMS);
        lAPF1.setDelay((int) lateDelay1 - m);
        lAPF2.setDelay((int) lateDelay2 + m);
        lAPF3.setDelay((int) lateDelay3 - m);
        lAPF4.setDelay((int) lateDelay4 + m);
    }

    /**
     * Gets the "room size".
     *
     * @return The "room size", between 0 and 1.
     */
    public float getSize() {
        return size;
    }

    /**
     * Sets the "room size". Valid value range from 0 to 1 (.5 is the default).
     * The larger the value, the longer the decay time.
     *
     * @param size The "room size".
     * @return This reverb instance.
     */
    public Reverb setSize(float size) {
        if (size > 1)
            size = 1;
        else if (size < 0.01)
            size = .01f;
        this.size = size;
        lateDelay1 = 86.0f * size * sampsPerMS;
        lateDelay2 = lateDelay1 * 1.16f;
        lateDelay3 = lateDelay2 * 1.16f;
        lateDelay4 = lateDelay3 * 1.16f;
        earlyTapOut.setDelay(60f * size);

        float d = 12.812f * sampsPerMS * size;
        eAPF1.setDelay((int) d);
        eAPF2.setDelay((int) (d * 3 - 2));
        eAPF3.setDelay((int) (d * 9.3 + 1));

        d = 60f * sampsPerMS * size;
        for (int i = 0; i < this.outs; i++) {
            apfOuts[i].setDelay((int) (d * outDelayScale[i]));
        }
        return this;
    }

    /**
     * Gets the damping factor.
     *
     * @return The damping factor, between 0 and 1.
     */
    public float getDamping() {
        return damping;
    }

    /**
     * Sets the damping factor. Valid values range from 0 to 1 (.7 is the
     * default). Higher values filter out higher frequencies faster.
     *
     * @param damping The damping factor.
     * @return This reverb instance.
     */
    public Reverb setDamping(float damping) {
        if (damping < 0)
            damping = 0;
        else if (damping > 1)
            damping = 1;
        this.damping = damping;

        float f = 1f - (float) Math.sqrt(damping);

        src.setFrequency(f * 10000 + 250);
        lpf.setFrequency(f * 8000 + 200);

        return this;
    }

    /**
     * Gets the early reflections level.
     *
     * @return The early reflections level.
     */
    public float getEarlyReflectionsLevel() {
        return earlyLevel;
    }

    /**
     * Sets the early reflections level (the amount of early reflections heard
     * in the output). The default value is 1.
     *
     * @param earlyLevel The early reflections level.
     * @return This reverb instance.
     */
    public Reverb setEarlyReflectionsLevel(float earlyLevel) {
        this.earlyLevel = earlyLevel;
        earlyGain.setGain(earlyLevel);
        return this;
    }

    /**
     * Gets the late reverb level.
     *
     * @return The late reverb level.
     */
    public float getLateReverbLevel() {
        return lateLevel;
    }

    /**
     * Sets the late reverb level (the amount of late reverb heard in the
     * output). The default value is 1.
     *
     * @param lateLevel The late reverb level.
     * @return This reverb instance.
     */
    public Reverb setLateReverbLevel(float lateLevel) {
        this.lateLevel = lateLevel;
        lateGain.setGain(lateLevel);
        return this;
    }

    /**
     * Sets the reverb parameters with a DataBead, using values stored in the
     * keys "damping", "roomSize", "earlyReflectionsLevel", and
     * "lateReverbLevel".
     *
     * @param db The parameter DataBead.
     */
    @Override
    public DataBeadReceiver sendData(DataAuvent db) {
        if (db != null) {
            setDamping(db.getFloat("damping", damping));
            setSize(db.getFloat("roomSize", size));
            setEarlyReflectionsLevel(db.getFloat("earlyReflectionsLevel",
                    earlyLevel));
            setLateReverbLevel(db.getFloat("lateReverbLevel", lateLevel));

        }
        return this;
    }

    /**
     * Gets a new DataBead filled with parameter values stored in the keys
     * "damping", "roomSize", "earlyReflectionsLevel", and "lateReverbLevel".
     *
     * @return The parameter DataBead.
     */
    public DataAuvent getParams() {
        DataAuvent db = new DataAuvent();
        db.put("damping", damping);
        db.put("roomSize", size);
        db.put("earlyReflectionsLevel", earlyLevel);
        db.put("lateReverbLevel", lateLevel);
        return db;
    }

}