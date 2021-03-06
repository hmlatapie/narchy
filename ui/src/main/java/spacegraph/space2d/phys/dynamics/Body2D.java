/*******************************************************************************
 * Copyright (c) 2013, Daniel Murphy
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 	* Redistributions of source code must retain the above copyright notice,
 * 	  this list of conditions and the following disclaimer.
 * 	* Redistributions in binary form must reproduce the above copyright notice,
 * 	  this list of conditions and the following disclaimer in the documentation
 * 	  and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package spacegraph.space2d.phys.dynamics;

import com.jogamp.opengl.GL2;
import jcog.Util;
import spacegraph.space2d.phys.collision.broadphase.BroadPhase;
import spacegraph.space2d.phys.collision.shapes.MassData;
import spacegraph.space2d.phys.collision.shapes.PolygonShape;
import spacegraph.space2d.phys.collision.shapes.Shape;
import spacegraph.space2d.phys.common.Rot;
import spacegraph.space2d.phys.common.Settings;
import spacegraph.space2d.phys.common.Sweep;
import spacegraph.space2d.phys.common.Transform;
import spacegraph.space2d.phys.dynamics.contacts.Contact;
import spacegraph.space2d.phys.dynamics.contacts.ContactEdge;
import spacegraph.space2d.phys.dynamics.joints.JointEdge;
import spacegraph.space2d.phys.fracture.Polygon;
import spacegraph.space2d.phys.fracture.PolygonFixture;
import spacegraph.util.math.Tuple2f;
import spacegraph.util.math.v2;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * A rigid body. These are created via World.createBody.
 *
 * @author Daniel Murphy
 */
public class Body2D extends Transform {
    public static final int e_islandFlag = 0x0001;
    public static final int e_awakeFlag = 0x0002;
    public static final int e_autoSleepFlag = 0x0004;
    public static final int e_bulletFlag = 0x0008;
    public static final int e_fixedRotationFlag = 0x0010;
    public static final int e_activeFlag = 0x0020;
    public static final int e_toiFlag = 0x0040;

    public BodyType type;

    public int flags;

    /**
     * island index
     */
    public int island;

    /**
     * The previous transform for particle simulation
     */
    public final Transform transformPrev = new Transform();

    /**
     * The swept motion for CCD
     */
    public final Sweep sweep = new Sweep();

    /**
     * linear velocity
     */
    public final v2 vel = new v2();

    /**
     * angular velocity
     */
    public float velAngular = 0;

    public final Tuple2f force = new v2();
    public float torque = 0;

    public final Dynamics2D W;

    public Fixture fixtures;
    public int fixtureCount;

    public JointEdge joints;
    public ContactEdge contacts;

    public float mass, m_invMass, m_massArea;
    public boolean m_fractureTransformUpdate = false;

    // Rotational inertia about the center of mass.
    public float m_I, m_invI;

    public float m_linearDamping;
    public float m_angularDamping;
    public float m_gravityScale;

    public float m_sleepTime;

    public Object data;

    public final static AtomicInteger serial = new AtomicInteger();
    final int id = serial.incrementAndGet();

    public Body2D(final BodyType t, Dynamics2D world) {
        this(new BodyDef(t), world);
    }

    public Body2D(final BodyDef bd, Dynamics2D world) {
        assert (bd.position.isValid());
        assert (bd.linearVelocity.isValid());
        assert (bd.gravityScale >= 0.0f);
        assert (bd.angularDamping >= 0.0f);
        assert (bd.linearDamping >= 0.0f);

        flags = 0;

        if (bd.bullet) {
            flags |= e_bulletFlag;
        }
        if (bd.fixedRotation) {
            flags |= e_fixedRotationFlag;
        }
        if (bd.allowSleep) {
            flags |= e_autoSleepFlag;
        }
        if (bd.awake) {
            flags |= e_awakeFlag;
        }
        if (bd.active) {
            flags |= e_activeFlag;
        }

        W = world;

        pos.set(bd.position);
        this.set(bd.angle);

        sweep.localCenter.set(0, 0);
        sweep.c0.set(pos);
        sweep.c.set(pos);
        sweep.a0 = bd.angle;
        sweep.a = bd.angle;
        sweep.alpha0 = 0.0f;

        joints = null;
        contacts = null;

        vel.set(bd.linearVelocity);
        velAngular = bd.angularVelocity;

        m_linearDamping = bd.linearDamping;
        m_angularDamping = bd.angularDamping;
        m_gravityScale = bd.gravityScale;

        force.setZero();
        torque = 0.0f;

        m_sleepTime = 0.0f;

        type = bd.type;

        if (type == BodyType.DYNAMIC) {
            mass = 1f;
            m_invMass = 1f;
        } else {
            mass = 0f;
            m_invMass = 0f;
        }

        m_I = 0.0f;
        m_invI = 0.0f;

        data = bd.userData;

        fixtures = null;
        fixtureCount = 0;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        return this==obj;
    }

