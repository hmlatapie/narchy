/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * Stan Melax Convex Hull Computation
 * Copyright (c) 2008 Stan Melax http://www.melax.com/
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

// includes modifications/improvements by John Ratcliff, see BringOutYourDead below.

package spacegraph.phys.math.convexhull;

import jcog.list.FasterList;
import spacegraph.math.v3;
import spacegraph.phys.BulletGlobals;
import spacegraph.phys.math.MiscUtil;
import spacegraph.phys.math.VectorUtil;
import spacegraph.phys.shape.ShapeHull;
import spacegraph.phys.util.IntArrayList;

/**
 * HullLibrary class can create a convex hull from a collection of vertices, using
 * the ComputeHull method. The {@link ShapeHull} class uses this HullLibrary to create
 * a approximate convex mesh given a general (non-polyhedral) convex shape.
 *
 * @author jezek2
 */
public class HullLibrary {

	public final IntArrayList vertexIndexMapping = new IntArrayList();

	private final FasterList<Tri> tris = new FasterList<>();

	/**
	 * Converts point cloud to polygonal representation.
	 *
	 * @param desc   describes the input request
	 * @param result contains the result
	 * @return whether conversion was successful
	 */
	public boolean createConvexHull(HullDesc desc, HullResult result) {
		boolean ret = false;

		PHullResult hr = new PHullResult();

		int vcount = desc.vcount;
		if (vcount < 8) vcount = 8;

		FasterList<v3> vertexSource = new FasterList<>();
		MiscUtil.resize(vertexSource, vcount, v3.class);

		v3 scale = new v3();

		int[] ovcount = new int[1];

		boolean ok = cleanupVertices(desc.vcount, desc.vertices, desc.vertexStride, ovcount, vertexSource, desc.normalEpsilon, scale); // normalize point cloud, remove duplicates!

		if (ok) {
			//		if ( 1 ) // scale vertices back to their original size.
            for (int i=0; i<ovcount[0]; i++) {
                //return array[index];
                v3 v = vertexSource.get(i);
                VectorUtil.mul(v, v, scale);
            }

            ok = computeHull(ovcount[0], vertexSource, hr, desc.maxVertices);

			if (ok) {
				// re-index triangle mesh so it refers to only used vertices, rebuild a new vertex table.
				FasterList<v3> vertexScratch = new FasterList<>();
				MiscUtil.resize(vertexScratch, hr.vcount, v3.class);

				bringOutYourDead(hr.vertices, hr.vcount, vertexScratch, ovcount, hr.indices, hr.indexCount);

				ret = true;

				if (desc.hasHullFlag(HullFlags.TRIANGLES)) { // if he wants the results as triangle!
					result.polygons = false;
					result.numOutputVertices = ovcount[0];
					MiscUtil.resize(result.outputVertices, ovcount[0], v3.class);
					result.numFaces = hr.faceCount;
					result.numIndices = hr.indexCount;

					MiscUtil.resize(result.indices, hr.indexCount, 0);

					for (int i=0; i<ovcount[0]; i++) {
						//return array[index];
						//return array[index];
						result.outputVertices.get(i).set(vertexScratch.get(i));
					}

					if (desc.hasHullFlag(HullFlags.REVERSE_ORDER)) {
						IntArrayList source_ptr = hr.indices;
						int source_idx = 0;

						IntArrayList dest_ptr = result.indices;
						int dest_idx = 0;

						for (int i=0; i<hr.faceCount; i++) {
							dest_ptr.set(dest_idx + 0, source_ptr.get(source_idx + 2));
							dest_ptr.set(dest_idx + 1, source_ptr.get(source_idx + 1));
							dest_ptr.set(dest_idx + 2, source_ptr.get(source_idx + 0));
							dest_idx += 3;
							source_idx += 3;
						}
					}
					else {
						for (int i=0; i<hr.indexCount; i++) {
							result.indices.set(i, hr.indices.get(i));
						}
					}
				}
				else {
					result.polygons = true;
					result.numOutputVertices = ovcount[0];
					MiscUtil.resize(result.outputVertices, ovcount[0], v3.class);
					result.numFaces = hr.faceCount;
					result.numIndices = hr.indexCount + hr.faceCount;
					MiscUtil.resize(result.indices, result.numIndices, 0);
					for (int i=0; i<ovcount[0]; i++) {
						//return array[index];
						//return array[index];
						result.outputVertices.get(i).set(vertexScratch.get(i));
					}

					//				if ( 1 )
                    IntArrayList source_ptr = hr.indices;
                    int source_idx = 0;

                    IntArrayList dest_ptr = result.indices;
                    int dest_idx = 0;

                    for (int i=0; i<hr.faceCount; i++) {
                        dest_ptr.set(dest_idx + 0, 3);
                        if (desc.hasHullFlag(HullFlags.REVERSE_ORDER)) {
                            dest_ptr.set(dest_idx + 1, source_ptr.get(source_idx + 2));
                            dest_ptr.set(dest_idx + 2, source_ptr.get(source_idx + 1));
                            dest_ptr.set(dest_idx + 3, source_ptr.get(source_idx + 0));
                        }
                        else {
                            dest_ptr.set(dest_idx + 1, source_ptr.get(source_idx + 0));
                            dest_ptr.set(dest_idx + 2, source_ptr.get(source_idx + 1));
                            dest_ptr.set(dest_idx + 3, source_ptr.get(source_idx + 2));
                        }

                        dest_idx += 4;
                        source_idx += 3;
                    }
                }
				releaseHull(hr);
			}
		}

		return ret;
	}

