package nars.experiment.minicraft;

import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.experiment.minicraft.side.SideScrollMinicraft;
import nars.experiment.minicraft.side.awtgraphics.AwtGraphicsHandler;
import nars.remote.SwingAgent;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.time.Tense;
import nars.util.Util;
import nars.util.signal.AutoClassifier;
import nars.video.MatrixSensor;
import nars.video.PixelBag;
import org.jetbrains.annotations.NotNull;
import spacegraph.obj.MatrixView;

import java.awt.image.BufferedImage;
import java.util.Arrays;

import static nars.util.Texts.n2;
import static nars.util.Texts.n4;

/**
 * Created by me on 9/19/16.
 */
public class SideCraft extends SwingAgent {

    private final SideScrollMinicraft craft;
    private final MatrixSensor pixels;
    private PixelAutoClassifier camAE;

    public static void main(String[] args) {
        run(SideCraft::new, 512);
    }

    public SideCraft(NAR nar) {
        super(nar, 0);

        this.craft = new SideScrollMinicraft();


        //swingCam.input(W/4, H/4, W/2, H/2); //50%

//        Scale cam = new Scale(swingCam, 48, 48);
//        SwingCamera swing = new SwingCamera(((AwtGraphicsHandler) craft.gfx).buffer);
//        nar.onFrame(nn -> {
//            swing.update();
//        });

        BufferedImage camBuffer = ((AwtGraphicsHandler) craft.gfx).buffer;

        PixelBag cam = new PixelBag(camBuffer, 64, 64).addActions("cra", this);

        //camAE = new PixelAutoClassifier("cra", cam.pixels, 16, 16, 24, 4, this);
        //SpaceGraph.window(new MatrixView(camAE.W.length, camAE.W[0].length, arrayRenderer(camAE.W)), 500, 500);


        pixels = addCamera("cra", cam, (v) -> $.t(v, alpha));


//        new NObj("cra", craft, nar)
//                .read(
//                    "player.health",
//                    "player.dir",
//                    "player.getTile().connectsToGrass",
//                    "player.getTile().connectsToWater"
//                ).into(this);

//        InputHandler input = craft.input;
        actionToggle("cra:(key,left)", (b) -> {
            if (b) craft.player.startLeft(false /* slow */);
            else craft.player.stopLeft();
        });
        actionToggle("cra:(key,right)", (b) -> {
            if (b) craft.player.startRight(false /* slow */);
            else craft.player.stopRight();
        });
        actionToggle("cra:(key,up)", (b) -> {
            if (b) craft.player.startClimb();
            else craft.player.stopClimb();
        });
        actionToggle("cra:(key,mouseL)", (b) -> craft.leftClick = b);
        actionToggle("cra:(key,mouseR)", (b) -> craft.rightClick = b);
        float mSpeed = 25f;
        actionBipolar("cra:(mouse,X)", (v) -> {
            int x = craft.screenMousePos.x;
            int xx = Util.clamp(x + v * mSpeed, 0, camBuffer.getWidth() - 1);
            if (xx != x) {
                craft.screenMousePos.x = xx;
                return true;
            }
            return false;
        });
        actionBipolar("cra:(mouse,Y)", (v) -> {
            int y = craft.screenMousePos.y;
            int yy = Util.clamp(y + v * mSpeed, 0, camBuffer.getHeight() - 1);
            if (yy != y) {
                craft.screenMousePos.y = yy;
                return true;
            }
            return false;
        });


//        addToggleAction("cra:up", (b) -> input.up.toggle(b) );
//        addToggleAction("cra:down", (b) -> input.down.toggle(b) );
//        addToggleAction("cra:left", (b) -> input.left.toggle(b) );
//        addToggleAction("cra:right", (b) -> input.right.toggle(b) );

        craft.startGame(false, 512);
    }


    float prevScore = 0;

    @Override
    protected float reward() {

        if (camAE!=null)
            camAE.frame();

        float nextScore = craft.frame();
        float ds = nextScore - prevScore;
        this.prevScore = nextScore;
        return ds;
    }

