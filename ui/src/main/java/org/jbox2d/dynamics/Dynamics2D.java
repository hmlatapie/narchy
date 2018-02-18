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
package org.jbox2d.dynamics;

import jcog.math.FloatSupplier;
import org.jbox2d.callbacks.*;
import org.jbox2d.collision.AABB;
import org.jbox2d.collision.RayCastInput;
import org.jbox2d.collision.RayCastOutput;
import org.jbox2d.collision.TimeOfImpact.TOIInput;
import org.jbox2d.collision.TimeOfImpact.TOIOutput;
import org.jbox2d.collision.TimeOfImpact.TOIOutputState;
import org.jbox2d.collision.broadphase.BroadPhase;
import org.jbox2d.collision.broadphase.BroadPhaseStrategy;
import org.jbox2d.collision.broadphase.DefaultBroadPhaseBuffer;
import org.jbox2d.collision.broadphase.DynamicTree;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.*;
import org.jbox2d.dynamics.contacts.Contact;
import org.jbox2d.dynamics.contacts.ContactEdge;
import org.jbox2d.dynamics.joints.Joint;
import org.jbox2d.dynamics.joints.JointDef;
import org.jbox2d.dynamics.joints.JointEdge;
import org.jbox2d.fracture.Fracture;
import org.jbox2d.fracture.FractureListener;
import org.jbox2d.fracture.fragmentation.Smasher;
import org.jbox2d.fracture.util.HashTabulka;
import org.jbox2d.particle.*;
import org.jbox2d.pooling.IWorldPool;
import org.jbox2d.pooling.Vec2Array;
import org.jbox2d.pooling.normal.DefaultWorldPool;
import spacegraph.math.Tuple2f;
import spacegraph.math.v2;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The world class manages all physics entities, dynamic simulation, and asynchronous queries. The
 * world also contains efficient memory management facilities.
 *
 * @author Daniel Murphy
 */
public class Dynamics2D {
    public static final int WORLD_POOL_SIZE = 256;
    public static final int WORLD_POOL_CONTAINER_SIZE = 16;

    public static final int NEW_FIXTURE = 0x0001;
//    public static final int LOCKED = 0x0002;
    public static final int CLEAR_FORCES = 0x0004;

    // statistics gathering
    public int activeContacts = 0;
    public int contactPoolCount = 0;

    protected int flags;

    protected final ContactManager m_contactManager;

    private Body2D m_bodyList;
    private Joint m_jointList;

    private int m_bodyCount;
    private int m_jointCount;

    private final Tuple2f m_gravity = new Vec2();
    private boolean m_allowSleep;

    // private Body m_groundBody;

    private DestructionListener m_destructionListener;
    private ParticleDestructionListener m_particleDestructionListener;
    //private DebugDraw m_debugDraw;

    public final IWorldPool pool;

    /**
     * This is used to compute the time step ratio to support a variable time step.
     */
    private float m_inv_dt0;

    // these are for debugging the solver
    private boolean m_warmStarting;
    private boolean m_continuousPhysics;
    private boolean m_subStepping;

    private boolean m_stepComplete;

    private final Profile m_profile;

    private final ParticleSystem m_particleSystem;


    private final Smasher smasher = new Smasher();
    @Deprecated private final HashTabulka<Fracture> fractures = new HashTabulka<>(); //TODO move into Smasher


    final Queue<Runnable> queue = new ConcurrentLinkedQueue<>();

    final AtomicBoolean LOCKED = new AtomicBoolean(false);

    private final Island toiIsland = new Island(this);
    private final TOIInput toiInput = new TOIInput();
    private final TOIOutput toiOutput = new TOIOutput();
    private final TimeStep subStep = new TimeStep();
    private final Body2D[] tempBodies = new Body2D[2];
    private final Sweep backup1 = new Sweep();
    private final Sweep backup2 = new Sweep();


    /**
     * Construct a world object.
     *
     * @param gravity the world gravity vector.
     */
    public Dynamics2D(Tuple2f gravity) {
        this(gravity, new DefaultWorldPool(WORLD_POOL_SIZE, WORLD_POOL_CONTAINER_SIZE));
    }

    /**
     * Construct a world object.
     *
     * @param gravity the world gravity vector.
     */
    public Dynamics2D(Tuple2f gravity, IWorldPool pool) {
        this(gravity, pool, new DynamicTree());
    }

    public Dynamics2D(Tuple2f gravity, IWorldPool pool, BroadPhaseStrategy strategy) {
        this(gravity, pool, new DefaultBroadPhaseBuffer(strategy));
    }

    public Dynamics2D(Tuple2f gravity, IWorldPool pool, BroadPhase broadPhase) {
        this.pool = pool;
        m_destructionListener = null;

        m_bodyList = null;
        m_jointList = null;

        m_bodyCount = 0;
        m_jointCount = 0;

        m_warmStarting = true;
        m_continuousPhysics = true;
        m_subStepping = false;
        m_stepComplete = true;

        m_allowSleep = true;
        m_gravity.set(gravity);

        flags = CLEAR_FORCES;

        m_inv_dt0 = 0f;

        m_contactManager = new ContactManager(this, broadPhase);
        m_profile = new Profile();

        m_particleSystem = new ParticleSystem(this);

    }

    public void setAllowSleep(boolean flag) {
        if (flag == m_allowSleep) {
            return;
        }

        m_allowSleep = flag;
        if (!m_allowSleep) {
            for (Body2D b = m_bodyList; b != null; b = b.next) {
                b.setAwake(true);
            }
        }
    }

    public void setSubStepping(boolean subStepping) {
        this.m_subStepping = subStepping;
    }

    public boolean isSubStepping() {
        return m_subStepping;
    }

    public boolean isAllowSleep() {
        return m_allowSleep;
    }

    public void addFracture(Fracture fracture) {
        //prida frakturu do hashovacej tabulky fraktur
        //ak na dany fixture tam uz existuje fraktura tak sa pozrie, ci je nova fraktura silnejsia a ak ano, tak ju vymeni
        Fracture f = fractures.get(fracture);
        if (f != null) {
            if (f.normalImpulse < fracture.normalImpulse) {
                fractures.remove(f);
                fractures.add(fracture);
            }
        } else {
            fractures.add(fracture);
        }
    }

    public boolean isFractured(Fixture fx) {
        return fractures.contains(fx);
    }


    public DestructionListener getDestructionListener() {
        return m_destructionListener;
    }

    public ParticleDestructionListener getParticleDestructionListener() {
        return m_particleDestructionListener;
    }

    public void setParticleDestructionListener(ParticleDestructionListener listener) {
        m_particleDestructionListener = listener;
    }


    /**
     * Register a destruction listener. The listener is owned by you and must remain in scope.
     *
     * @param listener
     */
    public void setDestructionListener(DestructionListener listener) {
        m_destructionListener = listener;
    }

    /**
     * Register a contact filter to provide specific control over collision. Otherwise the default
     * filter is used (_defaultFilter). The listener is owned by you and must remain in scope.
     *
     * @param filter
     */
    public void setContactFilter(ContactFilter filter) {
        m_contactManager.m_contactFilter = filter;
    }

    /**
     * Register a contact event listener. The listener is owned by you and must remain in scope.
     *
     * @param listener
     */
    public void setContactListener(ContactListener listener) {
        m_contactManager.m_contactListener = listener;
    }

    /**
     * Registruje FractureListener.
     *
     * @param listener
     */
    public void setFractureListener(FractureListener listener) {
        m_contactManager.m_fractureListener = listener;
    }

//    /**
//     * Register a routine for debug drawing. The debug draw functions are called inside with
//     * World.DrawDebugData method. The debug draw object is owned by you and must remain in scope.
//     *
//     * @param debugDraw
//     */
//    public void setDebugDraw(DebugDraw debugDraw) {
//        m_debugDraw = debugDraw;
//    }