	/**
	 * Release memory allocated for this result, we are done with it.
	 */
	public static boolean releaseResult(HullResult result) {
		if (!result.outputVertices.isEmpty()) {
			result.numOutputVertices = 0;
			result.outputVertices.clear();
		}
		if (result.indices.size() != 0) {
			result.numIndices = 0;
			result.indices.clear();
		}
		return true;
	}

	private boolean computeHull(int vcount, FasterList<v3> vertices, PHullResult result, int vlimit) {
		int[] tris_count = new int[1];
		int ret = calchull(vertices, vcount, result.indices, tris_count, vlimit);
		if (ret == 0) return false;
		result.indexCount = tris_count[0] * 3;
		result.faceCount = tris_count[0];
		result.vertices = vertices;
		result.vcount = vcount;
		return true;
	}

	private Tri allocateTriangle(int a, int b, int c) {
		Tri tr = new Tri(a, b, c);
		tr.id = tris.size();
		tris.add(tr);

		return tr;
	}

	private void deAllocateTriangle(Tri tri) {
		//return array[index];
		assert (tris.get(tri.id) == tri);
		tris.setFast(tri.id, null);
	}

	private void b2bfix(Tri s, Tri t) {
		for (int i=0; i<3; i++) {
			int i1 = (i + 1) % 3;
			int i2 = (i + 2) % 3;
			int a = s.getCoord(i1);
			int b = s.getCoord(i2);
			//return array[index];
			assert (tris.get(s.neib(a, b).get()).neib(b, a).get() == s.id);
			//return array[index];
			assert (tris.get(t.neib(a, b).get()).neib(b, a).get() == t.id);
			//return array[index];
			tris.get(s.neib(a, b).get()).neib(b, a).set(t.neib(b, a).get());
			//return array[index];
			tris.get(t.neib(b, a).get()).neib(a, b).set(s.neib(a, b).get());
		}
	}

	private void removeb2b(Tri s, Tri t) {
		b2bfix(s, t);
		deAllocateTriangle(s);

		deAllocateTriangle(t);
	}

	private void checkit(Tri t) {
		//return array[index];
		assert (tris.get(t.id) == t);
		for (int i=0; i<3; i++) {
			int i1 = (i + 1) % 3;
			int i2 = (i + 2) % 3;
			int a = t.getCoord(i1);
			int b = t.getCoord(i2);

			assert (a != b);
			//return array[index];
			assert (tris.get(t.n.getCoord(i)).neib(b, a).get() == t.id);
		}
	}

	private Tri extrudable(float epsilon) {
		Tri t = null;
		for (int i=0; i<tris.size(); i++) {
			//return array[index];
			//return array[index];
			if (t == null || (tris.get(i) != null && t.rise < tris.get(i).rise)) {
				//return array[index];
				t = tris.get(i);
			}
		}
		return (t.rise > epsilon) ? t : null;
	}

