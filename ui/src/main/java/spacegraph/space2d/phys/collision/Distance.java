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
package spacegraph.space2d.phys.collision;

import spacegraph.space2d.phys.collision.shapes.*;
import spacegraph.space2d.phys.common.*;
import spacegraph.util.math.Tuple2f;
import spacegraph.util.math.v2;

// updated to rev 100

/**
 * This is non-static for faster pooling. To get an instance, use the {@link SingletonPool}, don't
 * construct a distance object.
 *
 * @author Daniel Murphy
 */
public class Distance {
    public static final int MAX_ITERS = 20;

    public static int GJK_CALLS = 0;
    public static int GJK_ITERS = 0;
    public static int GJK_MAX_ITERS = 20;

    /**
     * GJK using Voronoi regions (Christer Ericson) and Barycentric coordinates.
     */
    private static class SimplexVertex {
        public final Tuple2f wA = new v2(); // support point in shapeA
        public final Tuple2f wB = new v2(); // support point in shapeB
        public final Tuple2f w = new v2(); // wB - wA
        public float a; // barycentric coordinate for closest point
        public int indexA; // wA index
        public int indexB; // wB index

        public void set(SimplexVertex sv) {
            wA.set(sv.wA);
            wB.set(sv.wB);
            w.set(sv.w);
            a = sv.a;
            indexA = sv.indexA;
            indexB = sv.indexB;
        }
    }

    /**
     * Used to warm start Distance. Set count to zero on first call.
     *
     * @author daniel
     */
    public static class SimplexCache {
        /**
         * length or area
         */
        public float metric;
        public int count;
        /**
         * vertices on shape A
         */
        public final int indexA[] = new int[3];
        /**
         * vertices on shape B
         */
        public final int indexB[] = new int[3];

        public SimplexCache() {
            metric = 0;
            count = 0;
            indexA[0] = Integer.MAX_VALUE;
            indexA[1] = Integer.MAX_VALUE;
            indexA[2] = Integer.MAX_VALUE;
            indexB[0] = Integer.MAX_VALUE;
            indexB[1] = Integer.MAX_VALUE;
            indexB[2] = Integer.MAX_VALUE;
        }

        public void set(SimplexCache sc) {
            System.arraycopy(sc.indexA, 0, indexA, 0, indexA.length);
            System.arraycopy(sc.indexB, 0, indexB, 0, indexB.length);
            metric = sc.metric;
            count = sc.count;
        }
    }

    private static class Simplex {
        public final SimplexVertex m_v1 = new SimplexVertex();
        public final SimplexVertex m_v2 = new SimplexVertex();
        public final SimplexVertex m_v3 = new SimplexVertex();
        public final SimplexVertex vertices[] = {m_v1, m_v2, m_v3};
        public int m_count;

        public void readCache(SimplexCache cache, DistanceProxy proxyA, Transform transformA,
                              DistanceProxy proxyB, Transform transformB) {
            assert (cache.count <= 3);

            // Copy data from cache.
            m_count = cache.count;

            for (int i = 0; i < m_count; ++i) {
                SimplexVertex v = vertices[i];
                v.indexA = cache.indexA[i];
                v.indexB = cache.indexB[i];
                Tuple2f wALocal = proxyA.vertex(v.indexA);
                Tuple2f wBLocal = proxyB.vertex(v.indexB);
                Transform.mulToOutUnsafe(transformA, wALocal, v.wA);
                Transform.mulToOutUnsafe(transformB, wBLocal, v.wB);
                v.w.set(v.wB).subbed(v.wA);
                v.a = 0.0f;
            }

            // Compute the new simplex metric, if it is substantially different than
            // old metric then flush the simplex.
            if (m_count > 1) {
                float metric1 = cache.metric;
                float metric2 = getMetric();
                if (metric2 < 0.5f * metric1 || 2.0f * metric1 < metric2 || metric2 < Settings.EPSILON) {
                    // Reset the simplex.
                    m_count = 0;
                }
            }

            // If the cache is empty or invalid ...
            if (m_count == 0) {
                SimplexVertex v = vertices[0];
                v.indexA = 0;
                v.indexB = 0;
                Tuple2f wALocal = proxyA.vertex(0);
                Tuple2f wBLocal = proxyB.vertex(0);
                Transform.mulToOutUnsafe(transformA, wALocal, v.wA);
                Transform.mulToOutUnsafe(transformB, wBLocal, v.wB);
                v.w.set(v.wB).subbed(v.wA);
                m_count = 1;
            }
        }