    /**
     * create a rigid body given a definition. No reference to the definition is retained.
     *
     * @param def
     * @return
     * @warning This function is locked during callbacks.
     */
    public Body2D newBody(BodyDef def) {
        return newBody(new Body2D(def, this));
    }

    public Body2D newBody(BodyDef def, FixtureDef... fd) {
        return newBody(new Body2D(def, this), fd);
    }

    public Body2D newBody(Body2D b, FixtureDef... fd) {
        //invokeLater(() -> {

            // add to world doubly linked list
            b.prev = null;
            b.next = m_bodyList;
            if (m_bodyList != null) {
                m_bodyList.prev = b;
            }
            m_bodyList = b;
            ++m_bodyCount;

        for (FixtureDef f : fd) {
            b.addFixture(f);
        }
        //});

        return b;
    }

    /**
     * destroy a rigid body given a definition. No reference to the definition is retained. This
     * function is locked during callbacks.
     *
     * @param body
     * @warning This automatically deletes all associated shapes and joints.
     * @warning This function is locked during callbacks.
     */
    public void destroyBody(Body2D body) {
        assert (m_bodyCount > 0);

        //invokeLater(() -> {

            // Delete the attached joints.
            JointEdge je = body.joints;
            while (je != null) {
                JointEdge je0 = je;
                je = je.next;
                if (m_destructionListener != null) {
                    m_destructionListener.sayGoodbye(je0.joint);
                }

                destroyJoint(je0.joint);

                body.joints = je;
            }
            body.joints = null;

            // Delete the attached contacts.
            ContactEdge ce = body.contacts;
            while (ce != null) {
                ContactEdge ce0 = ce;
                ce = ce.next;
                m_contactManager.destroy(ce0.contact);
            }
            body.contacts = null;

            Fixture f = body.fixtures;
            while (f != null) {
                Fixture f0 = f;
                f = f.next;

                if (m_destructionListener != null) {
                    m_destructionListener.sayGoodbye(f0);
                }

                f0.destroyProxies(m_contactManager.m_broadPhase);
                f0.destroy();
                // TODO djm recycle fixtures (here or in that destroy method)
                body.fixtures = f;
                body.fixtureCount -= 1;
            }
            body.fixtures = null;
            body.fixtureCount = 0;

            // Remove world body list.
            if (body.prev != null) {
                body.prev.next = body.next;
            }

            if (body.next != null) {
                body.next.prev = body.prev;
            }

            if (body == m_bodyList) {
                m_bodyList = body.next;
            }

            --m_bodyCount;
            // TODO djm recycle body
        //});
    }

    /**
     * create a joint to constrain bodies together. No reference to the definition is retained. This
     * may cause the connected bodies to cease colliding.
     *
     * @param def
     * @return
     * @warning This function is locked during callbacks.
     */
    public Joint newJoint(JointDef def) {


        Joint j = Joint.create(this, def);

        invokeLater(() -> {

            // Connect to the world list.
            j.m_prev = null;
            j.next = m_jointList;
            if (m_jointList != null) {
                m_jointList.m_prev = j;
            }
            m_jointList = j;
            ++m_jointCount;

            // Connect to the bodies' doubly linked lists.
            j.m_edgeA.joint = j;
            j.m_edgeA.other = j.getBodyB();
            j.m_edgeA.prev = null;
            j.m_edgeA.next = j.getBodyA().joints;
            if (j.getBodyA().joints != null) {
                j.getBodyA().joints.prev = j.m_edgeA;
            }
            j.getBodyA().joints = j.m_edgeA;

            j.m_edgeB.joint = j;
            j.m_edgeB.other = j.getBodyA();
            j.m_edgeB.prev = null;
            j.m_edgeB.next = j.getBodyB().joints;
            if (j.getBodyB().joints != null) {
                j.getBodyB().joints.prev = j.m_edgeB;
            }
            j.getBodyB().joints = j.m_edgeB;

            Body2D bodyA = def.bodyA;
            Body2D bodyB = def.bodyB;

            // If the joint prevents collisions, then flag any contacts for filtering.
            if (!def.collideConnected) {
                ContactEdge edge = bodyB.contacts();
                while (edge != null) {
                    if (edge.other == bodyA) {
                        // Flag the contact for filtering at the next time step (where either
                        // body is awake).
                        edge.contact.flagForFiltering();
                    }

                    edge = edge.next;
                }
            }

            // Note: creating a joint doesn't wake the bodies.

        });

        return j;
    }

    /**
     * destroy a joint. This may cause the connected bodies to begin colliding.
     *
     * @param joint
     * @warning This function is locked during callbacks.
     */
    public void destroyJoint(Joint j) {

        //invokeLater(() -> {

            boolean collideConnected = j.getCollideConnected();

            // Remove from the doubly linked list.
            if (j.m_prev != null) {
                j.m_prev.next = j.next;
            }

            if (j.next != null) {
                j.next.m_prev = j.m_prev;
            }

            if (j == m_jointList) {
                m_jointList = j.next;
            }

            // Disconnect from island graph.
            Body2D bodyA = j.getBodyA();
            Body2D bodyB = j.getBodyB();

            // Wake up connected bodies.
            bodyA.setAwake(true);
            bodyB.setAwake(true);

            // Remove from body 1.
            if (j.m_edgeA.prev != null) {
                j.m_edgeA.prev.next = j.m_edgeA.next;
            }

            if (j.m_edgeA.next != null) {
                j.m_edgeA.next.prev = j.m_edgeA.prev;
            }

            if (j.m_edgeA == bodyA.joints) {
                bodyA.joints = j.m_edgeA.next;
            }

            j.m_edgeA.prev = null;
            j.m_edgeA.next = null;

            // Remove from body 2
            if (j.m_edgeB.prev != null) {
                j.m_edgeB.prev.next = j.m_edgeB.next;
            }

            if (j.m_edgeB.next != null) {
                j.m_edgeB.next.prev = j.m_edgeB.prev;
            }

            if (j.m_edgeB == bodyB.joints) {
                bodyB.joints = j.m_edgeB.next;
            }

            j.m_edgeB.prev = null;
            j.m_edgeB.next = null;

            Joint.destroy(j);

            assert (m_jointCount > 0);
            --m_jointCount;

            // If the joint prevents collisions, then flag any contacts for filtering.
            if (!collideConnected) {
                ContactEdge edge = bodyB.contacts();
                while (edge != null) {
                    if (edge.other == bodyA) {
                        // Flag the contact for filtering at the next time step (where either
                        // body is awake).
                        edge.contact.flagForFiltering();
                    }

                    edge = edge.next;
                }
            }
        //});
    }

    // djm pooling
    private final TimeStep step = new TimeStep();
    private final Timer stepTimer = new Timer();
    private final Timer tempTimer = new Timer();

    public synchronized void invokeLater(Runnable r) {
        if (isLocked()) {
            queue.add(r);
        } else {
            r.run();
        }
    }