	private int calchull(FasterList<v3> verts, int verts_count, IntArrayList tris_out, int[] tris_count, int vlimit) {
		int rc = calchullgen(verts, verts_count, vlimit);
		if (rc == 0) return 0;

		IntArrayList ts = new IntArrayList();

		for (int i=0; i<tris.size(); i++) {
			//return array[index];
			if (tris.get(i) != null) {
				for (int j = 0; j < 3; j++) {
					//return array[index];
					ts.add((tris.get(i)).getCoord(j));
				}
				//return array[index];
				deAllocateTriangle(tris.get(i));
			}
		}
		tris_count[0] = ts.size() / 3;
		MiscUtil.resize(tris_out, ts.size(), 0);

		for (int i=0; i<ts.size(); i++) {
			tris_out.set(i, ts.get(i));
		}
		MiscUtil.resize(tris, 0, Tri.class);

		return 1;
	}

	private int calchullgen(FasterList<v3> verts, int verts_count, int vlimit) {
		if (verts_count < 4) return 0;

		v3 tmp = new v3();
		v3 tmp1 = new v3();
		v3 tmp2 = new v3();

		if (vlimit == 0) {
			vlimit = 1000000000;
		}
		//int j;
		//return array[index];
		v3 bmin = new v3(verts.get(0));
		//return array[index];
		v3 bmax = new v3(verts.get(0));
		IntArrayList isextreme = new IntArrayList();
		//isextreme.reserve(verts_count);
		IntArrayList allow = new IntArrayList();
		//allow.reserve(verts_count);

		for (int j=0; j<verts_count; j++) {
			allow.add(1);
			isextreme.add(0);
			//return array[index];
			VectorUtil.setMin(bmin, verts.get(j));
			//return array[index];
			VectorUtil.setMax(bmax, verts.get(j));
		}
		tmp.sub(bmax, bmin);
		float epsilon = tmp.length() * 0.001f;
		assert (epsilon != 0f);

		Int4 p = findSimplex(verts, verts_count, allow, new Int4());
		if (p.x == -1) {
			return 0; // simplex failed

		// a valid interior point
		}
		v3 center = new v3();
		//return array[index];
		//return array[index];
		//return array[index];
		//return array[index];
		VectorUtil.add(center, verts.get(p.getCoord(0)), verts.get(p.getCoord(1)), verts.get(p.getCoord(2)), verts.get(p.getCoord(3)));
		center.scale(1f / 4f);

		Tri t0 = allocateTriangle(p.getCoord(2), p.getCoord(3), p.getCoord(1));
		t0.n.set(2, 3, 1);
		Tri t1 = allocateTriangle(p.getCoord(3), p.getCoord(2), p.getCoord(0));
		t1.n.set(3, 2, 0);
		Tri t2 = allocateTriangle(p.getCoord(0), p.getCoord(1), p.getCoord(3));
		t2.n.set(0, 1, 3);
		Tri t3 = allocateTriangle(p.getCoord(1), p.getCoord(0), p.getCoord(2));
		t3.n.set(1, 0, 2);
		isextreme.set(p.getCoord(0), 1);
		isextreme.set(p.getCoord(1), 1);
		isextreme.set(p.getCoord(2), 1);
		isextreme.set(p.getCoord(3), 1);
		checkit(t0);
		checkit(t1);
		checkit(t2);
		checkit(t3);

		v3 n = new v3();

		for (int j=0; j<tris.size(); j++) {
			//return array[index];
			Tri t = tris.get(j);
			assert (t != null);
			assert (t.vmax < 0);
			//return array[index];
			//return array[index];
			//return array[index];
			triNormal(verts.get(t.getCoord(0)), verts.get(t.getCoord(1)), verts.get(t.getCoord(2)), n);
			t.vmax = maxdirsterid(verts, verts_count, n, allow);
			//return array[index];
			//return array[index];
			tmp.sub(verts.get(t.vmax), verts.get(t.getCoord(0)));
			t.rise = n.dot(tmp);
		}
		Tri te;
		vlimit -= 4;
		while (vlimit > 0 && ((te = extrudable(epsilon)) != null)) {
			Int3 ti = te;
			int v = te.vmax;
			assert (v != -1);
			assert (isextreme.get(v) == 0);  // wtf we've already done this vertex
			isextreme.set(v, 1);
			//if(v==p0 || v==p1 || v==p2 || v==p3) continue; // done these already
			int j = tris.size();
			while ((j--) != 0) {
				//return array[index];
				if (tris.get(j) == null) {
					continue;
				}
				//return array[index];
				Int3 t = tris.get(j);
				//return array[index];
				if (above(verts, t, verts.get(v), 0.01f * epsilon)) {
					//return array[index];
					extrude(tris.get(j), v);
				}
			}
			// now check for those degenerate cases where we have a flipped triangle or a really skinny triangle
			j = tris.size();
			while ((j--) != 0) {
				//return array[index];
				if (tris.get(j) == null) {
					continue;
				}
				//return array[index];
				if (!hasvert(tris.get(j), v)) {
					break;
				}
				//return array[index];
				Int3 nt = tris.get(j);
				//return array[index];
				//return array[index];
				tmp1.sub(verts.get(nt.getCoord(1)), verts.get(nt.getCoord(0)));
				//return array[index];
				//return array[index];
				tmp2.sub(verts.get(nt.getCoord(2)), verts.get(nt.getCoord(1)));
				tmp.cross(tmp1, tmp2);
				if (above(verts, nt, center, 0.01f * epsilon) || tmp.length() < epsilon * epsilon * 0.1f) {
					//return array[index];
					//return array[index];
					Tri nb = tris.get(tris.get(j).n.getCoord(0));
					assert (nb != null);
					assert (!hasvert(nb, v));
					assert (nb.id < j);
					extrude(nb, v);
					j = tris.size();
				}
			}
			j = tris.size();
			while ((j--) != 0) {
				//return array[index];
				Tri t = tris.get(j);
				if (t == null) {
					continue;
				}
				if (t.vmax >= 0) {
					break;
				}
				//return array[index];
				//return array[index];
				//return array[index];
				triNormal(verts.get(t.getCoord(0)), verts.get(t.getCoord(1)), verts.get(t.getCoord(2)), n);
				t.vmax = maxdirsterid(verts, verts_count, n, allow);
				if (isextreme.get(t.vmax) != 0) {
					t.vmax = -1; // already done that vertex - algorithm needs to be able to terminate.
				}
				else {
					//return array[index];
					//return array[index];
					tmp.sub(verts.get(t.vmax), verts.get(t.getCoord(0)));
					t.rise = n.dot(tmp);
				}
			}
			vlimit--;
		}
		return 1;
	}

