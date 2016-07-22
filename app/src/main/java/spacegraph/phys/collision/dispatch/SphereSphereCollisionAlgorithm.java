/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * Bullet Continuous Collision Detection and Physics Library
 * Copyright (c) 2003-2008 Erwin Coumans  http://www.bulletphysics.com/
 *
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from
 * the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose, 
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 * 1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 */

package spacegraph.phys.collision.dispatch;

import spacegraph.phys.BulletGlobals;
import spacegraph.phys.collision.broadphase.CollisionAlgorithm;
import spacegraph.phys.collision.broadphase.CollisionAlgorithmConstructionInfo;
import spacegraph.phys.collision.broadphase.DispatcherInfo;
import spacegraph.phys.collision.narrowphase.PersistentManifold;
import spacegraph.phys.collision.shapes.SphereShape;
import spacegraph.phys.linearmath.Transform;
import spacegraph.phys.util.ObjectArrayList;

import javax.vecmath.Vector3f;

/**
 * Provides collision detection between two spheres.
 * 
 * @author jezek2
 */
public class SphereSphereCollisionAlgorithm extends CollisionAlgorithm {
	
	private boolean ownManifold;
	private PersistentManifold manifoldPtr;
	
	public void init(PersistentManifold mf, CollisionAlgorithmConstructionInfo ci, Collidable col0, Collidable col1) {
		super.init(ci);
		manifoldPtr = mf;

		if (manifoldPtr == null) {
			manifoldPtr = dispatcher.getNewManifold(col0, col1);
			ownManifold = true;
		}
	}

	@Override
	public void init(CollisionAlgorithmConstructionInfo ci) {
		super.init(ci);
	}

	@Override
	public void destroy() {
		if (ownManifold) {
			if (manifoldPtr != null) {
				dispatcher.releaseManifold(manifoldPtr);
			}
			manifoldPtr = null;
		}
	}

	@Override
	public void processCollision(Collidable col0, Collidable col1, DispatcherInfo dispatchInfo, ManifoldResult resultOut) {
		if (manifoldPtr == null) {
			return;
		}

		Transform tmpTrans1 = new Transform();
		Transform tmpTrans2 = new Transform();

		resultOut.setPersistentManifold(manifoldPtr);

		SphereShape sphere0 = (SphereShape) col0.shape();
		SphereShape sphere1 = (SphereShape) col1.shape();

		Vector3f diff = new Vector3f();
		diff.sub(col0.getWorldTransform(tmpTrans1).origin, col1.getWorldTransform(tmpTrans2).origin);

		float len = diff.length();
		float radius0 = sphere0.getRadius();
		float radius1 = sphere1.getRadius();

		//#ifdef CLEAR_MANIFOLD
		//manifoldPtr.clearManifold(); // don't do this, it disables warmstarting
		//#endif

		// if distance positive, don't generate a new contact
		if (len > (radius0 + radius1)) {
			//#ifndef CLEAR_MANIFOLD
			resultOut.refreshContactPoints();
			//#endif //CLEAR_MANIFOLD
			return;
		}
		// distance (negative means penetration)
		float dist = len - (radius0 + radius1);

		Vector3f normalOnSurfaceB = new Vector3f();
		normalOnSurfaceB.set(1f, 0f, 0f);
		if (len > BulletGlobals.FLT_EPSILON) {
			normalOnSurfaceB.scale(1f / len, diff);
		}

		Vector3f tmp = new Vector3f();

		// point on A (worldspace)
		Vector3f pos0 = new Vector3f();
		tmp.scale(radius0, normalOnSurfaceB);
		pos0.sub(col0.getWorldTransform(tmpTrans1).origin, tmp);

		// point on B (worldspace)
		Vector3f pos1 = new Vector3f();
		tmp.scale(radius1, normalOnSurfaceB);
		pos1.add(col1.getWorldTransform(tmpTrans2).origin, tmp);

		// report a contact. internally this will be kept persistent, and contact reduction is done
		resultOut.addContactPoint(normalOnSurfaceB, pos1, dist);

		//#ifndef CLEAR_MANIFOLD
		resultOut.refreshContactPoints();
		//#endif //CLEAR_MANIFOLD
	}

	@Override
	public float calculateTimeOfImpact(Collidable body0, Collidable body1, DispatcherInfo dispatchInfo, ManifoldResult resultOut) {
		return 1f;
	}

	@Override
	public void getAllContactManifolds(ObjectArrayList<PersistentManifold> manifoldArray) {
		if (manifoldPtr != null && ownManifold) {
			manifoldArray.add(manifoldPtr);
		}
	}

	////////////////////////////////////////////////////////////////////////////

	public static class CreateFunc extends CollisionAlgorithmCreateFunc {
		@Override
		public CollisionAlgorithm createCollisionAlgorithm(CollisionAlgorithmConstructionInfo ci, Collidable body0, Collidable body1) {
			SphereSphereCollisionAlgorithm algo = new SphereSphereCollisionAlgorithm();
			algo.init(null, ci, body0, body1);
			return algo;
		}

		@Override
		public void releaseCollisionAlgorithm(CollisionAlgorithm algo) {

		}
	}

}
