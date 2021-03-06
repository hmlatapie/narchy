package spacegraph.space2d.container;

import com.google.common.collect.Sets;
import jcog.list.FastCoWList;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;

public class MutableContainer extends Container {


    private final FastCoWList<Surface> children = new FastCoWList(1,
            NEW_SURFACE_ARRAY);


    public MutableContainer(Surface... children) {
        super();
        if (children.length > 0)
            set(children);
    }

    public MutableContainer(Collection<Surface> x) {
        this(x.toArray(new Surface[0]));
    }

    public Surface[] children() {
        return children.copy;
    }

    @Override
    public int childrenCount() {
        return children.size();
    }

    @Override
    public void start(SurfaceBase parent) {
        synchronized (children) {
            super.start(parent);
            children.forEach(c -> {
                assert(c.parent==null || c.parent == this);
                c.start(this);
            });
        }
        layout();
    }

    @Override
    public void stop() {
        synchronized (children) {
            children.forEach(Surface::stop);
            super.stop();
        }
    }

    @Override
    public void doLayout(int dtMS) {
        children.forEach(Surface::layout);
    }

    public Surface get(int index) {
        return children.copy[index];
    }

    /**
     * returns the existing value that was replaced
     */
    public Surface set(int index, Surface next) {
        Surface existing;
        synchronized (children) {
            if (children.size() - 1 < index)
                throw new RuntimeException("index out of bounds");

            existing = get(index);
            if (existing != next) {
                existing.stop();

                children.set(index, next);

                if (this.parent!=null)
                    next.start(this);
            }
        }
        layout();
        return existing;
    }

    public void addAll(Surface... s) {
        for (Surface x : s)
            add(x);
    }

    public void add(Surface s) {
        synchronized (children) {

            assert (s.parent == null);
            s.start(this);

            children.add(s); //assume it was added to the list
        }

        layout();
    }

    //TODO: addIfNotPresent(x) that tests for existence first

    public boolean remove(Surface s) {
        synchronized (children) {
            assert (s.parent == this);
            if (children.remove(s)) {
                s.stop();
                layout();
                return true;
            } else {
                return false;
            }
        }
    }

    public final Container set(Surface... next) {

        synchronized (children) {

            if (parent == null) {
                children.set(next);
                return this;
            }

            int numExisting = children.size();
            if (numExisting == 0) {
                //currently empty, just add all
                for (Surface n : next)
                    n.start(this);

                children.set(next);

            } else if (next.length == 0) {

                children.forEach(Surface::stop);
                children.clear();

            } else {
                //possibly some remaining, so use Set intersection to invoke start/stop only as necessary

                Sets.SetView unchanged = Sets.intersection(
                        Set.of(children.copy), Set.of(next)
                );
                if (unchanged.isEmpty()) unchanged = null;

                for (Surface existing : children.copy) {
                    if (unchanged == null || !unchanged.contains(existing))
                        existing.stop();
                }

                for (Surface n : next) {
                    if (unchanged == null || !unchanged.contains(n))
                        n.start(this);
                }

                children.set(next);
            }

        }

        layout();
        return this;
    }

    public final Container set(List<? extends Surface> next) {
        set(next.toArray(new Surface[0]));
        return this;
    }


    @Override
    public void forEach(Consumer<Surface> o) {
        for (Surface x : children.copy) {
            if (x.parent!=null) //if ready
                o.accept(x);
        }
    }

    @Override
    public boolean whileEach(Predicate<Surface> o) {
        for (Surface x : children.copy) {
            if (x.parent!=null) //if ready
                if (!o.test(x))
                    return false;
        }
        return true;
    }
    @Override
    public boolean whileEachReverse(Predicate<Surface> o) {
        @Nullable Surface[] copy = children.copy;
        for (int i = copy.length-1; i >= 0; i--) {
            Surface x = copy[i];
            if (x.parent != null) //if ready
                if (!o.test(x))
                    return false;
        }
        return true;
    }

    final static Surface[] EMPTY_SURFACE_ARRAY = new Surface[0];

    static final IntFunction<Surface[]> NEW_SURFACE_ARRAY = (i) -> {
        return i == 0 ? EMPTY_SURFACE_ARRAY : new Surface[i];
    };

    public int size() {
        return children.size();
    }
    public boolean isEmpty() {
        return children.isEmpty();
    }

    public void clear() {
        synchronized (children) {
            if (size()> 0) {
                children.forEach(Surface::stop);
                children.clear();
                layout();
            }
        }
    }

    /** this can be accelerated by storing children as a Map */
    public void replace(Surface child, Surface replacement) {
        synchronized (children) {
            if (!children.remove(child))
                throw new RuntimeException("could not replace missing " + child + " with " + replacement);

            add(replacement);
        }
    }

//    private class Children extends FastCoWList<Surface> {
//
//        public Children(int capacity) {
//            super(capacity, NEW_SURFACE_ARRAY);
//        }
//
//        @Override
//        public boolean add(Surface surface) {
//            synchronized (children) {
//                if (!super.add(surface)) {
//                    return false;
//                }
//                if (parent != null) {
//                    layout();
//                }
//            }
//            return true;
//        }
//
//        @Override
//        public Surface set(int index, Surface neww) {
//            Surface old;
//            synchronized (children) {
//                while (size() <= index) {
//                    add(null);
//                }
//                old = super.set(index, neww);
//                if (old == neww)
//                    return neww;
//                else {
//                    if (old != null) {
//                        old.stop();
//                    }
//                    if (neww != null && parent != null) {
//                        neww.start(MutableContainer.this);
//                    }
//                }
//            }
//            layout();
//            return old;
//        }
//
//        @Override
//        public boolean addAll(Collection<? extends Surface> c) {
//            synchronized (children) {
//                for (Surface s : c)
//                    add(s);
//            }
//            layout();
//            return true;
//        }
//
//        @Override
//        public Surface remove(int index) {
//            Surface x;
//            synchronized (children) {
//                x = super.remove(index);
//                if (x == null)
//                    return null;
//                x.stop();
//            }
//            layout();
//            return x;
//        }
//
//        @Override
//        public boolean remove(Object o) {
//            synchronized (children) {
//                if (!super.remove(o))
//                    return false;
//                ((Surface) o).stop();
//            }
//            layout();
//            return true;
//        }
//
//
//        @Override
//        public void add(int index, Surface element) {
//            synchronized (children) {
//                super.add(index, element);
//            }
//            layout();
//        }
//
//        @Override
//        public void clear() {
//            synchronized (children) {
//                this.removeIf(x -> {
//                    x.stop();
//                    return true;
//                });
//            }
//            layout();
//        }
//    }

}
