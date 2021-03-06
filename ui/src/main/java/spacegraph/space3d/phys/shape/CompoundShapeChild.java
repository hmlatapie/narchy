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

package spacegraph.space3d.phys.shape;

import spacegraph.space3d.phys.math.Transform;

/**
 * Compound shape child.
 * 
 * @author jezek2
 */
public final class CompoundShapeChild {
	
	public final Transform transform = new Transform();
	public final CollisionShape childShape;
	//public final BroadphaseNativeType childShapeType;
	//public float childMargin;

	public CompoundShapeChild(CollisionShape childShape) {
		this.childShape = childShape;
		//this.childShapeType = childShapeType;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof CompoundShapeChild)) return false;
		CompoundShapeChild child = (CompoundShapeChild)obj;
		return transform.equals(child.transform) &&
		       childShape == child.childShape
				//&&
		       //childShape.getShapeType() == child.childShape.getShapeType()  &&
		       //childMargin == child.childMargin
		;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 19 * hash + transform.hashCode();
		hash = 19 * hash + childShape.hashCode();
		hash = 19 * hash + childShape.getShapeType().hashCode();
		hash = 19 * hash + Float.floatToIntBits(childShape.getMargin());
		return hash;
	}

}