    /**
     * Take a time step. This performs collision detection, integration, and constraint solution.
     *
     * @param dt                 the amount of time to simulate, this should not vary.
     * @param velocityIterations for the velocity constraint solver.
     * @param positionIterations for the position constraint solver.
     */
    public void step(float dt, int velocityIterations, int positionIterations) {


        if (!LOCKED.compareAndSet(false, true))
            return; //already busy

        try {

            stepTimer.reset();
            tempTimer.reset();
            fractures.clear();

            Runnable r;
            while ((r = queue.poll()) != null)
                r.run();

            // log.debug("Starting step");
            // If new fixtures were added, we need to find the new contacts.
            if ((flags & NEW_FIXTURE) == NEW_FIXTURE) {
                // log.debug("There's a new fixture, lets look for new contacts");
                m_contactManager.findNewContacts();
                flags &= ~NEW_FIXTURE;
            }

            step.dt = dt;
            step.velocityIterations = velocityIterations;
            step.positionIterations = positionIterations;
            if (dt > 0.0f) {
                step.inv_dt = 1.0f / dt;
            } else {
                step.inv_dt = 0.0f;
            }

            step.dtRatio = m_inv_dt0 * dt;

            step.warmStarting = m_warmStarting;
            m_profile.stepInit.record(tempTimer::getMilliseconds);

            // Update contacts. This is where some contacts are destroyed.
            tempTimer.reset();
            m_contactManager.collide();
            m_profile.collide.record(tempTimer::getMilliseconds);

            // Integrate velocities, solve velocity constraints, and integrate positions.
            if (m_stepComplete && step.dt > 0.0f) {
                tempTimer.reset();
                m_particleSystem.solve(step); // Particle Simulation
                m_profile.solveParticleSystem.record(tempTimer::getMilliseconds);
                tempTimer.reset();
                solve(step);
                m_profile.solve.record(tempTimer::getMilliseconds);
            }

            // Handle TOI events.
            if (m_continuousPhysics && step.dt > 0.0f) {
                tempTimer.reset();
                solveTOI(step);
                m_profile.solveTOI.record(tempTimer::getMilliseconds);
            }

            if (step.dt > 0.0f) {
                m_inv_dt0 = step.inv_dt;
            }

            if ((flags & CLEAR_FORCES) == CLEAR_FORCES) {
                clearForces();
            }
        } finally {
            LOCKED.set(false);
        }

        {

//            Fracture[] array = fractures.toArray(new Fracture[fractures.size()]);
//            for (Fracture f : array)
//                f.smash(smasher, dt);
            fractures.forEach(f -> {
               f.smash(smasher, dt);
            });
        }



        m_profile.step.record(stepTimer.getMilliseconds());
    }

    /**
     * Call this after you are done with time steps to clear the forces. You normally call this after
     * each call to Step, unless you are performing sub-steps. By default, forces will be
     * automatically cleared, so you don't need to call this function.
     *
     * @see setAutoClearForces
     */
    public void clearForces() {
        for (Body2D body = m_bodyList; body != null; body = body.next()) {
            body.force.setZero();
            body.torque = 0.0f;
        }
    }

    private final Color3f color = new Color3f();
    private final Transform xf = new Transform();
    private final Tuple2f cA = new Vec2();
    private final Tuple2f cB = new Vec2();
    private final Vec2Array avs = new Vec2Array();


    private final WorldQueryWrapper wqwrapper = new WorldQueryWrapper();

    /**
     * Query the world for all fixtures that potentially overlap the provided AABB.
     *
     * @param callback a user implemented callback class.
     * @param aabb     the query box.
     */
    public void queryAABB(QueryCallback callback, AABB aabb) {
        wqwrapper.broadPhase = m_contactManager.m_broadPhase;
        wqwrapper.callback = callback;
        m_contactManager.m_broadPhase.query(wqwrapper, aabb);
    }

    /**
     * Query the world for all fixtures and particles that potentially overlap the provided AABB.
     *
     * @param callback         a user implemented callback class.
     * @param particleCallback callback for particles.
     * @param aabb             the query box.
     */
    public void queryAABB(QueryCallback callback, ParticleQueryCallback particleCallback, AABB aabb) {
        wqwrapper.broadPhase = m_contactManager.m_broadPhase;
        wqwrapper.callback = callback;
        m_contactManager.m_broadPhase.query(wqwrapper, aabb);
        m_particleSystem.queryAABB(particleCallback, aabb);
    }

    /**
     * Query the world for all particles that potentially overlap the provided AABB.
     *
     * @param particleCallback callback for particles.
     * @param aabb             the query box.
     */
    public void queryAABB(ParticleQueryCallback particleCallback, AABB aabb) {
        m_particleSystem.queryAABB(particleCallback, aabb);
    }

    private final WorldRayCastWrapper wrcwrapper = new WorldRayCastWrapper();
    private final RayCastInput input = new RayCastInput();

    /**
     * Ray-cast the world for all fixtures in the path of the ray. Your callback controls whether you
     * get the closest point, any point, or n-points. The ray-cast ignores shapes that contain the
     * starting point.
     *
     * @param callback a user implemented callback class.
     * @param point1   the ray starting point
     * @param point2   the ray ending point
     */
    public void raycast(RayCastCallback callback, Tuple2f point1, Tuple2f point2) {
        wrcwrapper.broadPhase = m_contactManager.m_broadPhase;
        wrcwrapper.callback = callback;
        input.maxFraction = 1.0f;
        input.p1.set(point1);
        input.p2.set(point2);
        m_contactManager.m_broadPhase.raycast(wrcwrapper, input);
    }

    /**
     * Ray-cast the world for all fixtures and particles in the path of the ray. Your callback
     * controls whether you get the closest point, any point, or n-points. The ray-cast ignores shapes
     * that contain the starting point.
     *
     * @param callback         a user implemented callback class.
     * @param particleCallback the particle callback class.
     * @param point1           the ray starting point
     * @param point2           the ray ending point
     */
    public void raycast(RayCastCallback callback, ParticleRaycastCallback particleCallback,
                        Tuple2f point1, Tuple2f point2) {
        wrcwrapper.broadPhase = m_contactManager.m_broadPhase;
        wrcwrapper.callback = callback;
        input.maxFraction = 1.0f;
        input.p1.set(point1);
        input.p2.set(point2);
        m_contactManager.m_broadPhase.raycast(wrcwrapper, input);
        m_particleSystem.raycast(particleCallback, point1, point2);
    }

    /**
     * Ray-cast the world for all particles in the path of the ray. Your callback controls whether you
     * get the closest point, any point, or n-points.
     *
     * @param particleCallback the particle callback class.
     * @param point1           the ray starting point
     * @param point2           the ray ending point
     */
    public void raycast(ParticleRaycastCallback particleCallback, Tuple2f point1, Tuple2f point2) {
        m_particleSystem.raycast(particleCallback, point1, point2);
    }

    /**
     * Get the world body list. With the returned body, use Body.getNext to get the next body in the
     * world list. A null body indicates the end of the list.
     *
     * @return the head of the world body list.
     */
    public Body2D bodies() {
        return m_bodyList;
    }

    /**
     * Get the world joint list. With the returned joint, use Joint.getNext to get the next joint in
     * the world list. A null joint indicates the end of the list.
     *
     * @return the head of the world joint list.
     */
    public Joint joints() {
        return m_jointList;
    }

    /**
     * Get the world contact list. With the returned contact, use Contact.getNext to get the next
     * contact in the world list. A null contact indicates the end of the list.
     *
     * @return the head of the world contact list.
     * @warning contacts are created and destroyed in the middle of a time step. Use ContactListener
     * to avoid missing contacts.
     */
    public Contact getContactList() {
        return m_contactManager.m_contactList;
    }

    public boolean isSleepingAllowed() {
        return m_allowSleep;
    }


    /**
     * Enable/disable warm starting. For testing.
     *
     * @param flag
     */
    public void setWarmStarting(boolean flag) {
        m_warmStarting = flag;
    }

    public boolean isWarmStarting() {
        return m_warmStarting;
    }

    /**
     * Enable/disable continuous physics. For testing.
     *
     * @param flag
     */
    public void setContinuousPhysics(boolean flag) {
        m_continuousPhysics = flag;
    }

    public boolean isContinuousPhysics() {
        return m_continuousPhysics;
    }


    /**
     * Get the number of broad-phase proxies.
     *
     * @return
     */
    public int getProxyCount() {
        return m_contactManager.m_broadPhase.getProxyCount();
    }

    /**
     * Get the number of bodies.
     *
     * @return
     */
    public int getBodyCount() {
        return m_bodyCount;
    }

    /**
     * Get the number of joints.
     *
     * @return
     */
    public int getJointCount() {
        return m_jointCount;
    }

    /**
     * Get the number of contacts (each may have 0 or more contact points).
     *
     * @return
     */
    public int getContactCount() {
        return m_contactManager.m_contactCount;
    }