    public static class PixelAutoClassifier extends AutoClassifier {

        private final NAR nar;
        @NotNull Atom TAG = $.the("ae");

        private final float[][] pixIn;
        private final int[] pixOut;
        public final float[][] pixRecon; //reconstructed input
        private final Term root;
        private final float in[];
        private final int sw, sh;
        private final int nw, nh;
        private final int pw, ph;
        private final int batchSize;
        private boolean reconstruct = true;
        public boolean learn = true;

//        public PixelAutoClassifier(String root, float[][] pixIn, int sw, int sh, NAgent agent) {
//            this(root, pixIn, sw, sh, sw * sh /* estimate */, 4, agent);
//        }

        public PixelAutoClassifier(String root, float[][] pixIn, int sw, int sh, int states, int termBatchSize, NAgent agent) {
            super(sw * sh, states, agent.nar.random);
            this.nar = agent.nar;
            this.root = $.the(root);
            this.pixIn = pixIn;
            this.sw = sw; //stride width
            this.sh = sh; //stride height
            ph = pixIn[0].length;
            pw = pixIn.length;
            this.nw = (int) Math.ceil(pw / ((float) sw)); //number strides wide
            this.nh = (int) Math.ceil(ph / ((float) sh)); //number strides high
            this.in = new float[sw * sh];
            this.pixRecon = new float[pw][ph];
            this.pixOut = new int[nw * nh];
            this.batchSize = termBatchSize;
            assert (nw * nh % batchSize == 0); //evenly divides
        }

        public void frame() {
            int q = 0;

            float alpha = 0.05f;

            float sumErrSquared = 0;
            //forget(alpha*alpha);


            for (int i = 0; i < nw; ) {
                for (int j = 0; j < nh; ) {

                    int p = 0;
                    int oi = i * sw;
                    int oj = j * sh;
                    for (int si = 0; si < sw; si++) {
                        int d = si + oi;
                        if (d >= pw)
                            break;

                        float[] col = pixIn[d];
                        for (int sj = 0; sj < sh; sj++) {

                            int c = sj + oj;

                            in[p++] = c < ph ? col[c] : 0;

                        }
                    }

                    boolean sigIn = true, sigOut = true;
                    if (learn) {
                        float regionError = train(in, alpha, 0f, 0.2f, sigIn, sigOut);
                        sumErrSquared += regionError;
                        //System.out.println(n2(y) + ", +-" + n4(regionError / y.length));
                    } else {
                        //System.out.println(n2(y));
                        recode(in, sigIn, sigOut);
                    }
                    pixOut[q++] = max();

                    if (reconstruct) {
                        float z[] = this.z;
                        p = 0;
                        for (int si = 0; si < sw; si++) {
                            int d = si + oi;
                            if (d >= pw)
                                break;

                            float[] col = pixRecon[d];
                            for (int sj = 0; sj < sh; sj++) {

                                int c = sj + oj;

                                if (c >= ph)
                                    break;
                                col[c] = z[p++];
                            }
                        }
                    }

                    j++;
                }

                i++;
            }

            float meanErrSquaredPerPixel = sumErrSquared / (pw * ph);
            System.out.println(Arrays.toString(pixOut) +", +-" + n4(meanErrSquaredPerPixel));


            int qq = 0;
            for (int i = 0; i < pixOut.length; i += batchSize) {
                Term[] t = new Term[batchSize + 1];
                int j = 0;
                for (; j < batchSize; )
                    t[j++] = $.the(pixOut[qq++]);
                t[j] = TAG;

                //TODO use a new Term choice sensor type here

                Term Y = $.inh($.p(t), root);
                //System.out.println(Y);
                nar.believe(Y, Tense.Present);
            }

        }


    }

    public static MatrixView.ViewFunc arrayRenderer(float[][] ww) {
        return (x, y, g) -> {
            float v = ww[x][y];
            if (v < 0) {
                v = -v;
                g.glColor3f(v / 2, 0, v);
            } else {
                g.glColor3f(v, v / 2, 0);
            }
            return 0;
        };
    }

}
