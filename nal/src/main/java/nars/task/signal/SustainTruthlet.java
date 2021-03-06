package nars.task.signal;

import jcog.Skill;
import nars.NAR;
import nars.Param;

import static nars.time.Tense.ETERNAL;

/** fades evidence before the beginning and after the end of a defined RangeTruthlet
 *
 *   -  - - --- -----*
 *                    \
 *                    *---- - - -  -   -
 *
 *                  |  |
 * */
@Skill({"Sustain","Audio_feedback"})
public class SustainTruthlet extends ProxyTruthlet<RangeTruthlet> {

    int dur;

    public SustainTruthlet(RangeTruthlet r, NAR nar) {
        this(r, nar.dur());
    }

    public SustainTruthlet(RangeTruthlet r, int dur) {
        super(r);
        this.dur = dur;
    }

    @Override
    public void truth(long when, float[] freqEvi) {

        if (when == ETERNAL)
            when = mid();

        long dist;
        long start, end;

        long w;
        //nearest endpoint
        if (when < (start=start())) {
            dist = Math.abs((w = start) - when);
        } else if (when > (end=end())) {
            dist = Math.abs(when - (w = end));
        } else {
            dist = 0; //contained; use full internal value
            w = when;
        }

        super.truth(w, freqEvi);
        if (dist > 0) {
            float f = freqEvi[0];
            if (f == f)
                freqEvi[1] = (float) Param.evi(freqEvi[1], dist, /* dur */ dur()); //dist is relative to the event's range
        }

    }

    public long dur() {
        //return 1 + range() / 2;
        return dur;
    }
}