    /**
     * Gets the height of the dynamic tree
     *
     * @return
     */
    public int getTreeHeight() {
        return m_contactManager.m_broadPhase.getTreeHeight();
    }

    /**
     * Gets the balance of the dynamic tree
     *
     * @return
     */
    public int getTreeBalance() {
        return m_contactManager.m_broadPhase.getTreeBalance();
    }

    /**
     * Gets the quality of the dynamic tree
     *
     * @return
     */
    public float getTreeQuality() {
        return m_contactManager.m_broadPhase.getTreeQuality();
    }

    /**
     * Change the global gravity vector.
     *
     * @param gravity
     */
    public void setGravity(Tuple2f gravity) {
        m_gravity.set(gravity);
    }

    /**
     * Get the global gravity vector.
     *
     * @return
     */
    public Tuple2f getGravity() {
        return m_gravity;
    }

    /**
     * Is the world locked (in the middle of a time step).
     *
     * @return
     */
    public boolean isLocked() {
        return LOCKED.get();
    }

    /**
     * Set flag to control automatic clearing of forces after each time step.
     *
     * @param flag
     */
    public void setAutoClearForces(boolean flag) {
        if (flag) {
            flags |= CLEAR_FORCES;
        } else {
            flags &= ~CLEAR_FORCES;
        }
    }

    /**
     * Get the flag that controls automatic clearing of forces after each time step.
     *
     * @return
     */
    public boolean getAutoClearForces() {
        return (flags & CLEAR_FORCES) == CLEAR_FORCES;
    }

    /**
     * Get the contact manager for testing purposes
     *
     * @return
     */
    public ContactManager getContactManager() {
        return m_contactManager;
    }

    public Profile getProfile() {
        return m_profile;
    }

    private final Island island = new Island(this);
    private Body2D[] stack = new Body2D[10]; // TODO djm find a good initial stack number;
    private final Timer broadphaseTimer = new Timer();

    private void solve(TimeStep step) {
        m_profile.solveInit.startAccum();
        m_profile.solveVelocity.startAccum();
        m_profile.solvePosition.startAccum();

        // update previous transforms
        for (Body2D b = m_bodyList; b != null; b = b.next) {
            b.transformPrev.set(b);
            b.preUpdate();
        }

        // Size the island for the worst case.
        island.init(m_bodyCount, m_contactManager.m_contactCount, m_jointCount,
                m_contactManager.m_contactListener);

        // Clear all the island flags.
        for (Body2D b = m_bodyList; b != null; b = b.next) {
            b.flags &= ~Body2D.e_islandFlag;
        }
        for (Contact c = m_contactManager.m_contactList; c != null; c = c.m_next) {
            c.m_flags &= ~Contact.ISLAND_FLAG;
        }
        for (Joint j = m_jointList; j != null; j = j.next) {
            j.m_islandFlag = false;
        }

        // Build and simulate all awake islands.
        int stackSize = m_bodyCount;
        if (stack.length < stackSize) {
            stack = new Body2D[stackSize];
        }
        for (Body2D seed = m_bodyList; seed != null; seed = seed.next) {
            if ((seed.flags & Body2D.e_islandFlag) == Body2D.e_islandFlag) {
                continue;
            }

            if (!seed.isAwake() || !seed.isActive()) {
                continue;
            }

            // The seed can be dynamic or kinematic.
            if (seed.getType() == BodyType.STATIC) {
                continue;
            }

            // Reset island and stack.
            island.clear();
            int stackCount = 0;
            stack[stackCount++] = seed;
            seed.flags |= Body2D.e_islandFlag;

            // Perform a depth first search (DFS) on the constraint graph.
            while (stackCount > 0) {
                // Grab the next body off the stack and add it to the island.
                Body2D b = stack[--stackCount];
                assert (b.isActive());
                island.add(b);

                // Make sure the body is awake.
                b.setAwake(true);

                // To keep islands as small as possible, we don't
                // propagate islands across static bodies.
                if (b.getType() == BodyType.STATIC) {
                    continue;
                }

                // Search all contacts connected to this body.
                for (ContactEdge ce = b.contacts; ce != null; ce = ce.next) {
                    Contact contact = ce.contact;

                    // Has this contact already been added to an island?
                    if ((contact.m_flags & Contact.ISLAND_FLAG) == Contact.ISLAND_FLAG) {
                        continue;
                    }

                    // Is this contact solid and touching?
                    if (!contact.isEnabled() || !contact.isTouching()) {
                        continue;
                    }

                    // Skip sensors.
                    boolean sensorA = contact.aFixture.isSensor;
                    boolean sensorB = contact.bFixture.isSensor;
                    if (sensorA || sensorB) {
                        continue;
                    }

                    island.add(contact);
                    contact.m_flags |= Contact.ISLAND_FLAG;

                    Body2D other = ce.other;

                    // Was the other body already added to this island?
                    if ((other.flags & Body2D.e_islandFlag) == Body2D.e_islandFlag) {
                        continue;
                    }

                    assert (stackCount < stackSize);
                    stack[stackCount++] = other;
                    other.flags |= Body2D.e_islandFlag;
                }

                // Search all joints connect to this body.
                for (JointEdge je = b.joints; je != null; je = je.next) {
                    if (je.joint.m_islandFlag) {
                        continue;
                    }

                    Body2D other = je.other;

                    // Don't simulate joints connected to inactive bodies.
                    if (!other.isActive()) {
                        continue;
                    }

                    island.add(je.joint);
                    je.joint.m_islandFlag = true;

                    if ((other.flags & Body2D.e_islandFlag) == Body2D.e_islandFlag) {
                        continue;
                    }

                    assert (stackCount < stackSize);
                    stack[stackCount++] = other;
                    other.flags |= Body2D.e_islandFlag;
                }
            }
            island.solve(m_profile, step, m_gravity, m_allowSleep);

            // Post solve cleanup.
            for (int i = 0; i < island.m_bodyCount; ++i) {
                // Allow static bodies to participate in other islands.
                Body2D b = island.m_bodies[i];
                if (b.getType() == BodyType.STATIC) {
                    b.flags &= ~Body2D.e_islandFlag;
                }
            }
        }
        m_profile.solveInit.endAccum();
        m_profile.solveVelocity.endAccum();
        m_profile.solvePosition.endAccum();

        broadphaseTimer.reset();
        // Synchronize fixtures, check for out of range bodies.
        for (Body2D b = m_bodyList; b != null; b = b.next()) {
            // If a body was not in an island then it did not move.
            if ((b.flags & Body2D.e_islandFlag) == 0) {
                continue;
            }

            if (b.getType() == BodyType.STATIC) {
                continue;
            }

            // Update fixtures (for broad-phase).
            b.synchronizeFixtures();
            b.postUpdate();
        }

        // Look for new contacts.
        m_contactManager.findNewContacts();
        m_profile.broadphase.record(broadphaseTimer.getMilliseconds());
    }