    /**
     * Creates a fixture and attach it to this body. Use this function if you need to set some fixture
     * parameters, like friction. Otherwise you can create the fixture directly from a shape. If the
     * density is non-zero, this function automatically updates the mass of the body. Contacts are not
     * created until the next time step.
     *
     * @param def the fixture definition.
     * @warning This function is locked during callbacks.
     */
    public final Fixture addFixture(FixtureDef def) {

        Fixture fixture = new Fixture();
        fixture.body = this;
        fixture.create(this, def);


        W.invoke(() -> {
            if ((flags & e_activeFlag) == e_activeFlag) {
                BroadPhase broadPhase = W.contactManager.broadPhase;
                fixture.createProxies(broadPhase, this);
            }

            fixture.next = fixtures;
            fixtures = fixture;
            ++fixtureCount;


            // Let the world know we have a new fixture. This will cause new contacts
            // to be created at the beginning of the next time step.
            W.flags |= Dynamics2D.NEW_FIXTURE;


            // Adjust mass properties if needed.
            if (fixture.density > 0.0f) {
                resetMassData();
            }
        });

        return fixture;
    }

    /**
     * call this if shape changes
     */
    protected final void updateFixtures(Consumer<Fixture> tx) {
        W.invoke(() -> {
            for (Fixture f = fixtures; f != null; f = f.next) {

                //destroy and re-create proxies
                //if ((m_flags & e_activeFlag) == e_activeFlag) {
                BroadPhase broadPhase = W.contactManager.broadPhase;
                f.destroyProxies(broadPhase);

                tx.accept(f);

                f.createProxies(broadPhase, this);
                //}

                // Adjust mass properties if needed.
                if (f.density > 0.0f) {
                    resetMassData();
                }
            }
            synchronizeFixtures();
            synchronizeTransform();
        });
    }

    private final FixtureDef fixDef = new FixtureDef();

    /**
     * Creates a fixture from a shape and attach it to this body. This is a convenience function. Use
     * FixtureDef if you need to set parameters like friction, restitution, user data, or filtering.
     * If the density is non-zero, this function automatically updates the mass of the body.
     *
     * @param shape   the shape to be cloned.
     * @param density the shape density (set to zero for static bodies).
     * @warning This function is locked during callbacks.
     */
    public final Fixture addFixture(Shape shape, float density) {
        fixDef.shape = shape;
        fixDef.density = density;

        return addFixture(fixDef);
    }

    /**
     * Vytvori lubovolny simple konkavny objekt s lubovolnym poctom vrcholov.
     * funkcia urobi konvexnu dekompoziciu polygonu a aplikuje na ne jednotlive
     * konvexne fixtures, ktore budu okrem ineho zachovavat limit
     * Settings.maxPolygonVertices. Funkcia je pocas callbacku zamknuta.
     * FixtudeDef prepise 2 svoje premenne - tie sa definuju algoritmom, ostatne
     * sa prenesu na novovzniknute Fixtury.
     *
     * @param polygon
     * @param def
     */
    public final void addFixture(PolygonFixture polygon, FixtureDef def) {


        Polygon[] convex = polygon.convexDecomposition();

        def.polygon = convex.length > 1 ? polygon : null;
        //ak je polygonFixture len jeden, tak sa referencia neulozi a polygon sa niekam strati
        //parameter def je jedna referencia na vsetky pragmenty, nastavenia sa prenasaju, preto to treba nulovat!!!!! - zabralo mi cez 10 hodin odhalit tuto hlupu chybu

        for (Polygon p : convex) {
            p.flip();
            PolygonShape ps = new PolygonShape();
            ps.set(p.getArray(), p.size());
            def.shape = ps;
            polygon.fixtureList.add(addFixture(def));
        }
    }