        public void writeCache(SimplexCache cache) {
            cache.metric = getMetric();
            cache.count = m_count;

            for (int i = 0; i < m_count; ++i) {
                SimplexVertex vi = vertices[i];
                cache.indexA[i] = vi.indexA;
                cache.indexB[i] = vi.indexB;
            }
        }

        private final Tuple2f e12 = new v2();

        public final void getSearchDirection(final Tuple2f out) {
            switch (m_count) {
                case 1:
                    out.set(m_v1.w).negated();
                    return;
                case 2:
                    e12.set(m_v2.w).subbed(m_v1.w);
                    // use out for a temp variable real quick
                    out.set(m_v1.w).negated();
                    float sgn = Tuple2f.cross(e12, out);

                    if (sgn > 0f) {
                        // Origin is left of e12.
                        Tuple2f.crossToOutUnsafe(1f, e12, out);
                        return;
                    } else {
                        // Origin is right of e12.
                        Tuple2f.crossToOutUnsafe(e12, 1f, out);
                        return;
                    }
                default:
                    assert (false);
                    out.setZero();
            }
        }

        // djm pooled
        private final Tuple2f case2 = new v2();
        private final Tuple2f case22 = new v2();

        /**
         * this returns pooled objects. don't keep or modify them
         *
         * @return
         */
        public void getClosestPoint(final Tuple2f out) {
            switch (m_count) {
                case 0:
                    assert (false);
                    out.setZero();
                    return;
                case 1:
                    out.set(m_v1.w);
                    return;
                case 2:
                    case22.set(m_v2.w).scaled(m_v2.a);
                    case2.set(m_v1.w).scaled(m_v1.a).added(case22);
                    out.set(case2);
                    return;
                case 3:
                    out.setZero();
                    return;
                default:
                    assert (false);
                    out.setZero();
            }
        }

        // djm pooled, and from above
        private final Tuple2f case3 = new v2();
        private final Tuple2f case33 = new v2();

        public void getWitnessPoints(Vec2 pA, Tuple2f pB) {
            switch (m_count) {
                case 0:
                    assert (false);
                    break;

                case 1:
                    pA.set(m_v1.wA);
                    pB.set(m_v1.wB);
                    break;

                case 2:
                    case2.set(m_v1.wA).scaled(m_v1.a);
                    pA.set(m_v2.wA).scaled(m_v2.a).added(case2);
                    // m_v1.a * m_v1.wA + m_v2.a * m_v2.wA;
                    // *pB = m_v1.a * m_v1.wB + m_v2.a * m_v2.wB;
                    case2.set(m_v1.wB).scaled(m_v1.a);
                    pB.set(m_v2.wB).scaled(m_v2.a).added(case2);

                    break;

                case 3:
                    pA.set(m_v1.wA).scaled(m_v1.a);
                    case3.set(m_v2.wA).scaled(m_v2.a);
                    case33.set(m_v3.wA).scaled(m_v3.a);
                    pA.addLocal(case3).addLocal(case33);
                    pB.set(pA);
                    // *pA = m_v1.a * m_v1.wA + m_v2.a * m_v2.wA + m_v3.a * m_v3.wA;
                    // *pB = *pA;
                    break;

                default:
                    assert (false);
                    break;
            }
        }

