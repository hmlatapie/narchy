package nars.audio;

import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.concept.SensorConcept;
import nars.gui.Vis;
import nars.remote.NAgents;
import nars.time.RealTime;
import nars.util.Util;
import nars.util.data.random.XorShift128PlusRandom;
import nars.util.signal.Autoencoder;
import spacegraph.SpaceGraph;
import spacegraph.audio.AudioSource;
import spacegraph.audio.WaveCapture;
import spacegraph.obj.widget.FloatSlider;
import spacegraph.obj.widget.MatrixView;
import spacegraph.render.Draw;

import java.util.List;

import static spacegraph.obj.layout.Grid.*;

/**
 * Created by me on 11/29/16.
 */
public class NARHear extends NAgent {

    public static void main(String[] args) {
        new NARHear(NAgents.newMultiThreadNAR(2, new RealTime.CS(true).dur(0.2f))).runRT(20);
    }

    public NARHear(NAR nar) {
        super(nar);
        AudioSource audio = new AudioSource(7, 20);
        WaveCapture au = new WaveCapture(
                audio,
                //new SineSource(128),
                20);

        List<SensorConcept> freqInputs = senseNumber(0, au.freqSamplesPerFrame,
                i -> $.func("f", $.the(i)).toString(),

        //        i -> () -> (Util.clamp(au.history[i], -1f, 1f)+1f)/2f); //raw bipolar
                i -> () -> (Util.clamp(Math.abs(au.dataNorm[i]), 0f, 1f))); //absolute value unipolar

        freqInputs.forEach(s -> s.resolution(0.1f));

        Autoencoder ae = new Autoencoder(au.freqSamplesPerFrame, 32, new XorShift128PlusRandom(1));
        nar.onFrame(f->{
            ae.train(au.dataNorm, 0.1f, 0.01f, 0.01f, false);
        });

        SpaceGraph.window(
                grid(
                    row(
                            au.newMonitorPane(),
                            new FloatSlider(audio.gain)
                    ),
                    new MatrixView(ae.W.length, ae.W[0].length, MatrixView.arrayRenderer(ae.W)),
                    new MatrixView(ae.y, (v, gl) -> { Draw.colorPolarized(gl, -v); return 0; }),
                    Vis.conceptLinePlot(nar, freqInputs, 64)
                ),
                1200, 1200);

        //Vis.conceptsWindow2D(nar, 64, 4).show(800, 800);

//            b.setScene(new Scene(au.newMonitorPane(), 500, 400));
//            b.show();
//        });
    }

    @Override
    protected float act() {
        return 0;
    }


}