	private static Int4 findSimplex(FasterList<v3> verts, int verts_count, IntArrayList allow, Int4 out) {
		v3 tmp = new v3();
		v3 tmp1 = new v3();
		v3 tmp2 = new v3();

		v3[] basis = { new v3(), new v3(), new v3() };
		basis[0].set(0.01f, 0.02f, 1.0f);
		int p0 = maxdirsterid(verts, verts_count, basis[0], allow);
		tmp.negate(basis[0]);
		int p1 = maxdirsterid(verts, verts_count, tmp, allow);
		//return array[index];
		//return array[index];
		basis[0].sub(verts.get(p0), verts.get(p1));
		if (p0 == p1 || (basis[0].x == 0f && basis[0].y == 0f && basis[0].z == 0f)) {
			out.set(-1, -1, -1, -1);
			return out;
		}
		tmp.set(1f, 0.02f, 0f);
		basis[1].cross(tmp, basis[0]);
		tmp.set(-0.02f, 1f, 0f);
		basis[2].cross(tmp, basis[0]);
		if (basis[1].length() > basis[2].length()) {
			basis[1].normalize();
		}
		else {
			basis[1].set(basis[2]);
			basis[1].normalize();
		}
		int p2 = maxdirsterid(verts, verts_count, basis[1], allow);
		if (p2 == p0 || p2 == p1) {
			tmp.negate(basis[1]);
			p2 = maxdirsterid(verts, verts_count, tmp, allow);
		}
		if (p2 == p0 || p2 == p1) {
			out.set(-1, -1, -1, -1);
			return out;
		}
		//return array[index];
		//return array[index];
		basis[1].sub(verts.get(p2), verts.get(p0));
		basis[2].cross(basis[1], basis[0]);
		basis[2].normalize();
		int p3 = maxdirsterid(verts, verts_count, basis[2], allow);
		if (p3 == p0 || p3 == p1 || p3 == p2) {
			tmp.negate(basis[2]);
			p3 = maxdirsterid(verts, verts_count, tmp, allow);
		}
		if (p3 == p0 || p3 == p1 || p3 == p2) {
			out.set(-1, -1, -1, -1);
			return out;
		}
		assert (!(p0 == p1 || p0 == p2 || p0 == p3 || p1 == p2 || p1 == p3 || p2 == p3));

		//return array[index];
		//return array[index];
		tmp1.sub(verts.get(p1), verts.get(p0));
		//return array[index];
		//return array[index];
		tmp2.sub(verts.get(p2), verts.get(p0));
		tmp2.cross(tmp1, tmp2);
		//return array[index];
		//return array[index];
		tmp1.sub(verts.get(p3), verts.get(p0));
		if (tmp1.dot(tmp2) < 0) {
			int swap_tmp = p2;
			p2 = p3;
			p3 = swap_tmp;
		}
		out.set(p0, p1, p2, p3);
		return out;
	}