        // djm pooled, from above
        public float getMetric() {
            switch (m_count) {
                case 0:
                    assert (false);
                    return 0.0f;

                case 1:
                    return 0.0f;

                case 2:
                    return MathUtils.distance(m_v1.w, m_v2.w);

                case 3:
                    case3.set(m_v2.w).subbed(m_v1.w);
                    case33.set(m_v3.w).subbed(m_v1.w);
                    // return Vec2.cross(m_v2.w - m_v1.w, m_v3.w - m_v1.w);
                    return Tuple2f.cross(case3, case33);

                default:
                    assert (false);
                    return 0.0f;
            }
        }

        // djm pooled from above

        /**
         * Solve a line segment using barycentric coordinates.
         */
        public void solve2() {
            // Solve a line segment using barycentric coordinates.
            //
            // p = a1 * w1 + a2 * w2
            // a1 + a2 = 1
            //
            // The vector from the origin to the closest point on the line is
            // perpendicular to the line.
            // e12 = w2 - w1
            // dot(p, e) = 0
            // a1 * dot(w1, e) + a2 * dot(w2, e) = 0
            //
            // 2-by-2 linear system
            // [1 1 ][a1] = [1]
            // [w1.e12 w2.e12][a2] = [0]
            //
            // Define
            // d12_1 = dot(w2, e12)
            // d12_2 = -dot(w1, e12)
            // d12 = d12_1 + d12_2
            //
            // Solution
            // a1 = d12_1 / d12
            // a2 = d12_2 / d12
            final Tuple2f w1 = m_v1.w;
            final Tuple2f w2 = m_v2.w;
            e12.set(w2).subbed(w1);

            // w1 region
            float d12_2 = -Tuple2f.dot(w1, e12);
            if (d12_2 <= 0.0f) {
                // a2 <= 0, so we clamp it to 0
                m_v1.a = 1.0f;
                m_count = 1;
                return;
            }

            // w2 region
            float d12_1 = Tuple2f.dot(w2, e12);
            if (d12_1 <= 0.0f) {
                // a1 <= 0, so we clamp it to 0
                m_v2.a = 1.0f;
                m_count = 1;
                m_v1.set(m_v2);
                return;
            }

            // Must be in e12 region.
            float inv_d12 = 1.0f / (d12_1 + d12_2);
            m_v1.a = d12_1 * inv_d12;
            m_v2.a = d12_2 * inv_d12;
            m_count = 2;
        }

        // djm pooled, and from above
        private final Tuple2f e13 = new v2();
        private final Tuple2f e23 = new v2();
        private final Tuple2f w1 = new v2();
        private final Tuple2f w2 = new v2();
        private final Tuple2f w3 = new v2();

