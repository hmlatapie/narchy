package nars.video;

import jcog.Util;
import nars.*;
import nars.concept.SensorConcept;
import nars.control.CauseChannel;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * manages reading a camera to a pixel grid of SensorConcepts
 * monochrome
 */
public class CameraSensor<P extends Bitmap2D> extends Sensor2D<P> implements Consumer<NAgent>, Iterable<CameraSensor<P>.PixelConcept> {


    public static final int RADIX = 1;

    public final List<PixelConcept> pixels;
    public final CauseChannel<Task> in;
    private final Term id;

    float resolution = 0.01f;//Param.TRUTH_EPSILON;

    final int numPixels;


    transient int w, h;
    transient float conf;


    public CameraSensor(Term root, P src, NAgent a) {
        super(src, src.width(), src.height(), a.nar);
        this.id = root;

        this.w = src.width();
        this.h = src.height();
        numPixels = w * h;

        this.in = a.nar.newCauseChannel(this);
//        this.in.amplitude(  //shared amongst all pixels
//                //1f/((float)Math.sqrt(w*h))
//                (float) (1f / (Math.sqrt(Math.min(w, h))))
//        );

        pixels = encode(RadixProduct(root, w, h, RADIX), a.nar);

        a.onFrame(this);

    }


    @NotNull
    @Override
    public Iterator<PixelConcept> iterator() {
        return pixels.iterator();
    }


    private final FloatToObjectFunction<Truth> brightnessTruth = (v) -> $.t(v, conf);

    public static Int2Function<Compound> XY(Term root, int width, int height) {
        return (x, y) -> {
            return $.inh($.p(x, y), root);
        };
    }
    public static Int2Function<Compound> XY(Term root, int radix, int width, int height) {
        return (x, y) -> {
            return $.inh($.p($.pRadix(x, radix, width), $.pRadix(y, radix, height)), root);
        };
    }

    private static Int2Function<Term> RadixProduct(Term root, int width, int height, int radix) {
        return (x, y) ->
                $.inh(
                //$.p(root,



                        //$.secte
                        radix > 1 ?
                                //$.pRecurse( zipCoords(coord(x, width), coord(y, height)) ) :
                                $.p(zipCoords(coord(x, width), coord(y, height))) :
                                //$.p(new Term[]{coord('x', x, width), coord('y', y, height)}) :
                                //new Term[]{coord('x', x, width), coord('y', y, height)} :
                                $.p(x, y)
                    , root)
        ;
    }

    private static Term[] zipCoords(@NotNull Term[] x, @NotNull Term[] y) {
        int m = Math.max(x.length, y.length);
        Term[] r = new Term[m];
        int sx = m - x.length;
        int sy = m - y.length;
        int ix = 0, iy = 0;
        for (int i = 0; i < m; i++) {
            Term xy;
            char levelPrefix =
                    (char) ('a' + (m - 1 - i)); //each level given a different scale prefix
            //'p';

            if (i >= sx && i >= sy) {
                xy = Atomic.the(levelPrefix + x[ix++].toString() + y[iy++]);
            } else if (i >= sx) {
                xy = Atomic.the(levelPrefix + x[ix++].toString() + "_");
            } else { //if (i < y.length) {
                xy = Atomic.the(levelPrefix + "_" + y[iy++]);
            }
            r[i] = xy;
        }
        return r;
    }

    @NotNull
    public static Term coord(char prefix, int n, int max) {
        //return $.pRecurseIntersect(prefix, $.radixArray(n, radix, max));
        //return $.pRecurse($.radixArray(n, radix, max));
        return $.p($.the(prefix), $.p($.radixArray(n, 2, max)));
    }

    @NotNull
    public static Term[] coord(int n, int max) {
        //return $.pRecurseIntersect(prefix, $.radixArray(n, radix, max));
        //return $.pRecurse($.radixArray(n, radix, max));
        return $.radixArray(n, 2, max);
    }


    public List<PixelConcept> encode(Int2Function<Term> cellTerm, NAR nar) {
        List<PixelConcept> l = $.newArrayList();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                //TODO support multiple coordinate termizations
                Term cell = cellTerm.get(x, y);


                PixelConcept sss = new PixelConcept(cell, x, y, nar);
                nar.on(sss);


                l.add(sss);

                matrix[x][y] = sss;
            }
        }
        return l;
    }

