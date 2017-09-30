package spacegraph;

import com.jogamp.nativewindow.util.Point;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL2;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.Finger;
import spacegraph.math.v2;

import static spacegraph.Surface.Align.None;
import static spacegraph.math.v3.v;

/**
 * orthographic widget adapter. something which goes on the "face" of a HUD ("head"s-up-display)
 */
public class Ortho extends Surface implements SurfaceRoot, WindowListener, KeyListener, MouseListener {

//    protected final AnimVector2f pos;
    boolean visible;

    final Finger finger;


//    public static void main(String[] args) {
//        SpaceGraph s = new SpaceGraph();
//        s.add(new Facial(new ConsoleSurface(80, 25)).scale(0.5f));
//        s.show(800, 600);
//    }

    public final Surface surface;
    private boolean maximize;
    public SpaceGraph window;

    public Ortho(Surface content) {

        this.surface = content;
        surface.align = None;

        this.finger = newFinger();
//        this.scale = new AnimVector2f(3f);
//        this.pos = new AnimVector2f(3f);
    }

    @Override
    public SurfaceRoot root() {
        return this;
    }

    protected Finger newFinger() {
        return new Finger(this);
    }

    @Override
    public Ortho translate(float x, float y) {
        pos.set(x, y);
        return this;
    }

    @Override
    public Ortho move(float x, float y) {
        pos.add(x, y, 0);
        return this;
    }

    @Override
    public Ortho scale(float s) {
        return scale(s, s);
    }

    @Override
    public v2 scale() {
        return scale;
    }

    @Override
    public Ortho scale(float sx, float sy) {
        scale.set(sx, sy);
        return this;
    }

    public void start(SpaceGraph s) {
        this.window = s;
        s.addWindowListener(this);
        s.addMouseListener(this);

        s.addKeyListener(this);
//        s.dyn.addAnimation(scale);
//        s.dyn.addAnimation(pos);
        surface.start(this);
        surface.layout();
        resized();
    }



    @Override
    protected void paint(GL2 gl) {
        surface.render(gl);
    }

    /**
     * expand to window
     */
    public Ortho maximize() {
        maximize = true;
        resized();
        return this;
    }

    @Override
    public void windowResized(WindowEvent e) {
        resized();
    }

    private void resized() {
        //TODO resize preserving aspect, translation, etc
        if (maximize && window != null) {
            scale(window.getWidth(), window.getHeight());
            translate(0, 0);
        }
    }

    @Override
    public void windowMoved(WindowEvent e) {

    }

    @Override
    public void windowDestroyNotify(WindowEvent e) {

    }

    @Override
    public void windowDestroyed(WindowEvent e) {
        visible = false;
        stop();
    }

    public void stop() {
        surface.stop();
    }

    @Override
    public void windowGainedFocus(WindowEvent e) {

    }

    @Override
    public void windowLostFocus(WindowEvent e) {
        updateMouse(null);
    }

    @Override
    public void windowRepaint(WindowUpdateEvent e) {
        visible = true;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        surface.onKey(e, true);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        surface.onKey(e, false);
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        updateMouse(e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        updateMouse(null);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        updateMouse(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        short[] bd = e.getButtonsDown();
        int ii = ArrayUtils.indexOf(bd, e.getButton());
        bd[ii] = -1;
        updateMouse(e, bd);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        updateMouse(e);
    }

    private void updateMouse(MouseEvent e) {
        updateMouse(e, e != null ? e.getButtonsDown() : null);
    }

    private boolean updateMouse(MouseEvent e, short[] buttonsDown) {
        float x, y;

        if (e != null && !e.isConsumed()) {

            //screen coordinates
            float sx = e.getX();
            float sy = window.getHeight() - e.getY();


            x = (sx - pos.x) / (scale.x);
            y = (sy - pos.y) / (scale.y);
            if (x >= 0 && y >= 0 && x <= 1f && y <= 1f) {
                updateMouse(e, x, y, buttonsDown);
                return true;
            }

        }

        x = y = Float.NaN;
        updateMouse(null, x, y, null);

        return false;
    }

    public Surface updateMouse(@Nullable MouseEvent e, float x, float y, short[] buttonsDown) {

        if (e != null) {
            SpaceGraph rw;
            if (window != null) {
                rw = window;
                if (rw != null) {
                    GLWindow rww = rw.window;
                    if (rww != null) {
                        Point p = rww.getLocationOnScreen(new Point());
                        Finger.pointer.set(p.getX() + e.getX(), p.getY() + e.getY());
                    }
                }
            }
        }

        /*if (e == null) {
            off();
        } else {*/
        Surface s;
        if ((s = finger.on(

                v(x, y), buttonsDown)) != null) {
            if (e != null)
                e.setConsumed(true);
            return s;
        }

        //}

        return null;
    }

    @Override
    public void mouseDragged(MouseEvent e) {

        updateMouse(e);
    }


    @Override
    public void mouseWheelMoved(MouseEvent e) {

    }

}