        /**
         * Solve a line segment using barycentric coordinates.<br/>
         * Possible regions:<br/>
         * - points[2]<br/>
         * - edge points[0]-points[2]<br/>
         * - edge points[1]-points[2]<br/>
         * - inside the triangle
         */
        public void solve3() {
            w1.set(m_v1.w);
            w2.set(m_v2.w);
            w3.set(m_v3.w);

            // Edge12
            // [1 1 ][a1] = [1]
            // [w1.e12 w2.e12][a2] = [0]
            // a3 = 0
            e12.set(w2).subbed(w1);
            float w1e12 = Tuple2f.dot(w1, e12);
            float w2e12 = Tuple2f.dot(w2, e12);
            float d12_1 = w2e12;
            float d12_2 = -w1e12;

            // Edge13
            // [1 1 ][a1] = [1]
            // [w1.e13 w3.e13][a3] = [0]
            // a2 = 0
            e13.set(w3).subbed(w1);
            float w1e13 = Tuple2f.dot(w1, e13);
            float w3e13 = Tuple2f.dot(w3, e13);
            float d13_1 = w3e13;
            float d13_2 = -w1e13;

            // Edge23
            // [1 1 ][a2] = [1]
            // [w2.e23 w3.e23][a3] = [0]
            // a1 = 0
            e23.set(w3).subbed(w2);
            float w2e23 = Tuple2f.dot(w2, e23);
            float w3e23 = Tuple2f.dot(w3, e23);
            float d23_1 = w3e23;
            float d23_2 = -w2e23;

            // Triangle123
            float n123 = Tuple2f.cross(e12, e13);

            float d123_1 = n123 * Tuple2f.cross(w2, w3);
            float d123_2 = n123 * Tuple2f.cross(w3, w1);
            float d123_3 = n123 * Tuple2f.cross(w1, w2);

            // w1 region
            if (d12_2 <= 0.0f && d13_2 <= 0.0f) {
                m_v1.a = 1.0f;
                m_count = 1;
                return;
            }

            // e12
            if (d12_1 > 0.0f && d12_2 > 0.0f && d123_3 <= 0.0f) {
                float inv_d12 = 1.0f / (d12_1 + d12_2);
                m_v1.a = d12_1 * inv_d12;
                m_v2.a = d12_2 * inv_d12;
                m_count = 2;
                return;
            }

            // e13
            if (d13_1 > 0.0f && d13_2 > 0.0f && d123_2 <= 0.0f) {
                float inv_d13 = 1.0f / (d13_1 + d13_2);
                m_v1.a = d13_1 * inv_d13;
                m_v3.a = d13_2 * inv_d13;
                m_count = 2;
                m_v2.set(m_v3);
                return;
            }

            // w2 region
            if (d12_1 <= 0.0f && d23_2 <= 0.0f) {
                m_v2.a = 1.0f;
                m_count = 1;
                m_v1.set(m_v2);
                return;
            }

            // w3 region
            if (d13_1 <= 0.0f && d23_1 <= 0.0f) {
                m_v3.a = 1.0f;
                m_count = 1;
                m_v1.set(m_v3);
                return;
            }

            // e23
            if (d23_1 > 0.0f && d23_2 > 0.0f && d123_1 <= 0.0f) {
                float inv_d23 = 1.0f / (d23_1 + d23_2);
                m_v2.a = d23_1 * inv_d23;
                m_v3.a = d23_2 * inv_d23;
                m_count = 2;
                m_v1.set(m_v3);
                return;
            }

            // Must be in triangle123
            float inv_d123 = 1.0f / (d123_1 + d123_2 + d123_3);
            m_v1.a = d123_1 * inv_d123;
            m_v2.a = d123_2 * inv_d123;
            m_v3.a = d123_3 * inv_d123;
            m_count = 3;
        }
    }

    /**
     * A distance proxy is used by the GJK algorithm. It encapsulates any shape. TODO: see if we can
     * just do assignments with m_vertices, instead of copying stuff over
     *
     * @author daniel
     */
    public static class DistanceProxy {
        public final Tuple2f[] m_vertices;
        public int m_count;
        public float m_radius;
        public final Tuple2f[] m_buffer;

        public DistanceProxy() {
            m_vertices = new Tuple2f[Settings.maxPolygonVertices];
            for (int i = 0; i < m_vertices.length; i++) {
                m_vertices[i] = new v2();
            }
            m_buffer = new Tuple2f[2];
            m_count = 0;
            m_radius = 0f;
        }

        /**
         * Initialize the proxy using the given shape. The shape must remain in scope while the proxy is
         * in use.
         */
        public final void set(final Shape shape, int index) {
            switch (shape.getType()) {
                case CIRCLE:
                    final CircleShape circle = (CircleShape) shape;
                    m_vertices[0].set(circle.center);
                    m_count = 1;
                    m_radius = circle.radius;

                    break;
                case POLYGON:
                    final PolygonShape poly = (PolygonShape) shape;
                    m_count = poly.vertices;
                    m_radius = poly.radius;
                    for (int i = 0; i < m_count; i++) {
                        m_vertices[i].set(poly.vertex[i]);
                    }
                    break;
                case CHAIN:
                    final ChainShape chain = (ChainShape) shape;
                    assert (0 <= index && index < chain.m_count);

                    m_buffer[0] = chain.m_vertices[index];
                    if (index + 1 < chain.m_count) {
                        m_buffer[1] = chain.m_vertices[index + 1];
                    } else {
                        m_buffer[1] = chain.m_vertices[0];
                    }

                    m_vertices[0].set(m_buffer[0]);
                    m_vertices[1].set(m_buffer[1]);
                    m_count = 2;
                    m_radius = chain.radius;
                    break;
                case EDGE:
                    EdgeShape edge = (EdgeShape) shape;
                    m_vertices[0].set(edge.m_vertex1);
                    m_vertices[1].set(edge.m_vertex2);
                    m_count = 2;
                    m_radius = edge.radius;
                    break;
                default:
                    assert (false);
            }
        }