    /**
     * Destroy a fixture. This removes the fixture from the broad-phase and destroys all contacts
     * associated with this fixture. This will automatically adjust the mass of the body if the body
     * is dynamic and the fixture has positive density. All fixtures attached to a body are implicitly
     * destroyed when the body is destroyed.
     *
     * @param fixture the fixture to be removed.
     * @warning This function is locked during callbacks.
     */
    public final void removeFixture(Fixture fixture) {

        W.invoke(() -> {
            assert (fixture.body == this);

            // Remove the fixture from this body's singly linked list.
            assert (fixtureCount > 0);

            //W.invokeLater(() -> {
            Fixture node = fixtures;
            Fixture last = null; // java change
            boolean found = false;
            while (node != null) {
                if (node == fixture) {
                    node = fixture.next;
                    found = true;
                    break;
                }
                last = node;
                node = node.next;
            }

            // You tried to remove a shape that is not attached to this body.
            assert (found);

            // java change, remove it from the list
            if (last == null) {
                fixtures = fixture.next;
            } else {
                last.next = fixture.next;
            }

            // Destroy any contacts associated with the fixture.
            ContactEdge edge = contacts;
            while (edge != null) {
                Contact c = edge.contact;
                edge = edge.next;

                Fixture fixtureA = c.aFixture;
                Fixture fixtureB = c.bFixture;

                if (fixture == fixtureA || fixture == fixtureB) {
                    // This destroys the contact and removes it from
                    // this body's contact list.
                    W.contactManager.destroy(c);
                }
            }

            if ((flags & e_activeFlag) == e_activeFlag) {
                BroadPhase broadPhase = W.contactManager.broadPhase;
                fixture.destroyProxies(broadPhase);
            }

            fixture.destroy();
            fixture.body = null;
            fixture.next = null;

            --fixtureCount;

            // Reset the mass data.
            resetMassData();
        });

    }

    public final boolean setTransform(Tuple2f position, float angle) {
        return setTransform(position, angle, Settings.EPSILON);
    }

    /**
     * Set the position of the body's origin and rotation. This breaks any contacts and wakes the
     * other bodies. Manipulating a body's transform may cause non-physical behavior. Note: contacts
     * are updated on the next call to World.step().
     *
     * @param position the world position of the body's local origin.
     * @param angle    the world rotation in radians.
     */
    public final boolean setTransform(Tuple2f position, float angle, float epsilon) {

        if (getPosition().equals(position, epsilon) && Util.equals(angle, getAngle(), epsilon))
            return false; //no change

        W.invoke(() -> {
            this.set(angle);
            pos.set(position);

            // m_sweep.c0 = m_sweep.c = Mul(m_xf, m_sweep.localCenter);
            Transform.mulToOutUnsafe(this, sweep.localCenter, sweep.c);
            sweep.a = angle;

            sweep.c0.set(sweep.c);
            sweep.a0 = sweep.a;

            BroadPhase broadPhase = W.contactManager.broadPhase;
            for (Fixture f = fixtures; f != null; f = f.next)
                f.synchronize(broadPhase, this, this);
        });

        return true;
    }

    /**
     * Get the world body origin position. Do not modify.
     *
     * @return the world position of the body's origin.
     */
    public final Tuple2f getPosition() {
        return pos;
    }

    /**
     * Get the angle in radians.
     *
     * @return the current world rotation angle in radians.
     */
    public final float getAngle() {
        return sweep.a;
    }

    /**
     * Get the world position of the center of mass. Do not modify.
     */
    public final Tuple2f getWorldCenter() {
        return sweep.c;
    }

    /**
     * Get the local position of the center of mass. Do not modify.
     */
    public final Tuple2f getLocalCenter() {
        return sweep.localCenter;
    }

    /**
     * Set the linear velocity of the center of mass.
     *
     * @param v the new linear velocity of the center of mass.
     */
    public final void setLinearVelocity(Tuple2f v) {
        if (type == BodyType.STATIC) {
            return;
        }

        if (Tuple2f.dot(v, v) > 0.0f) {
            setAwake(true);
        }

        vel.set(v);
    }

    /**
     * Get the linear velocity of the center of mass. Do not modify, instead use
     * {@link #setLinearVelocity(Tuple2f)}.
     *
     * @return the linear velocity of the center of mass.
     */
    public final v2 getLinearVelocity() {
        return vel;
    }

    /**
     * Set the angular velocity.
     *
     * @param omega the new angular velocity in radians/second.
     */
    public final void setAngularVelocity(float w) {
        if (type == BodyType.STATIC) {
            return;
        }

        if (w * w > 0f) {
            setAwake(true);
        }

        velAngular = w;
    }

    /**
     * Get the angular velocity.
     *
     * @return the angular velocity in radians/second.
     */
    public final float getAngularVelocity() {
        return velAngular;
    }

    /**
     * Get the gravity scale of the body.
     *
     * @return
     */
    public float getGravityScale() {
        return m_gravityScale;
    }

