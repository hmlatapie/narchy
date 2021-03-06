package spacegraph.input.finger;

import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.space2d.Surface;
import spacegraph.util.math.v2;

public class FingerMove extends FingerDragging {
    private final Surface moving;
    private RectFloat2D before;
    boolean x, y;

    public FingerMove(Surface moving) {
        this(moving, true, true);
    }

    public FingerMove(Surface moving, boolean xAxis, boolean yAxis) {
        super(0 /* LEFT BUTTON */);
        this.x = xAxis;
        this.y = yAxis;
        this.moving = moving;
    }


    @Override public boolean drag(Finger finger) {
        if (before == null)
            this.before = moving.bounds;

        float pmx = before.x;
        float pmy = before.y;
        v2 fh = finger.pos;
        //if (fh!=null) {
            v2 fhd = finger.hitOnDown[0];
            if (fhd!=null) {
                float tx = pmx + (x ? (fh.x - fhd.x) : 0);
                float ty = pmy + (y ? (fh.y - fhd.y) : 0);
                moved(tx, ty, moving.w() + tx, moving.h() + ty);
                return true;
            }
        //}
        return false;
    }

    protected void moved(float x1, float y1, float x2, float y2) {
        moving.pos(x1, y1, x2, y2);
    }


}
