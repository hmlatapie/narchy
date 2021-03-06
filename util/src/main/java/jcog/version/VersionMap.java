package jcog.version;

import jcog.list.ArrayUnenforcedSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;


public class VersionMap<X, Y> extends AbstractMap<X, Y> {

    private final Versioning context;
    public final Map<X, Versioned<Y>> map;
    public final int elementStackSizeDefault; //stackSizePerElement


    public VersionMap(Versioning context) {
        this(context, 0);
    }

    public VersionMap(Versioning context, int mapCap) {
        this(context, mapCap, 1);
    }

    /**
     * @param context
     * @param mapCap  initial capacity of map (but can grow
     * @param eleCap  initial capacity of map elements (but can grow
     */
    public VersionMap(Versioning context, int mapCap, int eleCap) {
        this(context,
                new HashMap(mapCap)
                //new LinkedHashMap<>(mapCap)
                //new UnifiedMap(mapCap)
                , eleCap
        );
    }

    public VersionMap(Versioning<Y> context, Map<X, Versioned<Y>/*<Y>*/> map, int elementStackSizeDefault) {
        this.context = context;
        this.map = map;
        this.elementStackSizeDefault = elementStackSizeDefault;
    }


    @Nullable
    @Override
    public Y remove(Object key) {
        Versioned<Y> x = map.remove(key);
        return x != null ? x.get() : null;
    }

    @Override
    public boolean remove(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public final int size() {
        int count = 0;
        for (Entry<X, Versioned<Y>> e : map.entrySet()) {
            if (e.getValue().get()!=null)
                count++;
        }
        return count;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }


    /**
     * avoid using this if possible because it involves transforming the entries from the internal map to the external form
     */
    @NotNull
    @Override
    public Set<Entry<X, Y>> entrySet() {
        ArrayUnenforcedSet<Entry<X, Y>> e = new ArrayUnenforcedSet<>();
        map.forEach((k, v) -> {
            Y vv = v.get();
            if (vv != null) {
                //TODO new LazyMapEntry(k, v)
                e.add(new SimpleEntry<>(k, vv));
            }
        });
        return e;
    }

//    public static class LazyMapEntry<K,V>  implements Map.Entry<K,V> {
//
//        @Override
//        public K getKey() {
//            return key;
//        }
//
//        @Override
//        public V getValue() {
//            return null;
//        }
//
//        @Override
//        public V setValue(V value) {
//            return null;
//        }
//    }

    /**
     * records an assignment operation
     * follows semantics of set()
     */
    @Override
    public final Y put(X key, Y value) {
        throw new UnsupportedOperationException("use tryPut(k,v)");
    }

    public boolean tryPut(X key, Y value) {
        return getOrCreateIfAbsent(key).set(value) != null;
    }

    public final Versioned<Y> getOrCreateIfAbsent(X key) {
        return map.computeIfAbsent(key, this::newEntry);

//        Versioned<Y> v = map.get(key);
//        if (v!=null) return v;
//        v = newEntry(key);
//        map.put(key, v);
//        return v;
    }

    protected Versioned<Y> newEntry(X x) {
        return new Versioned<>(context, elementStackSizeDefault);
        //return cache(k) ? new Versioned(context) :
        //return new RemovingVersionedEntry(k);
    }

    public void forEach(BiConsumer<? super X, ? super Y> each) {
        map.forEach((x,yy)->{
            Y y = yy.get();
            if (y!=null)
                each.accept(x, y);
        });
    }


    public boolean forEachVersioned(BiPredicate<? super X, ? super Y> each) {
        Set<Entry<X, Versioned<Y>>> ee = map.entrySet();
        for (Entry<X, Versioned<Y>> e : ee) {
            Y y = e.getValue().get();
            if (y != null) {
                if (!each.test(e.getKey(), y)) {
                    return false;
                }
            }
        }
        return true;
    }

//    public boolean replace(Function<? super Y, Y> eachValue) {
//        Set<Entry<X, Versioned<Y>>> eee = map.entrySet();
//        for (Entry<X, Versioned<Y>> e : eee) {
//            Versioned<Y> ee = e.getValue();
//            Y x = ee.get();
//            if (x != null) {
//                ee.replaceTop(eachValue.apply(x));
//            }
//        }
//        return true;
//    }


    @Override
    public Y get(/*X*/Object key) {
        Versioned<Y> v = getVersioned(key);
        return v != null ? v.get() : null;
    }

    public Versioned<Y> getVersioned(/*X*/Object key) {
        return map.get(key);
    }

    public boolean compute(/*X*/X key, Function<Y,Y> f) {
        final boolean[] result = {false};
        map.compute(key, (k, v)->{

            Y prev, next;

            prev = v == null ? null : v.get();

            next = f.apply(prev);

            if (next!=null) {
                if (v == null)
                    v = newEntry(k);
                result[0] = v.set(next)!=null;
            } else {
                result[0] = false;
            }
            return v;
        });
        return result[0];
    }

//    @Nullable
//    public Y get(X key, @NotNull Supplier<Y> ifAbsentPut) {
//        //TODO use compute... Map methods
//        Y o = get(key);
//        if (o == null) {
//            o = ifAbsentPut.get();
//            put(key, o);
//        }
//        return o;
//    }


    @Override
    public final boolean containsKey(Object key) {
        throw new UnsupportedOperationException(); //requires filtering
        //return map.containsKey(key);
    }

    @NotNull
    @Override
    public Set<X> keySet() {
        throw new UnsupportedOperationException(); //requires filtering
        //return map.keySet();
    }

    public static final VersionMap Empty = new VersionMap(new Versioning<>(1, 0), 0, 0) {

        @Override
        public boolean tryPut(Object key, Object value) {
            return false;
        }

        @Override
        public Object get(Object key) {
            return null;
        }
    };



}