        /**
         * Get the supporting vertex index in the given direction.
         *
         * @param d
         * @return
         */
        public final int getSupport(final Tuple2f d) {
            int bestIndex = 0;
            float bestValue = Tuple2f.dot(m_vertices[0], d);
            for (int i = 1; i < m_count; i++) {
                float value = Tuple2f.dot(m_vertices[i], d);
                if (value > bestValue) {
                    bestIndex = i;
                    bestValue = value;
                }
            }

            return bestIndex;
        }

        /**
         * Get the supporting vertex in the given direction.
         *
         * @param d
         * @return
         */
        public final Tuple2f getSupportVertex(final Tuple2f d) {
            int bestIndex = 0;
            float bestValue = Tuple2f.dot(m_vertices[0], d);
            for (int i = 1; i < m_count; i++) {
                float value = Tuple2f.dot(m_vertices[i], d);
                if (value > bestValue) {
                    bestIndex = i;
                    bestValue = value;
                }
            }

            return m_vertices[bestIndex];
        }

        /**
         * Get the vertex count.
         *
         * @return
         */
        public final int getVertexCount() {
            return m_count;
        }

        /**
         * Get a vertex by index. Used by Distance.
         *
         * @param index
         * @return
         */
        public final Tuple2f vertex(int index) {
            assert (0 <= index && index < m_count);
            return m_vertices[index];
        }
    }

    private final Simplex simplex = new Simplex();
    private final int[] saveA = new int[3];
    private final int[] saveB = new int[3];
    private final Tuple2f closestPoint = new v2();
    private final v2 d = new v2();
    private final Tuple2f temp = new v2();
    private final v2 normal = new v2();

    public final void distance(final DistanceOutput output, final DistanceInput input) {
        distance(output, new SimplexCache(), input);
    }

