package spacegraph.widget.meta;

import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.WindowUpdateEvent;
import com.jogamp.newt.opengl.GLWindow;
import spacegraph.SpaceGraph;
import spacegraph.input.Finger;
import spacegraph.math.v2;
import spacegraph.widget.button.CheckBox;
import spacegraph.widget.button.ToggleButton;

import java.util.function.Supplier;

/** toggle button, which when actived, creates a window, and when inactivated destroys it
 *  TODO window width, height parameters
 * */
public class WindowButton extends CheckBox implements ToggleButton.ToggleAction, WindowListener {

    private final Supplier spacer;

    int width = 400, height = 240;

    SpaceGraph space;

    public WindowButton(String text, Object o) {
        this(text, ()->o);
    }

    public WindowButton(String text, Supplier spacer) {
        super(text);
        this.spacer = spacer;
        on(this);
    }
    public WindowButton(String text, Supplier spacer, int w, int h) {
        this(text, spacer);
        this.width = w; this.height = h;
    }

    @Override
    protected boolean onTouching(v2 hitPoint, short[] buttons) {
        return super.onTouching(hitPoint, buttons);
    }

    @Override
    public void onChange(ToggleButton t, boolean enabled) {
        synchronized (spacer) {
            if (enabled) {
                if (space == null) {
                    space = SpaceGraph.window(spacer.get(), 0, 0);
                    int sx = Finger.pointer.getX();
                    int sy = Finger.pointer.getY();
                    int nx = sx - width/2;
                    int ny = sy - height/2;
                    space.show(this.toString(), width,height, nx, ny);
                    space.addWindowListener(this);
                }
            } else {
                if (this.space!=null) {
                    GLWindow win = this.space.window;
                    this.space = null;
                    if (win != null && win.getWindowHandle() != 0)
                        win.destroy();
                }
            }
        }
    }

    @Override
    public void windowResized(WindowEvent e) {

    }

    @Override
    public void windowMoved(WindowEvent e) {

    }

    @Override
    public void windowDestroyNotify(WindowEvent e) {

    }

    @Override
    public void windowDestroyed(WindowEvent e) {
        this.space = null;
        set(false);
    }

    @Override
    public void windowGainedFocus(WindowEvent e) {

    }

    @Override
    public void windowLostFocus(WindowEvent e) {

    }

    @Override
    public void windowRepaint(WindowUpdateEvent e) {

    }
}