    private void solveTOI(final TimeStep step) {

        final Island island = toiIsland;
        island.init(2 * Settings.maxTOIContacts, Settings.maxTOIContacts, 0,
                m_contactManager.m_contactListener);
        if (m_stepComplete) {
            for (Body2D b = m_bodyList; b != null; b = b.next) {
                b.flags &= ~Body2D.e_islandFlag;
                b.sweep.alpha0 = 0.0f;
            }

            for (Contact c = m_contactManager.m_contactList; c != null; c = c.m_next) {
                // Invalidate TOI
                c.m_flags &= ~(Contact.TOI_FLAG | Contact.ISLAND_FLAG);
                c.m_toiCount = 0;
                c.m_toi = 1.0f;
            }
        }

        // Find TOI events and solve them.
        for (; ; ) {
            // Find the first TOI.
            Contact minContact = null;
            float minAlpha = 1.0f;

            for (Contact c = m_contactManager.m_contactList; c != null; c = c.m_next) {
                // Is this contact disabled?
                if (!c.isEnabled()) {
                    continue;
                }

                // Prevent excessive sub-stepping.
                if (c.m_toiCount > Settings.maxSubSteps) {
                    continue;
                }

                float alpha = 1.0f;
                if ((c.m_flags & Contact.TOI_FLAG) != 0) {
                    // This contact has a valid cached TOI.
                    alpha = c.m_toi;
                } else {
                    Fixture fA = c.aFixture;
                    Fixture fB = c.bFixture;

                    // Is there a sensor?
                    if (fA.isSensor() || fB.isSensor()) {
                        continue;
                    }

                    Body2D bA = fA.getBody();
                    Body2D bB = fB.getBody();

                    BodyType typeA = bA.type;
                    BodyType typeB = bB.type;
                    assert (typeA == BodyType.DYNAMIC || typeB == BodyType.DYNAMIC);

                    boolean activeA = bA.isAwake() && typeA != BodyType.STATIC;
                    boolean activeB = bB.isAwake() && typeB != BodyType.STATIC;

                    // Is at least one body active (awake and dynamic or kinematic)?
                    if (!activeA && !activeB) {
                        continue;
                    }

                    boolean collideA = bA.isBullet() || typeA != BodyType.DYNAMIC;
                    boolean collideB = bB.isBullet() || typeB != BodyType.DYNAMIC;

                    // Are these two non-bullet dynamic bodies?
                    if (!collideA && !collideB) {
                        continue;
                    }

                    // Compute the TOI for this contact.
                    // Put the sweeps onto the same time interval.
                    float alpha0 = bA.sweep.alpha0;

                    if (bA.sweep.alpha0 < bB.sweep.alpha0) {
                        alpha0 = bB.sweep.alpha0;
                        bA.sweep.advance(alpha0);
                    } else if (bB.sweep.alpha0 < bA.sweep.alpha0) {
                        alpha0 = bA.sweep.alpha0;
                        bB.sweep.advance(alpha0);
                    }

                    assert (alpha0 < 1.0f);

                    int indexA = c.aIndex;
                    int indexB = c.bIndex;

                    // Compute the time of impact in interval [0, minTOI]
                    final TOIInput input = toiInput;
                    input.proxyA.set(fA.shape(), indexA);
                    input.proxyB.set(fB.shape(), indexB);
                    input.sweepA.set(bA.sweep);
                    input.sweepB.set(bB.sweep);
                    input.tMax = 1.0f;

                    pool.getTimeOfImpact().timeOfImpact(toiOutput, input);

                    // Beta is the fraction of the remaining portion of the .
                    float beta = toiOutput.t;
                    if (toiOutput.state == TOIOutputState.TOUCHING) {
                        alpha = MathUtils.min(alpha0 + (1.0f - alpha0) * beta, 1.0f);
                    } else {
                        alpha = 1.0f;
                    }

                    c.m_toi = alpha;
                    c.m_flags |= Contact.TOI_FLAG;
                }

                if (alpha < minAlpha) {
                    // This is the minimum TOI found so far.
                    minContact = c;
                    minAlpha = alpha;
                }
            }

            if (minContact == null || 1.0f - 10.0f * Settings.EPSILON < minAlpha) {
                // No more TOI events. Done!
                m_stepComplete = true;
                break;
            }

            // Advance the bodies to the TOI.
            Fixture fA = minContact.aFixture;
            Fixture fB = minContact.bFixture;
            Body2D bA = fA.getBody();
            Body2D bB = fB.getBody();

            backup1.set(bA.sweep);
            backup2.set(bB.sweep);

            bA.advance(minAlpha);
            bB.advance(minAlpha);

            // The TOI contact likely has some new contact points.
            minContact.update(m_contactManager.m_contactListener);
            minContact.m_flags &= ~Contact.TOI_FLAG;
            ++minContact.m_toiCount;

            // Is the contact solid?
            if (minContact.isEnabled() == false || minContact.isTouching() == false) {
                // Restore the sweeps.
                minContact.setEnabled(false);
                bA.sweep.set(backup1);
                bB.sweep.set(backup2);
                bA.synchronizeTransform();
                bB.synchronizeTransform();
                continue;
            }

            bA.setAwake(true);
            bB.setAwake(true);

            // Build the island
            island.clear();
            island.add(bA);
            island.add(bB);
            island.add(minContact);

            bA.flags |= Body2D.e_islandFlag;
            bB.flags |= Body2D.e_islandFlag;
            minContact.m_flags |= Contact.ISLAND_FLAG;

            // Get contacts on bodyA and bodyB.
            tempBodies[0] = bA;
            tempBodies[1] = bB;
            for (int i = 0; i < 2; ++i) {
                Body2D body = tempBodies[i];
                if (body.type == BodyType.DYNAMIC) {
                    for (ContactEdge ce = body.contacts; ce != null; ce = ce.next) {
                        if (island.m_bodyCount == island.m_bodyCapacity) {
                            break;
                        }

                        if (island.m_contactCount == island.m_contactCapacity) {
                            break;
                        }

                        Contact contact = ce.contact;

                        // Has this contact already been added to the island?
                        if ((contact.m_flags & Contact.ISLAND_FLAG) != 0) {
                            continue;
                        }

                        // Only add static, kinematic, or bullet bodies.
                        Body2D other = ce.other;
                        if (other.type == BodyType.DYNAMIC && !body.isBullet()
                                && !other.isBullet()) {
                            continue;
                        }

                        // Skip sensors.
                        boolean sensorA = contact.aFixture.isSensor;
                        boolean sensorB = contact.bFixture.isSensor;
                        if (sensorA || sensorB) {
                            continue;
                        }

                        // Tentatively advance the body to the TOI.
                        backup1.set(other.sweep);
                        if ((other.flags & Body2D.e_islandFlag) == 0) {
                            other.advance(minAlpha);
                        }

                        // Update the contact points
                        contact.update(m_contactManager.m_contactListener);

                        // Was the contact disabled by the user?
                        if (!contact.isEnabled()) {
                            other.sweep.set(backup1);
                            other.synchronizeTransform();
                            continue;
                        }

                        // Are there contact points?
                        if (!contact.isTouching()) {
                            other.sweep.set(backup1);
                            other.synchronizeTransform();
                            continue;
                        }

                        // Add the contact to the island
                        contact.m_flags |= Contact.ISLAND_FLAG;
                        island.add(contact);

                        // Has the other body already been added to the island?
                        if ((other.flags & Body2D.e_islandFlag) != 0) {
                            continue;
                        }

                        // Add the other body to the island.
                        other.flags |= Body2D.e_islandFlag;

                        if (other.type != BodyType.STATIC) {
                            other.setAwake(true);
                        }

                        island.add(other);
                    }
                }
            }

            subStep.dt = (1.0f - minAlpha) * step.dt;
            subStep.inv_dt = 1.0f / subStep.dt;
            subStep.dtRatio = 1.0f;
            subStep.positionIterations = 20;
            subStep.velocityIterations = step.velocityIterations;
            subStep.warmStarting = false;
            island.solveTOI(subStep, bA.island, bB.island);

            // Reset island flags and synchronize broad-phase proxies.
            for (int i = 0; i < island.m_bodyCount; ++i) {
                Body2D body = island.m_bodies[i];
                body.flags &= ~Body2D.e_islandFlag;

                if (body.type != BodyType.DYNAMIC) {
                    continue;
                }

                body.synchronizeFixtures();

                // Invalidate all contact TOIs on this displaced body.
                for (ContactEdge ce = body.contacts; ce != null; ce = ce.next) {
                    ce.contact.m_flags &= ~(Contact.TOI_FLAG | Contact.ISLAND_FLAG);
                }
            }

            // Commit fixture proxy movements to the broad-phase so that new contacts are created.
            // Also, some contacts can be destroyed.
            m_contactManager.findNewContacts();

            if (m_subStepping) {
                m_stepComplete = false;
                break;
            }
        }
    }