    /**
     * Set the gravity scale of the body.
     *
     * @param gravityScale
     */
    public void setGravityScale(float gravityScale) {
        this.m_gravityScale = gravityScale;
    }

    /**
     * Apply a force at a world point. If the force is not applied at the center of mass, it will
     * generate a torque and affect the angular velocity. This wakes up the body.
     *
     * @param force the world force vector, usually in Newtons (N).
     * @param point the world position of the point of application.
     */
    public final void applyForce(Tuple2f force, Tuple2f point) {
        if (type != BodyType.DYNAMIC) {
            return;
        }

        if (isAwake() == false) {
            setAwake(true);
        }

        // m_force.addLocal(force);
        // Vec2 temp = tltemp.get();
        // temp.set(point).subLocal(m_sweep.c);
        // m_torque += Vec2.cross(temp, force);

        this.force.x += force.x;
        this.force.y += force.y;

        torque += (point.x - sweep.c.x) * force.y - (point.y - sweep.c.y) * force.x;
    }

    /**
     * Apply a force to the center of mass. This wakes up the body.
     *
     * @param force the world force vector, usually in Newtons (N).
     */
    public final void applyForceToCenter(Tuple2f force) {
        if (type != BodyType.DYNAMIC) {
            return;
        }

        if (isAwake() == false) {
            setAwake(true);
        }

        this.force.x += force.x;
        this.force.y += force.y;
    }

    /**
     * Apply a torque. This affects the angular velocity without affecting the linear velocity of the
     * center of mass. This wakes up the body.
     *
     * @param torque about the z-axis (out of the screen), usually in N-m.
     */
    public final void applyTorque(float torque) {
        if (type != BodyType.DYNAMIC) {
            return;
        }

        if (isAwake() == false) {
            setAwake(true);
        }

        this.torque += torque;
    }

    /**
     * Apply an impulse at a point. This immediately modifies the velocity. It also modifies the
     * angular velocity if the point of application is not at the center of mass. This wakes up the
     * body if 'wake' is set to true. If the body is sleeping and 'wake' is false, then there is no
     * effect.
     *
     * @param impulse the world impulse vector, usually in N-seconds or kg-m/s.
     * @param point   the world position of the point of application.
     * @param wake    also wake up the body
     */
    public final void applyLinearImpulse(Tuple2f impulse, Tuple2f point, boolean wake) {
        if (type != BodyType.DYNAMIC) {
            return;
        }

        if (!isAwake()) {
            if (wake) {
                setAwake(true);
            } else {
                return;
            }
        }

        vel.x += impulse.x * m_invMass;
        vel.y += impulse.y * m_invMass;

        velAngular +=
                m_invI * ((point.x - sweep.c.x) * impulse.y - (point.y - sweep.c.y) * impulse.x);
    }

    /**
     * Apply an angular impulse.
     *
     * @param impulse the angular impulse in units of kg*m*m/s
     */
    public void applyAngularImpulse(float impulse) {
        if (type != BodyType.DYNAMIC) {
            return;
        }

        if (isAwake() == false) {
            setAwake(true);
        }
        velAngular += m_invI * impulse;
    }

    /**
     * Get the total mass of the body.
     *
     * @return the mass, usually in kilograms (kg).
     */
    public final float getMass() {
        return mass;
    }

    /**
     * Get the central rotational inertia of the body.
     *
     * @return the rotational inertia, usually in kg-m^2.
     */
    public final float getInertia() {
        return m_I
                + mass
                * (sweep.localCenter.x * sweep.localCenter.x + sweep.localCenter.y
                * sweep.localCenter.y);
    }

    /**
     * Get the mass data of the body. The rotational inertia is relative to the center of mass.
     *
     * @return a struct containing the mass, inertia and center of the body.
     */
    public final void getMassData(MassData data) {
        // data.mass = m_mass;
        // data.I = m_I + m_mass * Vec2.dot(m_sweep.localCenter, m_sweep.localCenter);
        // data.center.set(m_sweep.localCenter);

        data.mass = mass;
        data.I =
                m_I
                        + mass
                        * (sweep.localCenter.x * sweep.localCenter.x + sweep.localCenter.y
                        * sweep.localCenter.y);
        data.center.x = sweep.localCenter.x;
        data.center.y = sweep.localCenter.y;
    }

