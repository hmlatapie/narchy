package nars.bag;

import jcog.bag.Bag;
import jcog.bag.impl.ArrayBag;
import jcog.learn.gng.NeuralGasNet;
import jcog.learn.gng.impl.Centroid;
import jcog.list.FasterList;
import jcog.pri.VLink;
import jcog.pri.op.PriMerge;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * clusterjunctioning
 * TODO abstract into general purpose "Cluster of Bags" class
 */
public class BagClustering<X> {

    public final Bag<X, VLink<X>> bag;

    final Dimensionalize<X> model;

    public final NeuralGasNet net;


    /**
     * TODO allow dynamic change
     */
    protected /*Flip<*/ FasterList<VLink<X>> sorted =
            new FasterList<>();
    //new Flip(FasterList::new);

    public BagClustering(Dimensionalize<X> model, int centroids, int initialCap) {


        this.model = model;

        this.net = new NeuralGasNet(model.dims, centroids, model::distanceSq);

        this.bag = new ArrayBag<>(PriMerge.max, initialCap) {

            @Nullable
            @Override
            public X key(VLink<X> x) {
                return x.id;
            }

        };

//        this.bag = new HijackBag<>(initialCap, 4) {
//            @Override
//            protected VLink<X> merge(VLink<X> existing, VLink<X> incoming, @Nullable MutableFloat overflowing) {
//                existing.priMax(incoming.priElseZero());
//                return existing;
//            }
//
//            @Override
//            public float pri(VLink<X> key) {
//                return key.pri();
//            }
//
//            @Override
//            public X key(VLink<X> value) {
//                return value.get();
//            }
//        };
    }


    public void print() {
        print(System.out);
    }

    public void print(PrintStream out) {
        forEachCluster(c -> {
            out.println(c);
            stream(c.id).forEach(i -> {
                out.print("\t");
                out.println(i);
            });
            out.println();
        });
        out.println(net.edges);
    }

    public void forEachCluster(Consumer<Centroid> c) {
        for (Centroid b : net.centroids) {
            c.accept(b);
        }
    }


//    protected class MyForget extends PriForget<VLink<X>> {
//
//        public MyForget(float priFactor) {
//            super(priFactor);
//        }
//
//        @Override
//        public void accept(VLink<X> b) {
//            super.accept(b);
//            learn(b);
//        }
//    }

    public int size() {
        return bag.size();
    }

    public <Y> void commitGroups(int iter, Y y, BiConsumer<Stream<VLink<X>>, Y> each) {
        commit(iter, (sorted) -> {
            int current = -1;
            int n = sorted.size();
            int bs = -1;
            for (int i = 0; i < n; i++) {
                VLink<X> x = sorted.get(i);

                if (current != x.centroid || (i == n - 1)) {
                    current = x.centroid;
                    if (bs != -1 && i - bs > 1) {
                        each.accept(IntStream.range(bs, i + 1).mapToObj(sorted::get), y);
                    }
                    bs = i;
                }
            }
        });
    }


    /**
     * how to interpret the bag items as vector space data
     */
    abstract public static class Dimensionalize<X> {

        final int dims;

        protected Dimensionalize(int dims) {
            this.dims = dims;
        }

        abstract public void coord(X t, double[] d);

        /**
         * default impl, feel free to override
         */
        public double distanceSq(double[] a, double[] b) {
            return Centroid.distanceCartesianSq(a, b);
        }

    }


    public boolean commit(int iterations, Consumer<List<VLink<X>>> takeSortedClusters) {

        FasterList<VLink<X>> x;

        synchronized (bag) {

            int s = bag.size();
            if (s == 0)
                return false;

            bag.commit(); //first, apply bag forgetting

            //                net.compact();
            //int cc = bag.capacity();

            for (int i = 0; i < iterations; i++)
                bag.forEach(this::learn);

            x = new FasterList<>(bag.size());
            bag.forEach(x::add);

            //Collections.sort(x, Comparator.comparingInt(v->v.centroid));
            x.sortThisByInt(xx -> xx.centroid);
            //x.sortThis(Comparator.comparingInt(v->v.centroid));
            //Arrays.sort(x.array(), )
            takeSortedClusters.accept(x);

        }




        return true;


    }


    private void learn(VLink<X> x) {
        double x0 = x.coord[0];
        if (x0 != x0)
            model.coord(x.id, x.coord);

        x.centroid = net.put(x.coord).id;
    }

    public void clear() {
        synchronized (bag) {
            bag.clear();
            net.clear();
        }
    }

    public void put(X x, float pri) {
        bag.putAsync(new VLink<>(x, pri, model.dims)); //TODO defer vectorization until after accepted
    }

    public void remove(X x) {
        bag.remove(x);
    }

    /**
     * returns NaN if either or both of the items are not present
     */
    public double distance(X x, X y) {
        assert (!x.equals(y));
        @Nullable VLink<X> xx = bag.get(x);
        if (xx != null && xx.centroid >= 0) {
            @Nullable VLink<X> yy = bag.get(y);
            if (yy != null && yy.centroid >= 0) {
                return Math.sqrt(net.distanceSq.distance(xx.coord, yy.coord));
            }
        }
        return Double.POSITIVE_INFINITY;
    }

    /**
     * TODO this is O(N) not great
     */
    public Stream<VLink<X>> stream(int centroid) {
        return bag.stream().filter(y -> y.centroid == centroid);
    }

    public Stream<VLink<X>> neighbors(X x) {
        @Nullable VLink<X> link = bag.get(x);
        if (link != null) {
            int centroid = link.centroid;
            if (centroid >= 0) {
                Centroid[] nodes = net.centroids;
                if (centroid < nodes.length) //in case of resize
                    return stream(centroid)
                            .filter(y -> !y.equals(x))
                            ;
            }
        }
        return Stream.empty();
    }

}
