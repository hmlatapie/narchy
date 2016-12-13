package spacegraph;

import com.jogamp.opengl.GL2;
import org.jetbrains.annotations.Nullable;
import spacegraph.math.v3;
import spacegraph.phys.Collidable;
import spacegraph.phys.Dynamic;
import spacegraph.phys.Dynamics;
import spacegraph.phys.collision.ClosestRay;
import spacegraph.phys.constraint.TypedConstraint;
import spacegraph.render.JoglPhysics;

import java.util.List;
import java.util.function.Consumer;

/**
 * volumetric subspace.
 * an atom (base unit) of spacegraph physics-simulated virtual matter
 */
public abstract class Spatial<X> implements Active {

    public final X key;
    public final int hash;


    /**
     * the draw order if being drawn
     * order = -1: inactive
     * order > =0: live
     */
    transient public short order;



    public Spatial() {
        this(null);
    }

    public Spatial(X k) {
        this.key = k!=null ? k : (X) this;
        this.hash = k!=null ? k.hashCode() : super.hashCode();
    }

    @Override
    public String toString() {

        return key + "<" +
                //(body!=null ? body.shape() : "shapeless")  +
                ">";
    }




    @Override
    public final boolean equals(Object obj) {

        return this == obj || key.equals(((Spatial) obj).key);
    }

    @Override
    public final int hashCode() {
        return hash;
    }


    public boolean preactive;



    public void update(Dynamics world) {
        //create and update any bodies and constraints
    }

//    public final boolean isShown(short nextOrder, Dynamics world) {
//        if (active()) {
//            return true;
//        } else {
//            return false;
//        }
//    }

    public boolean active() {
        return preactive && order > -1;
    }

    public final boolean preactive() {
        return preactive;
    }

    public void reactivate(boolean b) {

        this.preactive = b;

    }


    public boolean collidable() {
        return true;
    }

    /** returns true if the event has been absorbed, false if it should continue propagating */
    public Surface onTouch(Collidable body, ClosestRay hitPoint, short[] buttons, JoglPhysics space) {
        return null;
    }

    /** returns true if the event has been absorbed, false if it should continue propagating */
    public boolean onKey(Collidable body, v3 hitPoint, char charCode, boolean pressed) {
        return false;
    }

    /** schedules this node for removal from the engine, where it will call stop(s) to complete the removal */
    @Override public boolean hide() {
        if (order > -1 || preactive) {
            order = -1;
            preactive = false;
            return true;
        }
        return false;
    }

    //abstract public Iterable<Collidable> bodies();
    abstract public void forEachBody(Consumer<Collidable> c);

    @Nullable abstract public List<TypedConstraint> constraints();

    public abstract void renderAbsolute(GL2 gl);

    public abstract void renderRelative(GL2 gl, Collidable body);

    public void delete() {

    }

    public void moveWithin(v3 boundsMin, v3 boundsMax) {
        forEachBody(b -> {
            v3 t = b.worldTransform;
            if (t.min(boundsMin) || t.max(boundsMax)) {
                ((Dynamic)b).linearVelocity.zero();
            }
        });
    }


    public float radius() {
        return 0;
    }

}