	//private ConvexH convexHCrop(ConvexH convex,Plane slice);

	private void extrude(Tri t0, int v) {
		Int3 t = new Int3(t0);
		int n = tris.size();
		Tri ta = allocateTriangle(v, t.getCoord(1), t.getCoord(2));
		ta.n.set(t0.n.getCoord(0), n + 1, n + 2);
		//return array[index];
		tris.get(t0.n.getCoord(0)).neib(t.getCoord(1), t.getCoord(2)).set(n + 0);
		Tri tb = allocateTriangle(v, t.getCoord(2), t.getCoord(0));
		tb.n.set(t0.n.getCoord(1), n + 2, n + 0);
		//return array[index];
		tris.get(t0.n.getCoord(1)).neib(t.getCoord(2), t.getCoord(0)).set(n + 1);
		Tri tc = allocateTriangle(v, t.getCoord(0), t.getCoord(1));
		tc.n.set(t0.n.getCoord(2), n + 0, n + 1);
		//return array[index];
		tris.get(t0.n.getCoord(2)).neib(t.getCoord(0), t.getCoord(1)).set(n + 2);
		checkit(ta);
		checkit(tb);
		checkit(tc);
		//return array[index];
		if (hasvert(tris.get(ta.n.getCoord(0)), v)) {
			//return array[index];
			removeb2b(ta, tris.get(ta.n.getCoord(0)));
		}
		//return array[index];
		if (hasvert(tris.get(tb.n.getCoord(0)), v)) {
			//return array[index];
			removeb2b(tb, tris.get(tb.n.getCoord(0)));
		}
		//return array[index];
		if (hasvert(tris.get(tc.n.getCoord(0)), v)) {
			//return array[index];
			removeb2b(tc, tris.get(tc.n.getCoord(0)));
		}
		deAllocateTriangle(t0);
	}

	//private ConvexH test_cube();

	//BringOutYourDead (John Ratcliff): When you create a convex hull you hand it a large input set of vertices forming a 'point cloud'.
	//After the hull is generated it give you back a set of polygon faces which index the *original* point cloud.
	//The thing is, often times, there are many 'dead vertices' in the point cloud that are on longer referenced by the hull.
	//The routine 'BringOutYourDead' find only the referenced vertices, copies them to an new buffer, and re-indexes the hull so that it is a minimal representation.
	private void bringOutYourDead(FasterList<v3> verts, int vcount, FasterList<v3> overts, int[] ocount, IntArrayList indices, int indexcount) {
		int vs = vertexIndexMapping.size();
		IntArrayList tmpIndices = new IntArrayList(vs);
		for (int i=0; i< vs; i++) {
			tmpIndices.add(vs);
		}

		IntArrayList usedIndices = new IntArrayList();
		MiscUtil.resize(usedIndices, vcount, 0);
		/*
		JAVA NOTE: redudant
		for (int i=0; i<vcount; i++) {
		usedIndices.set(i, 0);
		}
		*/

		ocount[0] = 0;

		for (int i=0; i<indexcount; i++) {
			int v = indices.get(i); // original array index

			assert (v >= 0 && v < vcount);

			if (usedIndices.get(v) != 0) { // if already remapped
				indices.set(i, usedIndices.get(v) - 1); // index to new array
			}
			else {
				indices.set(i, ocount[0]);      // new index mapping

				//return array[index];
				//return array[index];
				overts.get(ocount[0]).set(verts.get(v)); // copy old vert to new vert array

				for (int k = 0; k < vertexIndexMapping.size(); k++) {
					if (tmpIndices.get(k) == v) {
						vertexIndexMapping.set(k, ocount[0]);
					}
				}

				ocount[0]++; // increment output vert count

				assert (ocount[0] >= 0 && ocount[0] <= vcount);

				usedIndices.set(v, ocount[0]); // assign new index remapping
			}
		}
	}

