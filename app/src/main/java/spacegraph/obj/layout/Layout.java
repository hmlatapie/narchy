package spacegraph.obj.layout;

import com.google.common.collect.Lists;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL2;
import org.jetbrains.annotations.Nullable;
import spacegraph.Surface;
import spacegraph.math.v2;

import java.util.List;
import java.util.Objects;

/**
 * Created by me on 7/20/16.
 */
abstract public class Layout extends Surface {

    volatile public List<Surface> children;

    public Layout(Surface... children) {
        this(Lists.newArrayList(children));
    }

    public Layout(List<Surface> children) {
        setChildren(children);
    }


    @Override
    public List<Surface> children() {
        return children;
    }

    public void setChildren(Surface... s) {
        setChildren(Lists.newArrayList(s));
    }

    public void setChildren(List<Surface> children) {
        if (!Objects.equals(this.children, children)) {
            this.children = children;
            layout();
        }
    }

    @Override @Nullable
    public final Surface onTouch(v2 hitPoint, short[] buttons) {
        Surface x = super.onTouch(hitPoint, buttons);
        if (x!=null || children == null)
            return x;

        //2. test children reaction
        return onChildTouching(hitPoint, buttons);
    }

    protected final Surface onChildTouching(v2 hitPoint, short[] buttons) {
        v2 subHit = new v2();

        for (Surface c : children) {
            //project to child's space
            subHit.set(hitPoint);

            float csx = c.scaleLocal.x;
            float csy = c.scaleLocal.y;
            subHit.sub(c.translateLocal.x, c.translateLocal.y);
            subHit.scale(1f / csx, 1f / csy);

            float hx = subHit.x, hy = subHit.y;
            if (hx >= 0f && hx <= 1f && hy >= 0 && hy <= 1f) {
                Surface s = c.onTouch(subHit, buttons);
                if (s!=null)
                    return s; //FIFO
            }
        }
        return this;
    }

    @Override
    public boolean onKey(KeyEvent e, boolean pressed) {
        if (!super.onKey(e, pressed)) {
            if (children != null) {
                for (Surface c : children) {
                    if (c.onKey(e, pressed))
                        return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean onKey(v2 hitPoint, char charCode, boolean pressed) {
        if (!super.onKey(hitPoint, charCode, pressed)) {
            if (children != null) {
                for (Surface c : children) {
                    if (c.onKey(hitPoint, charCode, pressed))
                        return true;
                }
            }
        }
        return false;
    }
}