    // NOTE this corresponds to the liquid test, so the debugdraw can draw
    // the liquid particles correctly. They should be the same.
    private static final Integer LIQUID_INT = 1234598372;
    private final float liquidLength = .12f;
    private final float averageLinearVel = -1;
    private final Tuple2f liquidOffset = new Vec2();
    private final Tuple2f circCenterMoved = new Vec2();
    private final Color3f liquidColor = new Color3f(.4f, .4f, 1f);

    private final Tuple2f center = new v2();
    private final Tuple2f axis = new v2();
    private final Tuple2f V = new v2();
    private final Tuple2f W = new v2();
    private final Vec2Array tlvertices = new Vec2Array();


    /**
     * Create a particle whose properties have been defined. No reference to the definition is
     * retained. A simulation step must occur before it's possible to interact with a newly created
     * particle. For example, DestroyParticleInShape() will not destroy a particle until Step() has
     * been called.
     *
     * @return the index of the particle.
     * @warning This function is locked during callbacks.
     */
    public int createParticle(ParticleDef def) {
//        assert (!isLocked());
//        if (isLocked()) {
//            return 0;
//        }
        int p = m_particleSystem.createParticle(def);
        return p;
    }

    /**
     * Destroy a particle. The particle is removed after the next step.
     *
     * @param index
     */
    public void destroyParticle(int index) {
        destroyParticle(index, false);
    }

    /**
     * Destroy a particle. The particle is removed after the next step.
     *
     * @param Index   of the particle to destroy.
     * @param Whether to call the destruction listener just before the particle is destroyed.
     */
    public void destroyParticle(int index, boolean callDestructionListener) {
        m_particleSystem.destroyParticle(index, callDestructionListener);
    }

    /**
     * Destroy particles inside a shape without enabling the destruction callback for destroyed
     * particles. This function is locked during callbacks. For more information see
     * DestroyParticleInShape(Shape&, Transform&,bool).
     *
     * @param Shape     which encloses particles that should be destroyed.
     * @param Transform applied to the shape.
     * @return Number of particles destroyed.
     * @warning This function is locked during callbacks.
     */
    public int destroyParticlesInShape(Shape shape, Transform xf) {
        return destroyParticlesInShape(shape, xf, false);
    }

    /**
     * Destroy particles inside a shape. This function is locked during callbacks. In addition, this
     * function immediately destroys particles in the shape in contrast to DestroyParticle() which
     * defers the destruction until the next simulation step.
     *
     * @param Shape     which encloses particles that should be destroyed.
     * @param Transform applied to the shape.
     * @param Whether   to call the world b2DestructionListener for each particle destroyed.
     * @return Number of particles destroyed.
     * @warning This function is locked during callbacks.
     */
    public int destroyParticlesInShape(Shape shape, Transform xf, boolean callDestructionListener) {
//        assert (!isLocked());
//        if (isLocked()) {
//            return 0;
//        }
        return m_particleSystem.destroyParticlesInShape(shape, xf, callDestructionListener);
    }

    /**
     * Create a particle group whose properties have been defined. No reference to the definition is
     * retained.
     *
     * @warning This function is locked during callbacks.
     */
    public ParticleGroup createParticleGroup(ParticleGroupDef def) {
//        assert (!isLocked());
//        if (isLocked()) {
//            return null;
//        }
        ParticleGroup g = m_particleSystem.createParticleGroup(def);
        return g;
    }

    /**
     * Join two particle groups.
     *
     * @param the first group. Expands to encompass the second group.
     * @param the second group. It is destroyed.
     * @warning This function is locked during callbacks.
     */
    public void joinParticleGroups(ParticleGroup groupA, ParticleGroup groupB) {
//        assert (!isLocked());
//        if (isLocked()) {
//            return;
//        }
        m_particleSystem.joinParticleGroups(groupA, groupB);
    }

    /**
     * Destroy particles in a group. This function is locked during callbacks.
     *
     * @param The     particle group to destroy.
     * @param Whether to call the world b2DestructionListener for each particle is destroyed.
     * @warning This function is locked during callbacks.
     */
    public void destroyParticlesInGroup(ParticleGroup group, boolean callDestructionListener) {
//        assert (!isLocked());
//        if (isLocked()) {
//            return;
//        }
        m_particleSystem.destroyParticlesInGroup(group, callDestructionListener);
    }

    /**
     * Destroy particles in a group without enabling the destruction callback for destroyed particles.
     * This function is locked during callbacks.
     *
     * @param The particle group to destroy.
     * @warning This function is locked during callbacks.
     */
    public void destroyParticlesInGroup(ParticleGroup group) {
        destroyParticlesInGroup(group, false);
    }

    /**
     * Get the world particle group list. With the returned group, use ParticleGroup::GetNext to get
     * the next group in the world list. A NULL group indicates the end of the list.
     *
     * @return the head of the world particle group list.
     */
    public ParticleGroup[] getParticleGroupList() {
        return m_particleSystem.getParticleGroupList();
    }

    /**
     * Get the number of particle groups.
     *
     * @return
     */
    public int getParticleGroupCount() {
        return m_particleSystem.getParticleGroupCount();
    }

    /**
     * Get the number of particles.
     *
     * @return
     */
    public int getParticleCount() {
        return m_particleSystem.getParticleCount();
    }

    /**
     * Get the maximum number of particles.
     *
     * @return
     */
    public int getParticleMaxCount() {
        return m_particleSystem.getParticleMaxCount();
    }

    /**
     * Set the maximum number of particles.
     *
     * @param count
     */
    public void setParticleMaxCount(int count) {
        m_particleSystem.setParticleMaxCount(count);
    }

    /**
     * Change the particle density.
     *
     * @param density
     */
    public void setParticleDensity(float density) {
        m_particleSystem.setParticleDensity(density);
    }

    /**
     * Get the particle density.
     *
     * @return
     */
    public float getParticleDensity() {
        return m_particleSystem.getParticleDensity();
    }

    /**
     * Change the particle gravity scale. Adjusts the effect of the global gravity vector on
     * particles. Default value is 1.0f.
     *
     * @param gravityScale
     */
    public void setParticleGravityScale(float gravityScale) {
        m_particleSystem.setParticleGravityScale(gravityScale);

    }

    /**
     * Get the particle gravity scale.
     *
     * @return
     */
    public float getParticleGravityScale() {
        return m_particleSystem.getParticleGravityScale();
    }

    /**
     * Damping is used to reduce the velocity of particles. The damping parameter can be larger than
     * 1.0f but the damping effect becomes sensitive to the time step when the damping parameter is
     * large.
     *
     * @param damping
     */
    public void setParticleDamping(float damping) {
        m_particleSystem.setParticleDamping(damping);
    }

    /**
     * Get damping for particles
     *
     * @return
     */
    public float getParticleDamping() {
        return m_particleSystem.getParticleDamping();
    }

    /**
     * Change the particle radius. You should set this only once, on world start. If you change the
     * radius during execution, existing particles may explode, shrink, or behave unexpectedly.
     *
     * @param radius
     */
    public void setParticleRadius(float radius) {
        m_particleSystem.setParticleRadius(radius);
    }

    /**
     * Get the particle radius.
     *
     * @return
     */
    public float getParticleRadius() {
        return m_particleSystem.getParticleRadius();
    }

    /**
     * Get the particle data. @return the pointer to the head of the particle data.
     *
     * @return
     */
    public int[] getParticleFlagsBuffer() {
        return m_particleSystem.getParticleFlagsBuffer();
    }

    public Tuple2f[] getParticlePositionBuffer() {
        return m_particleSystem.getParticlePositionBuffer();
    }

    public Tuple2f[] getParticleVelocityBuffer() {
        return m_particleSystem.getParticleVelocityBuffer();
    }

