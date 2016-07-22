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

package spacegraph.phys;

import spacegraph.phys.collision.dispatch.CollisionFlags;
import spacegraph.phys.collision.dispatch.Collidable;
import spacegraph.phys.collision.narrowphase.ManifoldPoint;

/**
 * Called when contact has been created between two collision objects. At least
 * one of object must have {@link CollisionFlags#CUSTOM_MATERIAL_CALLBACK} flag set.
 * 
 * @see BulletGlobals#setContactAddedCallback
 * @author jezek2
 */
public abstract class ContactAddedCallback {

	public abstract boolean contactAdded(ManifoldPoint cp, Collidable colObj0, int partId0, int index0, Collidable colObj1, int partId1, int index1);
	
}