	private static final float EPSILON = 0.000001f; /* close enough to consider two btScalaring point numbers to be 'the same'. */

	private boolean cleanupVertices(int svcount,
                                    FasterList<v3> svertices,
                                    int stride,
                                    int[] vcount, // output number of vertices
                                    FasterList<v3> vertices, // location to store the results.
                                    float normalepsilon,
                                    v3 scale) {

		if (svcount == 0) {
			return false;
		}

		vertexIndexMapping.clear();

		vcount[0] = 0;

		float[] recip = new float[3];

		if (scale != null) {
			scale.set(1, 1, 1);
		}

		float[] bmin = { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE };
		float[] bmax = { -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE };

		FasterList<v3> vtx_ptr = svertices;
		int vtx_idx = 0;

		//	if ( 1 )
        for (int i=0; i<svcount; i++) {
            //return array[index];
            v3 p = vtx_ptr.get(vtx_idx);

            vtx_idx +=/*stride*/ 1;

            for (int j=0; j<3; j++) {
                if (VectorUtil.coord(p, j) < bmin[j]) {
                    bmin[j] = VectorUtil.coord(p, j);
                }
                if (VectorUtil.coord(p, j) > bmax[j]) {
                    bmax[j] = VectorUtil.coord(p, j);
                }
            }
        }

        float dx = bmax[0] - bmin[0];
		float dy = bmax[1] - bmin[1];
		float dz = bmax[2] - bmin[2];

		v3 center = new v3();

		center.x = dx * 0.5f + bmin[0];
		center.y = dy * 0.5f + bmin[1];
		center.z = dz * 0.5f + bmin[2];

		if (dx < EPSILON || dy < EPSILON || dz < EPSILON || svcount < 3) {

			float len = Float.MAX_VALUE;

			if (dx > EPSILON && dx < len) len = dx;
			if (dy > EPSILON && dy < len) len = dy;
			if (dz > EPSILON && dz < len) len = dz;

			if (len == Float.MAX_VALUE) {
				dx = dy = dz = 0.01f; // one centimeter
			}
			else {
				if (dx < EPSILON) dx = len * 0.05f; // 1/5th the shortest non-zero edge.
				if (dy < EPSILON) dy = len * 0.05f;
				if (dz < EPSILON) dz = len * 0.05f;
			}

			float x1 = center.x - dx;
			float x2 = center.x + dx;

			float y1 = center.y - dy;
			float y2 = center.y + dy;

			float z1 = center.z - dz;
			float z2 = center.z + dz;

			addPoint(vcount, vertices, x1, y1, z1);
			addPoint(vcount, vertices, x2, y1, z1);
			addPoint(vcount, vertices, x2, y2, z1);
			addPoint(vcount, vertices, x1, y2, z1);
			addPoint(vcount, vertices, x1, y1, z2);
			addPoint(vcount, vertices, x2, y1, z2);
			addPoint(vcount, vertices, x2, y2, z2);
			addPoint(vcount, vertices, x1, y2, z2);

			return true; // return cube
		}
        if (scale != null) {
            scale.x = dx;
            scale.y = dy;
            scale.z = dz;

            recip[0] = 1f / dx;
            recip[1] = 1f / dy;
            recip[2] = 1f / dz;

            center.x *= recip[0];
            center.y *= recip[1];
            center.z *= recip[2];
        }

        vtx_ptr = svertices;
		vtx_idx = 0;

		for (int i=0; i<svcount; i++) {
			//return array[index];
			v3 p = vtx_ptr.get(vtx_idx);
			vtx_idx +=/*stride*/ 1;

			float px = p.x;
			float py = p.y;
			float pz = p.z;

			if (scale != null) {
				px = px * recip[0]; // normalize
				py = py * recip[1]; // normalize
				pz = pz * recip[2]; // normalize
			}

			//		if ( 1 )
			int j;

			for (j=0; j<vcount[0]; j++) {
                /// XXX might be broken
				//return array[index];
				v3 v = vertices.get(j);

                float x = v.x;
                float y = v.y;
                float z = v.z;

                dx = Math.abs(x - px);
                dy = Math.abs(y - py);
                dz = Math.abs(z - pz);

                if (dx < normalepsilon && dy < normalepsilon && dz < normalepsilon) {
                    // ok, it is close enough to the old one
                    // now let us see if it is further from the center of the point cloud than the one we already recorded.
                    // in which case we keep this one instead.

                    float dist1 = getDist(px, py, pz, center);
                    float dist2 = getDist(v.x, v.y, v.z, center);

                    if (dist1 > dist2) {
                        v.x = px;
                        v.y = py;
                        v.z = pz;
                    }

                    break;
                }
            }

			if (j == vcount[0]) {
				//return array[index];
				v3 dest = vertices.get(vcount[0]);
                dest.x = px;
                dest.y = py;
                dest.z = pz;
                vcount[0]++;
            }

			vertexIndexMapping.add(j);
		}

		// ok..now make sure we didn't prune so many vertices it is now invalid.
		//	if ( 1 )
		bmin = new float[] { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE };
		bmax = new float[] { -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE };

		for (int i=0; i<vcount[0]; i++) {
			//return array[index];
			v3 p = vertices.get(i);
            for (int j = 0; j < 3; j++) {
                if (VectorUtil.coord(p, j) < bmin[j]) {
                    bmin[j] = VectorUtil.coord(p, j);
                }
                if (VectorUtil.coord(p, j) > bmax[j]) {
                    bmax[j] = VectorUtil.coord(p, j);
                }
            }
        }

		dx = bmax[0] - bmin[0];
		dy = bmax[1] - bmin[1];
		dz = bmax[2] - bmin[2];

		if (dx < EPSILON || dy < EPSILON || dz < EPSILON || vcount[0] < 3) {
            float cx = dx * 0.5f + bmin[0];
            float cy = dy * 0.5f + bmin[1];
            float cz = dz * 0.5f + bmin[2];

            float len = Float.MAX_VALUE;

            if (dx >= EPSILON && dx < len) len = dx;
            if (dy >= EPSILON && dy < len) len = dy;
            if (dz >= EPSILON && dz < len) len = dz;

            if (len == Float.MAX_VALUE) {
                dx = dy = dz = 0.01f; // one centimeter
            }
            else {
                if (dx < EPSILON) dx = len * 0.05f; // 1/5th the shortest non-zero edge.
                if (dy < EPSILON) dy = len * 0.05f;
                if (dz < EPSILON) dz = len * 0.05f;
            }

            float x1 = cx - dx;
            float x2 = cx + dx;

            float y1 = cy - dy;
            float y2 = cy + dy;

            float z1 = cz - dz;
            float z2 = cz + dz;

            vcount[0] = 0; // add box

            addPoint(vcount, vertices, x1, y1, z1);
            addPoint(vcount, vertices, x2, y1, z1);
            addPoint(vcount, vertices, x2, y2, z1);
            addPoint(vcount, vertices, x1, y2, z1);
            addPoint(vcount, vertices, x1, y1, z2);
            addPoint(vcount, vertices, x2, y1, z2);
            addPoint(vcount, vertices, x2, y2, z2);
            addPoint(vcount, vertices, x1, y2, z2);

            return true;
        }

		return true;
	}