    /**
     * Set the mass properties to override the mass properties of the fixtures. Note that this changes
     * the center of mass position. Note that creating or destroying fixtures can also alter the mass.
     * This function has no effect if the body isn't dynamic.
     *
     * @param massData the mass properties.
     */
    public final void setMassData(MassData massData) {
        // TODO_ERIN adjust linear velocity and torque to account for movement of center.
//        assert (W.isLocked() == false);
//        if (W.isLocked() == true) {
//            return;
//        }

        if (type != BodyType.DYNAMIC) {
            return;
        }

        m_invMass = 0.0f;
        m_I = 0.0f;
        m_invI = 0.0f;

        mass = massData.mass;
        if (mass <= 0.0f) {
            mass = 1f;
        }

        m_invMass = 1.0f / mass;

        if (massData.I > 0.0f && (flags & e_fixedRotationFlag) == 0) {
            m_I = massData.I - mass * Tuple2f.dot(massData.center, massData.center);
            assert (m_I > 0.0f);
            m_invI = 1.0f / m_I;
        }

        final Tuple2f oldCenter = W.pool.popVec2();
        // Move center of mass.
        oldCenter.set(sweep.c);
        sweep.localCenter.set(massData.center);
        // m_sweep.c0 = m_sweep.c = Mul(m_xf, m_sweep.localCenter);
        Transform.mulToOutUnsafe(this, sweep.localCenter, sweep.c0);
        sweep.c.set(sweep.c0);

        // Update center of mass velocity.
        // m_linearVelocity += Cross(m_angularVelocity, m_sweep.c - oldCenter);
        final Tuple2f temp = W.pool.popVec2();
        temp.set(sweep.c).subbed(oldCenter);
        Tuple2f.crossToOut(velAngular, temp, temp);
        vel.added(temp);

        W.pool.pushVec2(2);
    }

    private final MassData pmd = new MassData();

    /**
     * This resets the mass properties to the sum of the mass properties of the fixtures. This
     * normally does not need to be called unless you called setMassData to override the mass and you
     * later want to reset the mass.
     */
    public final void resetMassData() {
        // Compute mass data from shapes. Each shape has its own density.
        mass = 0.0f;
        m_massArea = 0.0f;
        m_invMass = 0.0f;
        m_I = 0.0f;
        m_invI = 0.0f;
        sweep.localCenter.set(0, 0);

        final MassData massData = pmd;
        for (Fixture f = fixtures; f != null; f = f.next) {
            if (f.density != 0.0f) {
                f.getMassData(massData);
                m_massArea += massData.mass;
            }
        }

        // Static and kinematic bodies have zero mass.
        if (type == BodyType.STATIC || type == BodyType.KINEMATIC) {
            // m_sweep.c0 = m_sweep.c = m_xf.position;
            sweep.c0.set(pos);
            sweep.c.set(pos);
            sweep.a0 = sweep.a;
            return;
        }

        assert (type == BodyType.DYNAMIC);

        // Accumulate mass over all fixtures.
        final Tuple2f localCenter = W.pool.popVec2();
        localCenter.set(0, 0);
        final Tuple2f temp = W.pool.popVec2();
        for (Fixture f = fixtures; f != null; f = f.next) {
            if (f.density == 0.0f) {
                continue;
            }
            f.getMassData(massData);
            mass += massData.mass;
            // center += massData.mass * massData.center;
            temp.set(massData.center).scaled(massData.mass);
            localCenter.added(temp);
            m_I += massData.I;
        }

        // Compute center of mass.
        if (mass > 0.0f) {
            m_invMass = 1.0f / mass;
            localCenter.scaled(m_invMass);
        } else {
            // Force all dynamic bodies to have a positive mass.
            mass = 1.0f;
            m_invMass = 1.0f;
        }

        if (m_I > 0.0f && (flags & e_fixedRotationFlag) == 0) {
            // Center the inertia about the center of mass.
            m_I -= mass * Tuple2f.dot(localCenter, localCenter);
            assert (m_I > 0.0f);
            m_invI = 1.0f / m_I;
        } else {
            m_I = 0.0f;
            m_invI = 0.0f;
        }

        Tuple2f oldCenter = W.pool.popVec2();
        // Move center of mass.
        oldCenter.set(sweep.c);
        sweep.localCenter.set(localCenter);
        // m_sweep.c0 = m_sweep.c = Mul(m_xf, m_sweep.localCenter);
        Transform.mulToOutUnsafe(this, sweep.localCenter, sweep.c0);
        sweep.c.set(sweep.c0);

        // Update center of mass velocity.
        // m_linearVelocity += Cross(m_angularVelocity, m_sweep.c - oldCenter);
        temp.set(sweep.c).subbed(oldCenter);

        final Tuple2f temp2 = oldCenter;
        Tuple2f.crossToOutUnsafe(velAngular, temp, temp2);
        vel.added(temp2);

        W.pool.pushVec2(3);
    }

