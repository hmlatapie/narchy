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

import spacegraph.phys.collision.broadphase.CollisionAlgorithm;
import spacegraph.phys.collision.broadphase.CollisionAlgorithmConstructionInfo;
import spacegraph.phys.collision.broadphase.DispatcherInfo;
import spacegraph.phys.collision.narrowphase.PersistentManifold;
import spacegraph.phys.collision.shapes.ConvexShape;
import spacegraph.phys.collision.shapes.StaticPlaneShape;
import spacegraph.phys.linearmath.Transform;
import spacegraph.phys.util.ObjectArrayList;

import javax.vecmath.Vector3f;

/**
 * ConvexPlaneCollisionAlgorithm provides convex/plane collision detection.
 * 
 * @author jezek2
 */
public class ConvexPlaneCollisionAlgorithm extends CollisionAlgorithm {

	private boolean ownManifold;
	private PersistentManifold manifoldPtr;
	private boolean isSwapped;
	
	public void init(PersistentManifold mf, CollisionAlgorithmConstructionInfo ci, Collidable col0, Collidable col1, boolean isSwapped) {
		super.init(ci);
		this.ownManifold = false;
		this.manifoldPtr = mf;
		this.isSwapped = isSwapped;

		Collidable convexObj = isSwapped ? col1 : col0;
		Collidable planeObj = isSwapped ? col0 : col1;

		if (manifoldPtr == null && dispatcher.needsCollision(convexObj, planeObj)) {
			manifoldPtr = dispatcher.getNewManifold(convexObj, planeObj);
			ownManifold = true;
		}
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
	public void processCollision(Collidable body0, Collidable body1, DispatcherInfo dispatchInfo, ManifoldResult resultOut) {
		if (manifoldPtr == null) {
			return;
		}

		Transform tmpTrans = new Transform();

		Collidable convexObj = isSwapped ? body1 : body0;
		Collidable planeObj = isSwapped ? body0 : body1;

		ConvexShape convexShape = (ConvexShape) convexObj.shape();
		StaticPlaneShape planeShape = (StaticPlaneShape) planeObj.shape();

		boolean hasCollision = false;
		Vector3f planeNormal = planeShape.getPlaneNormal(new Vector3f());
		float planeConstant = planeShape.getPlaneConstant();

		Transform planeInConvex = new Transform();
		convexObj.getWorldTransform(planeInConvex);
		planeInConvex.inverse();
		planeInConvex.mul(planeObj.getWorldTransform(tmpTrans));

		Transform convexInPlaneTrans = new Transform();
		convexInPlaneTrans.inverse(planeObj.getWorldTransform(tmpTrans));
		convexInPlaneTrans.mul(convexObj.getWorldTransform(tmpTrans));

		Vector3f tmp = new Vector3f();
		tmp.negate(planeNormal);
		planeInConvex.basis.transform(tmp);

		Vector3f vtx = convexShape.localGetSupportingVertex(tmp, new Vector3f());
		Vector3f vtxInPlane = new Vector3f(vtx);
		convexInPlaneTrans.transform(vtxInPlane);

		float distance = (planeNormal.dot(vtxInPlane) - planeConstant);

		Vector3f vtxInPlaneProjected = new Vector3f();
		tmp.scale(distance, planeNormal);
		vtxInPlaneProjected.sub(vtxInPlane, tmp);

		Vector3f vtxInPlaneWorld = new Vector3f(vtxInPlaneProjected);
		planeObj.getWorldTransform(tmpTrans).transform(vtxInPlaneWorld);

		hasCollision = distance < PersistentManifold.getContactBreakingThreshold();
		resultOut.setPersistentManifold(manifoldPtr);
		if (hasCollision) {
			// report a contact. internally this will be kept persistent, and contact reduction is done
			Vector3f normalOnSurfaceB = new Vector3f(planeNormal);
			planeObj.getWorldTransform(tmpTrans).basis.transform(normalOnSurfaceB);

			Vector3f pOnB = new Vector3f(vtxInPlaneWorld);
			resultOut.addContactPoint(normalOnSurfaceB, pOnB, distance);
		}
		if (ownManifold) {
			if (manifoldPtr.getNumContacts() != 0) {
				resultOut.refreshContactPoints();
			}
		}
	}

	@Override
	public float calculateTimeOfImpact(Collidable body0, Collidable body1, DispatcherInfo dispatchInfo, ManifoldResult resultOut) {
		// not yet
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
			ConvexPlaneCollisionAlgorithm algo = new ConvexPlaneCollisionAlgorithm();
			if (!swapped) {
				algo.init(null, ci, body0, body1, false);
			}
			else {
				algo.init(null, ci, body0, body1, true);
			}
			return algo;
		}

		@Override
		public void releaseCollisionAlgorithm(CollisionAlgorithm algo) {

		}
	}
	
}