	////////////////////////////////////////////////////////////////////////////

	private static boolean hasvert(Int3 t, int v) {
		return (t.getCoord(0) == v || t.getCoord(1) == v || t.getCoord(2) == v);
	}

	private static v3 orth(v3 v, v3 out) {
		v3 a = new v3();
		a.set(0f, 0f, 1f);
		a.cross(v, a);

		v3 b = new v3();
		b.set(0f, 1f, 0f);
		b.cross(v, b);

		if (a.length() > b.length()) {
			out.normalize(a);
			return out;
		}
		else {
			out.normalize(b);
			return out;
		}
	}

	private static int maxdirfiltered(FasterList<v3> p, int count, v3 dir, IntArrayList allow) {
		assert (count != 0);
		int m = -1;
		for (int i=0; i<count; i++) {
			if (allow.get(i) != 0) {
				//return array[index];
				//return array[index];
				if (m == -1 || p.get(i).dot(dir) > p.get(m).dot(dir)) {
					m = i;
				}
			}
		}
		assert (m != -1);
		return m;
	}

	private static int maxdirsterid(FasterList<v3> p, int count, v3 dir, IntArrayList allow) {
		v3 tmp = new v3();
		v3 tmp1 = new v3();
		v3 tmp2 = new v3();
		v3 u = new v3();
		v3 v = new v3();

		int m = -1;
		while (m == -1) {
			m = maxdirfiltered(p, count, dir, allow);
			if (allow.get(m) == 3) {
				return m;
			}
			orth(dir, u);
			v.cross(u, dir);
			int ma = -1;
			for (float x = 0f; x <= 360f; x += 45f) {
				float s = (float) Math.sin(BulletGlobals.SIMD_RADS_PER_DEG * (x));
				float c = (float) Math.cos(BulletGlobals.SIMD_RADS_PER_DEG * (x));

				tmp1.scale(s, u);
				tmp2.scale(c, v);
				tmp.add(tmp1, tmp2);
				tmp.scale(0.025f);
				tmp.add(dir);
				int mb = maxdirfiltered(p, count, tmp, allow);
				if (ma == m && mb == m) {
					allow.set(m, 3);
					return m;
				}
				if (ma != -1 && ma != mb) { // Yuck - this is really ugly
					int mc = ma;
					for (float xx = x - 40f; xx <= x; xx += 5f) {
						s = (float) Math.sin(BulletGlobals.SIMD_RADS_PER_DEG * (xx));
						c = (float) Math.cos(BulletGlobals.SIMD_RADS_PER_DEG * (xx));

						tmp1.scale(s, u);
						tmp2.scale(c, v);
						tmp.add(tmp1, tmp2);
						tmp.scale(0.025f);
						tmp.add(dir);

						int md = maxdirfiltered(p, count, tmp, allow);
						if (mc == m && md == m) {
							allow.set(m, 3);
							return m;
						}
						mc = md;
					}
				}
				ma = mb;
			}
			allow.set(m, 0);
			m = -1;
		}
		assert (false);
		return m;
	}