    /**
     * Get the world coordinates of a point given the local coordinates.
     *
     * @param localPoint a point on the body measured relative the the body's origin.
     * @return the same point expressed in world coordinates.
     */
    public final v2 getWorldPoint(Tuple2f localPoint) {
        v2 v = new v2();
        getWorldPointToOut(localPoint, v);
        return v;
    }

    public final void getWorldPointToOut(Tuple2f localPoint, Tuple2f out) {
        Transform.mulToOutUnsafe(this, localPoint, out);
    }

    public final void getWorldPointToOut(Tuple2f localPoint, float preScale, Tuple2f out) {
        Transform.mulToOutUnsafe(this, localPoint, preScale, out);
    }

    /**
     * Get the world coordinates of a vector given the local coordinates.
     *
     * @param localVector a vector fixed in the body.
     * @return the same vector expressed in world coordinates.
     */
    public final v2 getWorldVector(Tuple2f localVector) {
        v2 out = new v2();
        getWorldVectorToOut(localVector, out);
        return out;
    }

    public final void getWorldVectorToOut(Tuple2f localVector, Tuple2f out) {
        Rot.mulToOut(this, localVector, out);
    }

    public final void getWorldVectorToOutUnsafe(Tuple2f localVector, Tuple2f out) {
        Rot.mulToOutUnsafe(this, localVector, out);
    }

    /**
     * Gets a local point relative to the body's origin given a world point.
     *
     * @param a point in world coordinates.
     * @return the corresponding local point relative to the body's origin.
     */
    public final Tuple2f getLocalPoint(Tuple2f worldPoint) {
        Tuple2f out = new v2();
        getLocalPointToOut(worldPoint, out);
        return out;
    }

    public final void getLocalPointToOut(Tuple2f worldPoint, Tuple2f out) {
        Transform.mulTransToOut(this, worldPoint, out);
    }

    /**
     * Gets a local vector given a world vector.
     *
     * @param a vector in world coordinates.
     * @return the corresponding local vector.
     */
    public final Tuple2f getLocalVector(Tuple2f worldVector) {
        Tuple2f out = new v2();
        getLocalVectorToOut(worldVector, out);
        return out;
    }

    public final void getLocalVectorToOut(Tuple2f worldVector, Tuple2f out) {
        Rot.mulTrans(this, worldVector, out);
    }

    public final void getLocalVectorToOutUnsafe(Tuple2f worldVector, Tuple2f out) {
        Rot.mulTransUnsafe(this, worldVector, out);
    }

    /**
     * Get the world linear velocity of a world point attached to this body.
     *
     * @param a point in world coordinates.
     * @return the world velocity of a point.
     */
    public final Tuple2f getLinearVelocityFromWorldPoint(Tuple2f worldPoint) {
        Tuple2f out = new v2();
        getLinearVelocityFromWorldPointToOut(worldPoint, out);
        return out;
    }

    public final void getLinearVelocityFromWorldPointToOut(Tuple2f worldPoint, Tuple2f out) {
        final float tempX = worldPoint.x - sweep.c.x;
        final float tempY = worldPoint.y - sweep.c.y;
        out.x = -velAngular * tempY + vel.x;
        out.y = velAngular * tempX + vel.y;
    }

    /**
     * Get the world velocity of a local point.
     *
     * @param a point in local coordinates.
     * @return the world velocity of a point.
     */
    public final Tuple2f getLinearVelocityFromLocalPoint(Tuple2f localPoint) {
        Tuple2f out = new v2();
        getLinearVelocityFromLocalPointToOut(localPoint, out);
        return out;
    }

    public final void getLinearVelocityFromLocalPointToOut(Tuple2f localPoint, Tuple2f out) {
        getWorldPointToOut(localPoint, out);
        getLinearVelocityFromWorldPointToOut(out, out);
    }

    /**
     * Get the linear damping of the body.
     */
    public final float getLinearDamping() {
        return m_linearDamping;
    }

    /**
     * Set the linear damping of the body.
     */
    public final void setLinearDamping(float linearDamping) {
        m_linearDamping = linearDamping;
    }

    /**
     * Get the angular damping of the body.
     */
    public final float getAngularDamping() {
        return m_angularDamping;
    }

    /**
     * Set the angular damping of the body.
     */
    public final void setAngularDamping(float angularDamping) {
        m_angularDamping = angularDamping;
    }

    public BodyType getType() {
        return type;
    }