    public ParticleColor[] getParticleColorBuffer() {
        return m_particleSystem.getParticleColorBuffer();
    }

    public ParticleGroup[] getParticleGroupBuffer() {
        return m_particleSystem.getParticleGroupBuffer();
    }

    public Object[] getParticleUserDataBuffer() {
        return m_particleSystem.getParticleUserDataBuffer();
    }

    /**
     * Set a buffer for particle data.
     *
     * @param buffer is a pointer to a block of memory.
     * @param size   is the number of values in the block.
     */
    public void setParticleFlagsBuffer(int[] buffer, int capacity) {
        m_particleSystem.setParticleFlagsBuffer(buffer, capacity);
    }

    public void setParticlePositionBuffer(v2[] buffer, int capacity) {
        m_particleSystem.setParticlePositionBuffer(buffer, capacity);

    }

    public void setParticleVelocityBuffer(v2[] buffer, int capacity) {
        m_particleSystem.setParticleVelocityBuffer(buffer, capacity);

    }

    public void setParticleColorBuffer(ParticleColor[] buffer, int capacity) {
        m_particleSystem.setParticleColorBuffer(buffer, capacity);

    }

    public void setParticleUserDataBuffer(Object[] buffer, int capacity) {
        m_particleSystem.setParticleUserDataBuffer(buffer, capacity);
    }

    /**
     * Get contacts between particles
     *
     * @return
     */
    public ParticleContact[] getParticleContacts() {
        return m_particleSystem.m_contactBuffer;
    }

    public int getParticleContactCount() {
        return m_particleSystem.m_contactCount;
    }

    /**
     * Get contacts between particles and bodies
     *
     * @return
     */
    public ParticleBodyContact[] getParticleBodyContacts() {
        return m_particleSystem.m_bodyContactBuffer;
    }

    public int getParticleBodyContactCount() {
        return m_particleSystem.m_bodyContactCount;
    }

    /**
     * Compute the kinetic energy that can be lost by damping force
     *
     * @return
     */
    public float computeParticleCollisionEnergy() {
        return m_particleSystem.computeParticleCollisionEnergy();
    }

    public Body2D newDynamicBody(Shape shape, float density, float friction) {
        return newBody(new BodyDef(BodyType.DYNAMIC, new v2()),
                new FixtureDef(shape, density, friction));
    }

    public static class Profile {

        public boolean active = false;

        private static final int LONG_AVG_NUMS = 20;
        private static final float LONG_FRACTION = 1f / LONG_AVG_NUMS;
        private static final int SHORT_AVG_NUMS = 5;
        private static final float SHORT_FRACTION = 1f / SHORT_AVG_NUMS;

        public class ProfileEntry {
            float longAvg;
            float shortAvg;
            float min;
            float max;
            float accum;

            ProfileEntry() {
                min = Float.MAX_VALUE;
                max = -Float.MAX_VALUE;
            }

            public void record(FloatSupplier value) {
                if (active)
                    record(value.asFloat());
            }

            public void record(float value) {
                longAvg = longAvg * (1 - LONG_FRACTION) + value * LONG_FRACTION;
                shortAvg = shortAvg * (1 - SHORT_FRACTION) + value * SHORT_FRACTION;
                min = MathUtils.min(value, min);
                max = MathUtils.max(value, max);
            }

            public void startAccum() {
                accum = 0;
            }

            public void accum(FloatSupplier value) {
                if (active)
                    accum(value.asFloat());
            }

            public void accum(float value) {
                accum += value;
            }

            public void endAccum() {
                record(accum);
            }

            @Override
            public String toString() {
                return String.format("%.2f (%.2f) [%.2f,%.2f]", shortAvg, longAvg, min, max);
            }
        }

        public final ProfileEntry step = new ProfileEntry();
        public final ProfileEntry stepInit = new ProfileEntry();
        public final ProfileEntry collide = new ProfileEntry();
        public final ProfileEntry solveParticleSystem = new ProfileEntry();
        public final ProfileEntry solve = new ProfileEntry();
        public final ProfileEntry solveInit = new ProfileEntry();
        public final ProfileEntry solveVelocity = new ProfileEntry();
        public final ProfileEntry solvePosition = new ProfileEntry();
        public final ProfileEntry broadphase = new ProfileEntry();
        public final ProfileEntry solveTOI = new ProfileEntry();

        public void toDebugStrings(List<String> strings) {
            strings.add("Profile:");
            strings.add(" step: " + step);
            strings.add("  init: " + stepInit);
            strings.add("  collide: " + collide);
            strings.add("  particles: " + solveParticleSystem);
            strings.add("  solve: " + solve);
            strings.add("   solveInit: " + solveInit);
            strings.add("   solveVelocity: " + solveVelocity);
            strings.add("   solvePosition: " + solvePosition);
            strings.add("   broadphase: " + broadphase);
            strings.add("  solveTOI: " + solveTOI);
        }
    }
}


class WorldQueryWrapper implements TreeCallback {
    public boolean treeCallback(int nodeId) {
        FixtureProxy proxy = (FixtureProxy) broadPhase.getUserData(nodeId);
        return callback.reportFixture(proxy.fixture);
    }

    BroadPhase broadPhase;
    QueryCallback callback;
}


class WorldRayCastWrapper implements TreeRayCastCallback {

    // djm pooling
    private final RayCastOutput output = new RayCastOutput();
    private final Tuple2f temp = new Vec2();
    private final Tuple2f point = new Vec2();

    public float raycastCallback(RayCastInput input, int nodeId) {
        Object userData = broadPhase.getUserData(nodeId);
        FixtureProxy proxy = (FixtureProxy) userData;
        Fixture fixture = proxy.fixture;
        int index = proxy.childIndex;
        boolean hit = fixture.raycast(output, input, index);

        if (hit) {
            float fraction = output.fraction;
            // Vec2 point = (1.0f - fraction) * input.p1 + fraction * input.p2;
            temp.set(input.p2).scaled(fraction);
            point.set(input.p1).scaled(1 - fraction).added(temp);
            return callback.reportFixture(fixture, point, output.normal, fraction);
        }

        return input.maxFraction;
    }

    BroadPhase broadPhase;
    RayCastCallback callback;
}

