/*
 * gleem -- OpenGL Extremely Easy-To-Use Manipulators.
 * Copyright (C) 1998-2003 Kenneth B. Russell (kbrussel@alum.mit.edu)
 *
 * Copying, distribution and use of this software in source and binary
 * forms, with or without modification, is permitted provided that the
 * following conditions are met:
 *
 * Distributions of source code must reproduce the copyright notice,
 * this list of conditions and the following disclaimer in the source
 * code header files; and Distributions of binary code must reproduce
 * the copyright notice, this list of conditions and the following
 * disclaimer in the documentation, Read me file, license file and/or
 * other materials provided with the software distribution.
 *
 * The names of Sun Microsystems, Inc. ("Sun") and/or the copyright
 * holder may not be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS," WITHOUT A WARRANTY OF ANY
 * KIND. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, NON-INTERFERENCE, ACCURACY OF
 * INFORMATIONAL CONTENT OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. THE
 * COPYRIGHT HOLDER, SUN AND SUN'S LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL THE
 * COPYRIGHT HOLDER, SUN OR SUN'S LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGES. YOU ACKNOWLEDGE THAT THIS SOFTWARE IS NOT
 * DESIGNED, LICENSED OR INTENDED FOR USE IN THE DESIGN, CONSTRUCTION,
 * OPERATION OR MAINTENANCE OF ANY NUCLEAR FACILITY. THE COPYRIGHT
 * HOLDER, SUN AND SUN'S LICENSORS DISCLAIM ANY EXPRESS OR IMPLIED
 * WARRANTY OF FITNESS FOR SUCH USES.
 */

package nars.gui.test;

import com.jogamp.newt.opengl.GLWindow;
import gleem.*;
import gleem.linalg.Vec3f;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.glu.GLU;
import nars.util.JoglSpace;


/**
 * Tests the Examiner Viewer.
 */

public class TestExaminerViewer extends JoglSpace {
    private static final int X_SIZE = 400;
    private static final int Y_SIZE = 400;
    private ManipManager manip;


    static class HandleBoxManipBSphereProvider implements BSphereProvider {
        private final HandleBoxManip manip;

        final BSphere bsph = new BSphere();

        private HandleBoxManipBSphereProvider(HandleBoxManip manip) {
            this.manip = manip;
        }

        @Override
        public BSphere getBoundingSphere() {
            return bsph.setCenter(manip.getTranslation()).setRadius(manip.getRadius());
        }
    }


    private static final GLU glu = new GLU();
    private final CameraParameters params = new CameraParameters();
    private ExaminerViewer viewer;

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glClearColor(0, 0, 0, 0);
        float[] lightPosition = new float[]{1, 1, 1, 0};
        float[] ambient = new float[]{0.0f, 0.0f, 0.0f, 1.0f};
        float[] diffuse = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
        gl.glLightfv(GL2ES1.GL_LIGHT0, GL2ES1.GL_AMBIENT, ambient, 0);
        gl.glLightfv(GL2ES1.GL_LIGHT0, GL2ES1.GL_DIFFUSE, diffuse, 0);
        gl.glLightfv(GL2ES1.GL_LIGHT0, GL2ES1.GL_POSITION, lightPosition, 0);

        gl.glEnable(GL2ES1.GL_LIGHTING);
        gl.glEnable(GL2ES1.GL_LIGHT0);
        gl.glEnable(GL.GL_DEPTH_TEST);

        params.setPosition(new Vec3f(0, 0, 0));
        params.setForwardDirection(Vec3f.NEG_Z_AXIS);
        params.setUpDirection(Vec3f.Y_AXIS);
        params.setVertFOV((float) (Math.PI / 8.0));
        params.setImagePlaneAspectRatio(1);
        params.xSize = X_SIZE;
        params.ySize = Y_SIZE;

        gl.glMatrixMode(GL2ES1.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(45, 1, 1, 100);
        gl.glMatrixMode(GL2ES1.GL_MODELVIEW);
        gl.glLoadIdentity();

        viewer = new ExaminerViewer();


        // Instantiate ExaminerViewer

        viewer.setUpVector(Vec3f.Y_AXIS);

        viewer.start((GLWindow) drawable);

        // Register the window with the ManipManager
        manip = viewer.manip;
        manip.registerWindow(drawable);

        // Instantiate a HandleBoxManip
        HandleBoxManip manip = new HandleBoxManip();
        manip.setTranslation(new Vec3f(0, 0, -10));
        this.manip.showManipInWindow(manip, drawable);


        viewer.attach(new HandleBoxManipBSphereProvider(manip));
        viewer.viewAll(gl);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        viewer.update(gl);
        manip.updateCameraParameters(drawable, viewer.getCameraParameters());
        manip.render(drawable, gl);
    }

    // Unused routines
    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
    }

//        public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
//        }
//

    public TestExaminerViewer() {
        super();

    }

    public static void main(String[] args) {
        new TestExaminerViewer().show(800, 600);
    }
}