    /**
     * Set the type of this body. This may alter the mass and velocity.
     *
     * @param type
     */
    public void setType(BodyType type) {
//        assert (W.isLocked() == false);
//        if (W.isLocked() == true) {
//            return;
//        }

        if (this.type == type) {
            return;
        }

        this.type = type;

        resetMassData();

        if (this.type == BodyType.STATIC) {
            vel.setZero();
            velAngular = 0.0f;
            sweep.a0 = sweep.a;
            sweep.c0.set(sweep.c);
            synchronizeFixtures();
        }

        setAwake(true);

        force.setZero();
        torque = 0.0f;

        // Delete the attached contacts.
        ContactEdge ce = contacts;
        while (ce != null) {
            ContactEdge ce0 = ce;
            ce = ce.next;
            W.contactManager.destroy(ce0.contact);
        }
        contacts = null;

        // Touch the proxies so that new contacts will be created (when appropriate)
        BroadPhase broadPhase = W.contactManager.broadPhase;
        for (Fixture f = fixtures; f != null; f = f.next) {
            int proxyCount = f.m_proxyCount;
            for (int i = 0; i < proxyCount; ++i) {
                broadPhase.touchProxy(f.proxies[i].id);
            }
        }
    }

    /**
     * Is this body treated like a bullet for continuous collision detection?
     */
    public final boolean isBullet() {
        return (flags & e_bulletFlag) == e_bulletFlag;
    }

    /**
     * Should this body be treated like a bullet for continuous collision detection?
     */
    public final void setBullet(boolean flag) {
        if (flag) {
            flags |= e_bulletFlag;
        } else {
            flags &= ~e_bulletFlag;
        }
    }

    /**
     * You can disable sleeping on this body. If you disable sleeping, the body will be woken.
     *
     * @param flag
     */
    public void setSleepingAllowed(boolean flag) {
        if (flag) {
            flags |= e_autoSleepFlag;
        } else {
            flags &= ~e_autoSleepFlag;
            setAwake(true);
        }
    }

    /**
     * Is this body allowed to sleep
     *
     * @return
     */
    public boolean isSleepingAllowed() {
        return (flags & e_autoSleepFlag) == e_autoSleepFlag;
    }

    /**
     * Set the sleep state of the body. A sleeping body has very low CPU cost.
     *
     * @param flag set to true to put body to sleep, false to wake it.
     * @param flag
     */
    public void setAwake(boolean flag) {
        if (flag) {
            if ((flags & e_awakeFlag) == 0) {
                flags |= e_awakeFlag;
                m_sleepTime = 0.0f;
            }
        } else {
            flags &= ~e_awakeFlag;
            m_sleepTime = 0.0f;
            vel.setZero();
            velAngular = 0.0f;
            force.setZero();
            torque = 0.0f;
        }
    }

    /**
     * Get the sleeping state of this body.
     *
     * @return true if the body is awake.
     */
    public boolean isAwake() {
        return (flags & e_awakeFlag) == e_awakeFlag;
    }

    /**
     * Set the active state of the body. An inactive body is not simulated and cannot be collided with
     * or woken up. If you pass a flag of true, all fixtures will be added to the broad-phase. If you
     * pass a flag of false, all fixtures will be removed from the broad-phase and all contacts will
     * be destroyed. Fixtures and joints are otherwise unaffected. You may continue to create/destroy
     * fixtures and joints on inactive bodies. Fixtures on an inactive body are implicitly inactive
     * and will not participate in collisions, ray-casts, or queries. Joints connected to an inactive
     * body are implicitly inactive. An inactive body is still owned by a World object and remains in
     * the body list.
     *
     * @param flag
     */
    void setActive(boolean flag) {
//        assert (W.isLocked() == false);

        if (flag == isActive()) {
            return;
        }

        W.invoke(() -> {

            if (flag) {
                flags |= e_activeFlag;

                // Create all proxies.
                BroadPhase broadPhase = W.contactManager.broadPhase;
                for (Fixture f = fixtures; f != null; f = f.next) {
                    f.createProxies(broadPhase, this);
                }

                // Contacts are created the next time step.
            } else {
                flags &= ~e_activeFlag;

                // Destroy all proxies.
                BroadPhase broadPhase = W.contactManager.broadPhase;
                for (Fixture f = fixtures; f != null; f = f.next) {
                    f.destroyProxies(broadPhase);
                }

                // Destroy the attached contacts.
                ContactEdge ce = contacts;
                while (ce != null) {
                    ContactEdge ce0 = ce;
                    ce = ce.next;
                    W.contactManager.destroy(ce0.contact);
                }
                contacts = null;
            }
        });
    }

    /**
     * Get the active state of the body.
     *
     * @return
     */
    public boolean isActive() {
        return (flags & e_activeFlag) == e_activeFlag;
    }