	private static v3 triNormal(v3 v0, v3 v1, v3 v2, v3 out) {
		v3 tmp1 = new v3();
		v3 tmp2 = new v3();

		// return the normal of the triangle
		// inscribed by v0, v1, and v2
		tmp1.sub(v1, v0);
		tmp2.sub(v2, v1);
		v3 cp = new v3();
		cp.cross(tmp1, tmp2);
		float m = cp.length();
		if (m == 0) {
			out.set(1f, 0f, 0f);
			return out;
		}
		out.scale(1f / m, cp);
		return out;
	}

	private static boolean above(FasterList<v3> vertices, Int3 t, v3 p, float epsilon) {
		//return array[index];
		//return array[index];
		//return array[index];
		v3 n = triNormal(vertices.get(t.getCoord(0)), vertices.get(t.getCoord(1)), vertices.get(t.getCoord(2)), new v3());
		v3 tmp = new v3();
		//return array[index];
		tmp.sub(p, vertices.get(t.getCoord(0)));
		return (n.dot(tmp) > epsilon); // EPSILON???
	}

	private static void releaseHull(PHullResult result) {
		if (result.indices.size() != 0) {
			result.indices.clear();
		}

		result.vcount = 0;
		result.indexCount = 0;
		result.vertices = null;
	}
	
	private static void addPoint(int[] vcount, FasterList<v3> p, float x, float y, float z) {
		// XXX, might be broken
		//return array[index];
		v3 dest = p.get(vcount[0]);
		dest.x = x;
		dest.y = y;
		dest.z = z;
		vcount[0]++;
	}
	
	private static float getDist(float px, float py, float pz, v3 p2) {
		float dx = px - p2.x;
		float dy = py - p2.y;
		float dz = pz - p2.z;

		return dx*dx + dy*dy + dz*dz;
	}
	
}