    /**
     * Compute the closest points between two shapes. Supports any combination of: CircleShape and
     * PolygonShape. The simplex cache is input/output. On the first call set SimplexCache.count to
     * zero.
     *
     * @param output
     * @param cache
     * @param input
     */
    public final void distance(final DistanceOutput output, final SimplexCache cache,
                               final DistanceInput input) {
        GJK_CALLS++;

        final DistanceProxy proxyA = input.proxyA;
        final DistanceProxy proxyB = input.proxyB;

        Transform transformA = input.transformA;
        Transform transformB = input.transformB;

        // Initialize the simplex.
        simplex.readCache(cache, proxyA, transformA, proxyB, transformB);

        // Get simplex vertices as an array.
        SimplexVertex[] vertices = simplex.vertices;

        // These store the vertices of the last simplex so that we
        // can check for duplicates and prevent cycling.
        // (pooled above)
        int saveCount = 0;

        simplex.getClosestPoint(closestPoint);
        float distanceSqr1 = closestPoint.lengthSquared();
        float distanceSqr2 = distanceSqr1;

        // Main iteration loop
        int iter = 0;
        while (iter < MAX_ITERS) {

            // Copy simplex so we can identify duplicates.
            saveCount = simplex.m_count;
            for (int i = 0; i < saveCount; i++) {
                saveA[i] = vertices[i].indexA;
                saveB[i] = vertices[i].indexB;
            }

            switch (simplex.m_count) {
                case 1:
                    break;
                case 2:
                    simplex.solve2();
                    break;
                case 3:
                    simplex.solve3();
                    break;
                default:
                    assert (false);
            }

            // If we have 3 points, then the origin is in the corresponding triangle.
            if (simplex.m_count == 3) {
                break;
            }

            // Compute closest point.
            simplex.getClosestPoint(closestPoint);
            distanceSqr2 = closestPoint.lengthSquared();

            // ensure progress
            if (distanceSqr2 >= distanceSqr1) {
                // break;
            }
            distanceSqr1 = distanceSqr2;

            // get search direction;
            simplex.getSearchDirection(d);

            // Ensure the search direction is numerically fit.
            if (d.lengthSquared() < Settings.EPSILON * Settings.EPSILON) {
                // The origin is probably contained by a line segment
                // or triangle. Thus the shapes are overlapped.

                // We can't return zero here even though there may be overlap.
                // In case the simplex is a point, segment, or triangle it is difficult
                // to determine if the origin is contained in the CSO or very close to it.
                break;
            }
            /*
             * SimplexVertex* vertex = vertices + simplex.m_count; vertex.indexA =
             * proxyA.GetSupport(MulT(transformA.R, -d)); vertex.wA = Mul(transformA,
             * proxyA.GetVertex(vertex.indexA)); Vec2 wBLocal; vertex.indexB =
             * proxyB.GetSupport(MulT(transformB.R, d)); vertex.wB = Mul(transformB,
             * proxyB.GetVertex(vertex.indexB)); vertex.w = vertex.wB - vertex.wA;
             */

            // Compute a tentative new simplex vertex using support points.
            SimplexVertex vertex = vertices[simplex.m_count];

            Rot.mulTransUnsafe(transformA, d.negated(), temp);
            vertex.indexA = proxyA.getSupport(temp);
            Transform.mulToOutUnsafe(transformA, proxyA.vertex(vertex.indexA), vertex.wA);
            // Vec2 wBLocal;
            Rot.mulTransUnsafe(transformB, d.negated(), temp);
            vertex.indexB = proxyB.getSupport(temp);
            Transform.mulToOutUnsafe(transformB, proxyB.vertex(vertex.indexB), vertex.wB);
            vertex.w.set(vertex.wB).subbed(vertex.wA);

            // Iteration count is equated to the number of support point calls.
            ++iter;
            ++GJK_ITERS;

            // Check for duplicate support points. This is the main termination criteria.
            boolean duplicate = false;
            for (int i = 0; i < saveCount; ++i) {
                if (vertex.indexA == saveA[i] && vertex.indexB == saveB[i]) {
                    duplicate = true;
                    break;
                }
            }

            // If we found a duplicate support point we must exit to avoid cycling.
            if (duplicate) {
                break;
            }

            // New vertex is ok and needed.
            ++simplex.m_count;
        }

        GJK_MAX_ITERS = MathUtils.max(GJK_MAX_ITERS, iter);

        // Prepare output.
        simplex.getWitnessPoints(output.pointA, output.pointB);
        output.distance = MathUtils.distance(output.pointA, output.pointB);
        output.iterations = iter;

        // Cache the simplex.
        simplex.writeCache(cache);

        // Apply radii if requested.
        if (input.useRadii) {
            float rA = proxyA.m_radius;
            float rB = proxyB.m_radius;

            if (output.distance > rA + rB && output.distance > Settings.EPSILON) {
                // Shapes are still no overlapped.
                // Move the witness points to the outer surface.
                output.distance -= rA + rB;
                normal.set(output.pointB).subbed(output.pointA);
                normal.normalize();
                temp.set(normal).scaled(rA);
                output.pointA.addLocal(temp);
                temp.set(normal).scaled(rB);
                output.pointB.subLocal(temp);
            } else {
                // Shapes are overlapped when radii are considered.
                // Move the witness points to the middle.
                // Vec2 p = 0.5f * (output.pointA + output.pointB);
                output.pointA.addLocal(output.pointB).scaled(.5f);
                output.pointB.set(output.pointA);
                output.distance = 0.0f;
            }
        }
    }
}