    /**
     * Set this body to have fixed rotation. This causes the mass to be reset.
     *
     * @param flag
     */
    public void setFixedRotation(boolean flag) {
        if (flag) {
            flags |= e_fixedRotationFlag;
        } else {
            flags &= ~e_fixedRotationFlag;
        }

        resetMassData();
    }

    /**
     * Does this body have fixed rotation?
     *
     * @return
     */
    public boolean isFixedRotation() {
        return (flags & e_fixedRotationFlag) == e_fixedRotationFlag;
    }

    /**
     * Get the list of all fixtures attached to this body.
     */
    public final Fixture fixtures() {
        return fixtures;
    }

    /**
     * Get the list of all joints attached to this body.
     */
    public final JointEdge getJointList() {
        return joints;
    }

    /**
     * Get the list of all contacts attached to this body.
     *
     * @warning this list changes during the time step and you may miss some collisions if you don't
     * use ContactListener.
     */
    public final ContactEdge contacts() {
        return contacts;
    }


    /**
     * Get the user data pointer that was provided in the body definition.
     */
    public final Object data() {
        return data;
    }

    /**
     * Set the user data. Use this to store your application specific data.
     */
    public final void setData(Object data) {
        this.data = data;
    }


    // djm pooling
    private final Transform pxf = new Transform();

    protected void synchronizeFixtures() {
        final Transform xf1 = pxf;
        // xf1.position = m_sweep.c0 - Mul(xf1.R, m_sweep.localCenter);

        // xf1.q.set(m_sweep.a0);
        // Rot.mulToOutUnsafe(xf1.q, m_sweep.localCenter, xf1.p);
        // xf1.p.mulLocal(-1).addLocal(m_sweep.c0);
        // inlined:
        Rot r = xf1;
        r.s = (float) Math.sin(sweep.a0);
        r.c = (float) Math.cos(sweep.a0);
        xf1.pos.x = sweep.c0.x - r.c * sweep.localCenter.x + r.s * sweep.localCenter.y;
        xf1.pos.y = sweep.c0.y - r.s * sweep.localCenter.x - r.c * sweep.localCenter.y;
        // end inline

        for (Fixture f = fixtures; f != null; f = f.next) {
            f.synchronize(W.contactManager.broadPhase, xf1, this);
        }
    }

    public final void synchronizeTransform() {
        // m_xf.q.set(m_sweep.a);
        //
        // // m_xf.position = m_sweep.c - Mul(m_xf.R, m_sweep.localCenter);
        // Rot.mulToOutUnsafe(m_xf.q, m_sweep.localCenter, m_xf.p);
        // m_xf.p.mulLocal(-1).addLocal(m_sweep.c);
        //
        Rot q = this;
        q.s = (float) Math.sin(sweep.a);
        q.c = (float) Math.cos(sweep.a);
        Tuple2f v = sweep.localCenter;
        pos.x = sweep.c.x - q.c * v.x + q.s * v.y;
        pos.y = sweep.c.y - q.s * v.x - q.c * v.y;
    }

    /**
     * This is used to prevent connected bodies from colliding. It may lie, depending on the
     * collideConnected flag.
     *
     * @param other
     * @return
     */
    public boolean shouldCollide(Body2D other) {
        // At least one body should be dynamic.
        if (type != BodyType.DYNAMIC && other.type != BodyType.DYNAMIC) {
            return false;
        }

        // Does a joint prevent collision?
        for (JointEdge jn = joints; jn != null; jn = jn.next) {
            if (jn.other == other && !jn.joint.getCollideConnected()) {
                return false;
            }
        }

        return true;
    }

    protected final void advance(float t) {
        // Advance to the new safe time. This doesn't sync the broad-phase.
        sweep.advance(t);
        sweep.c.set(sweep.c0);
        sweep.a = sweep.a0;
        this.set(sweep.a);
        // m_xf.position = m_sweep.c - Mul(m_xf.R, m_sweep.localCenter);
        Rot.mulToOutUnsafe(this, sweep.localCenter, pos);
        pos.scaled(-1).added(sweep.c);
    }

    /** return false to immediately remove this body */
    public boolean preUpdate() {
        return true;
    }

    public void postUpdate() {

    }


    public void getWorldPointToGL(Tuple2f localPoint, float preScale, GL2 gl) {
        Transform.mulToOutUnsafe(this, localPoint, preScale, gl);
    }

    public void remove() {
        W.removeBody(this);
    }

    /** called prior to removal */
    protected void onRemoval() {

    }
}
