package spacegraph.space2d.widget.meta;

import jcog.exe.Loop;
import jcog.math.MutableInteger;
import spacegraph.space2d.container.Gridding;
import spacegraph.space2d.widget.button.IconToggleButton;
import spacegraph.space2d.widget.meter.Plot2D;
import spacegraph.space2d.widget.slider.IntSpinner;
import spacegraph.space2d.widget.tab.ButtonSet;
import spacegraph.space2d.widget.windo.Widget;

/**
 * control and view statistics of a loop
 */
public class LoopPanel extends Widget {

    private final IntSpinner fpsLabel;
    private final Loop loop;
    private final Plot2D cycleTimePlot;
    MutableInteger fps;

    private volatile boolean pause = false;

    public LoopPanel(Loop loop) {
        this.loop = loop;
        fps = new MutableInteger(Math.round(loop.getFPS()));
        fpsLabel = new IntSpinner(fps, (f)-> f + "fps", 0, 100);
        cycleTimePlot = new Plot2D(128, Plot2D.Line)
                .add("cycleTime", ()->loop.cycleTime.getMean())
                .add("dutyTime", ()->loop.dutyTime.getMean());

        content(
            new Gridding(
                new ButtonSet(ButtonSet.Mode.One,
                    IconToggleButton.awesome("play").on((b) -> {
                        if (b) {
                            if (pause) {
                                pause = false;
                                update();
                            }

                        }
                    }), IconToggleButton.awesome("pause").on((b) -> {
                        if (b) {

                            if (!pause) {
                                pause = true;
                                update(); //update because this view wont be updated while paused
                            }
                        }
                    })
                ),
                fpsLabel, //TODO number spin control
                cycleTimePlot
        ));
        update();
    }

    public synchronized void update() {
        if (!pause) {
            int f = fps.intValue();
            int g = Math.round(loop.getFPS());
            if (f != g) {
                loop.runFPS(f);
                fpsLabel.set(f);
            }
            cycleTimePlot.update();
        } else {
            if (loop.isRunning())
                loop.stop();
            //TODO fpsLabel.disable(); // but don't: set(0)
        }

    }
}