//    private float distToResolution(float dist) {
//
//        float r = Util.lerp(minResolution, maxResolution, dist);
//
//        return r;
//    }

    public CameraSensor resolution(float resolution) {
        this.resolution = resolution;
        pixels.forEach(p -> p.resolution(resolution));
        return this;
    }

    public PixelConcept concept(int x, int y) {
        if (x < 0)
            x += w;
        if (y < 0)
            y += h;
        if (x >= w)
            x -= w;
        if (y >= h)
            y -= h;
        return (PixelConcept) matrix[x][y]; //pixels.get(x * width + y);
    }

    @Override
    public void accept(NAgent a) {

        //frameStamp();

        src.update(1);

        NAR nar = a.nar;

        long now = a.now;
        int dur = nar.dur();

        this.conf = nar.confDefault(Op.BELIEF);
        in.input(pixels.stream() /*filter(PixelConcept::update).*/
                        .map(c -> c.update(now, dur, nar)));
    }


    interface Int2Function<T> {
        T get(int x, int y);
    }

//    private long nextStamp;
//    private void frameStamp() {
//        nextStamp = nar.time.nextStamp();
//    }


    class PixelConcept extends SensorConcept {

//        //private final int x, y;
        //private final TermContainer templates;

        PixelConcept(Term cell, int x, int y, NAR nar) {
            super(cell, nar, null, brightnessTruth);
            setSignal(() -> Util.unitize(src.brightness(x, y)));

            //            this.x = x;
//            this.y = y;

            //                List<Term> s = $.newArrayList(4);
//                //int extraSize = subs.size() + 4;
//
//                if (x > 0) s.add( concept(x-1, y) );
//                if (x < w-1) s.add( concept(x+1, y) );
//                if (y > 0) s.add( concept(x, y-1) );
//                if (y < h-1) s.add( concept(x, y+1) );
//
//                return TermVector.the(s);

            //this.templates = new PixelNeighborsXYRandom(x, y, w, h, 1);
        }


        //        @Override
//        public TermContainer templates() {
//            return templates;
//        }


        //        @Override
//        protected LongSupplier update(Truth currentBelief, @NotNull NAR nar) {
//            return ()->nextStamp;
//        }

    }



//    /** links only to the 'id' of the image, and N random neighboring pixels */
//    private class PixelNeighborsXYRandom implements TermContainer {
//
//        private final int x;
//        private final int y;
//        private final int w;
//        private final int h;
//        private final int extra;
//
//        public PixelNeighborsXYRandom(int x, int y, int w, int h, int extra) {
//            this.extra = extra;
//            this.x = x;
//            this.y = y;
//            this.w = w;
//            this.h = h;
//        }
//
//        @Override
//        public int size() {
//            return extra + 1;
//        }
//
//        @Override
//        public @NotNull Term sub(int i) {
//
//            if (i == 0) {
//                return id;
//            } else {
//                //extra
//                Random rng = nar.random();
//                return concept(
//                        x + (rng.nextBoolean() ? -1 : +1),
//                        y + (rng.nextBoolean() ? -1 : +1)
//                ).term();
//            }
//
//
//
//        }
//    }

//    private class PixelNeighborsManhattan implements TermContainer {
//
//        private final int x;
//        private final int y;
//        private final int w;
//        private final int h;
//        private final TermContainer subs;
//
//        public PixelNeighborsManhattan(TermContainer subs, int x, int y, int w, int h) {
//            this.subs = subs;
//            this.x = x;
//            this.y = y;
//            this.w = w;
//            this.h = h;
//        }
//
//        @Override
//        public int size() {
//            return 4 + subs.size();
//        }
//
//        @Override
//        public @NotNull Term sub(int i) {
//
//
//            switch (i) {
//                case 0:
//                    return (x == 0) ? sub(1) : concept(x - 1, y).sub(0);
//                case 1:
//                    return (x == w - 1) ? sub(0) : concept(x + 1, y).sub(0);
//                case 2:
//                    return (y == 0) ? sub(3) : concept(x, y - 1).sub(0);
//                case 3:
//                    return (y == h - 1) ? sub(2) : concept(x, y + 1).sub(0);
//                default:
//                    return subs.sub(i - 4);
//            }
//
//        }
//    }
}