//    private void drawJoint(Joint joint) {
//        Body bodyA = joint.getBodyA();
//        Body bodyB = joint.getBodyB();
//        Transform xf1 = bodyA.getTransform();
//        Transform xf2 = bodyB.getTransform();
//        Tuple2f x1 = xf1.p;
//        Tuple2f x2 = xf2.p;
//        Tuple2f p1 = pool.popVec2();
//        Tuple2f p2 = pool.popVec2();
//        joint.getAnchorA(p1);
//        joint.getAnchorB(p2);
//
//        color.set(0.5f, 0.8f, 0.8f);
//
//        switch (joint.getType()) {
//            // TODO djm write after writing joints
//            case DISTANCE:
//                m_debugDraw.drawSegment(p1, p2, color);
//                break;
//
//            case PULLEY: {
//                PulleyJoint pulley = (PulleyJoint) joint;
//                Tuple2f s1 = pulley.getGroundAnchorA();
//                Tuple2f s2 = pulley.getGroundAnchorB();
//                m_debugDraw.drawSegment(s1, p1, color);
//                m_debugDraw.drawSegment(s2, p2, color);
//                m_debugDraw.drawSegment(s1, s2, color);
//            }
//            break;
//            case CONSTANT_VOLUME:
//            case MOUSE:
//                // don't draw this
//                break;
//            default:
//                m_debugDraw.drawSegment(x1, p1, color);
//                m_debugDraw.drawSegment(p1, p2, color);
//                m_debugDraw.drawSegment(x2, p2, color);
//        }
//        pool.pushVec2(2);
//    }
//
//    private void drawShape(Fixture fixture, Transform xf, Color3f color, boolean wireframe) {
//        switch (fixture.getType()) {
//            case CIRCLE: {
//                CircleShape circle = (CircleShape) fixture.getShape();
//
//                // Vec2 center = Mul(xf, circle.m_p);
//                Transform.mulToOutUnsafe(xf, circle.m_p, center);
//                float radius = circle.m_radius;
//                xf.q.getXAxis(axis);
//
//                if (fixture.getUserData() != null && fixture.getUserData().equals(LIQUID_INT)) {
//                    Body b = fixture.getBody();
//                    liquidOffset.set(b.m_linearVelocity);
//                    float linVelLength = b.m_linearVelocity.length();
//                    if (averageLinearVel == -1) {
//                        averageLinearVel = linVelLength;
//                    } else {
//                        averageLinearVel = .98f * averageLinearVel + .02f * linVelLength;
//                    }
//                    liquidOffset.scaled(liquidLength / averageLinearVel / 2);
//                    circCenterMoved.set(center).added(liquidOffset);
//                    center.subbed(liquidOffset);
//                    m_debugDraw.drawSegment(center, circCenterMoved, liquidColor);
//                    return;
//                }
//                if (wireframe) {
//                    m_debugDraw.drawCircle(center, radius, axis, color);
//                } else {
//                    m_debugDraw.drawSolidCircle(center, radius, axis, color);
//                }
//            }
//            break;
//
//            case POLYGON: {
//                PolygonShape poly = (PolygonShape) fixture.getShape();
//                int vertexCount = poly.m_count;
//                assert (vertexCount <= Settings.maxPolygonVertices);
//                Tuple2f[] vertices = tlvertices.get(Settings.maxPolygonVertices);
//
//                for (int i = 0; i < vertexCount; ++i) {
//                    // vertices[i] = Mul(xf, poly.m_vertices[i]);
//                    Transform.mulToOutUnsafe(xf, poly.m_vertices[i], vertices[i]);
//                }
//                if (wireframe) {
//                    m_debugDraw.drawPolygon(vertices, vertexCount, color);
//                } else {
//                    m_debugDraw.drawSolidPolygon(vertices, vertexCount, color);
//                }
//            }
//            break;
//            case EDGE: {
//                EdgeShape edge = (EdgeShape) fixture.getShape();
//                Transform.mulToOutUnsafe(xf, edge.m_vertex1, V);
//                Transform.mulToOutUnsafe(xf, edge.m_vertex2, W);
//                m_debugDraw.drawSegment(V, W, color);
//            }
//            break;
//            case CHAIN: {
//                ChainShape chain = (ChainShape) fixture.getShape();
//                int count = chain.m_count;
//                Tuple2f[] vertices = chain.m_vertices;
//
//                Transform.mulToOutUnsafe(xf, vertices[0], V);
//                for (int i = 1; i < count; ++i) {
//                    Transform.mulToOutUnsafe(xf, vertices[i], W);
//                    m_debugDraw.drawSegment(V, W, color);
//                    m_debugDraw.drawCircle(V, 0.05f, color);
//                    V.set(W);
//                }
//            }
//            break;
//            default:
//                break;
//        }
//    }
//
//    private void drawParticleSystem(ParticleSystem system) {
//        boolean wireframe = (m_debugDraw.getFlags() & DebugDraw.e_wireframeDrawingBit) != 0;
//        int particleCount = system.getParticleCount();
//        if (particleCount != 0) {
//            float particleRadius = system.getParticleRadius();
//            Tuple2f[] positionBuffer = system.getParticlePositionBuffer();
//            ParticleColor[] colorBuffer = null;
//            if (system.m_colorBuffer.data != null) {
//                colorBuffer = system.getParticleColorBuffer();
//            }
//            if (wireframe) {
//                m_debugDraw.drawParticlesWireframe(positionBuffer, particleRadius, colorBuffer,
//                        particleCount);
//            } else {
//                m_debugDraw.drawParticles(positionBuffer, particleRadius, colorBuffer, particleCount);
//            }
//        }
//    }
//    /**
//     * Call this to draw shapes and other debug draw data.
//     */
//    public void drawDebugData() {
//        if (m_debugDraw == null) {
//            return;
//        }
//
//
//        int flags = m_debugDraw.getFlags();
//        boolean wireframe = (flags & DebugDraw.e_wireframeDrawingBit) != 0;
//
//        if ((flags & DebugDraw.e_shapeBit) != 0) {
//            for (Body b = m_bodyList; b != null; b = b.getNext()) {
//                xf.set(b.getTransform());
//                for (Fixture f = b.getFixtureList(); f != null; f = f.getNext()) {
//                    if (b.isActive() == false) {
//                        color.set(0.5f, 0.5f, 0.3f);
//                        drawShape(f, xf, color, wireframe);
//                    } else if (b.getType() == BodyType.STATIC) {
//                        color.set(0.5f, 0.9f, 0.3f);
//                        drawShape(f, xf, color, wireframe);
//                    } else if (b.getType() == BodyType.KINEMATIC) {
//                        color.set(0.5f, 0.5f, 0.9f);
//                        drawShape(f, xf, color, wireframe);
//                    } else if (b.isAwake() == false) {
//                        color.set(0.5f, 0.5f, 0.5f);
//                        drawShape(f, xf, color, wireframe);
//                    } else {
//                        color.set(0.9f, 0.7f, 0.7f);
//                        drawShape(f, xf, color, wireframe);
//                    }
//                }
//            }
//            drawParticleSystem(m_particleSystem);
//        }
//
//        if ((flags & DebugDraw.e_jointBit) != 0) {
//            for (Joint j = m_jointList; j != null; j = j.getNext()) {
//                drawJoint(j);
//            }
//        }
//
//        if ((flags & DebugDraw.e_pairBit) != 0) {
//            color.set(0.3f, 0.9f, 0.9f);
//            for (Contact c = m_contactManager.m_contactList; c != null; c = c.getNext()) {
//                Fixture fixtureA = c.getFixtureA();
//                Fixture fixtureB = c.getFixtureB();
//                fixtureA.getAABB(c.getChildIndexA()).getCenterToOut(cA);
//                fixtureB.getAABB(c.getChildIndexB()).getCenterToOut(cB);
//                m_debugDraw.drawSegment(cA, cB, color);
//            }
//        }
//
//        if ((flags & DebugDraw.e_aabbBit) != 0) {
//            color.set(0.9f, 0.3f, 0.9f);
//
//            for (Body b = m_bodyList; b != null; b = b.getNext()) {
//                if (b.isActive() == false) {
//                    continue;
//                }
//
//                for (Fixture f = b.getFixtureList(); f != null; f = f.getNext()) {
//                    for (int i = 0; i < f.m_proxyCount; ++i) {
//                        FixtureProxy proxy = f.m_proxies[i];
//                        AABB aabb = m_contactManager.m_broadPhase.getFatAABB(proxy.proxyId);
//                        if (aabb != null) {
//                            Tuple2f[] vs = avs.get(4);
//                            vs[0].set(aabb.lowerBound.x, aabb.lowerBound.y);
//                            vs[1].set(aabb.upperBound.x, aabb.lowerBound.y);
//                            vs[2].set(aabb.upperBound.x, aabb.upperBound.y);
//                            vs[3].set(aabb.lowerBound.x, aabb.upperBound.y);
//                            m_debugDraw.drawPolygon(vs, 4, color);
//                        }
//                    }
//                }
//            }
//        }
//
//        if ((flags & DebugDraw.e_centerOfMassBit) != 0) {
//            for (Body b = m_bodyList; b != null; b = b.getNext()) {
//                xf.set(b.getTransform());
//                xf.p.set(b.getWorldCenter());
//                m_debugDraw.drawTransform(xf);
//            }
//        }
//
//        if ((flags & DebugDraw.e_dynamicTreeBit) != 0) {
//            m_contactManager.m_broadPhase.drawTree(m_debugDraw);
//        }
//
//        m_debugDraw.flush();
//    }