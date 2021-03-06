//package jcog.list;
//
//import org.eclipse.collections.api.LazyIterable;
//import org.eclipse.collections.api.RichIterable;
//import org.eclipse.collections.api.block.HashingStrategy;
//import org.eclipse.collections.api.block.function.Function;
//import org.eclipse.collections.api.block.function.Function2;
//import org.eclipse.collections.api.block.function.primitive.*;
//import org.eclipse.collections.api.block.predicate.Predicate;
//import org.eclipse.collections.api.block.predicate.Predicate2;
//import org.eclipse.collections.api.block.procedure.Procedure;
//import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
//import org.eclipse.collections.api.list.ImmutableList;
//import org.eclipse.collections.api.list.MutableList;
//import org.eclipse.collections.api.list.ParallelListIterable;
//import org.eclipse.collections.api.list.primitive.*;
//import org.eclipse.collections.api.map.MutableMap;
//import org.eclipse.collections.api.multimap.list.MutableListMultimap;
//import org.eclipse.collections.api.ordered.OrderedIterable;
//import org.eclipse.collections.api.partition.list.PartitionMutableList;
//import org.eclipse.collections.api.stack.MutableStack;
//import org.eclipse.collections.api.tuple.Pair;
//import org.eclipse.collections.impl.collection.mutable.AbstractMultiReaderMutableCollection;
//import org.eclipse.collections.impl.factory.Lists;
//import org.eclipse.collections.impl.lazy.ReverseIterable;
//import org.eclipse.collections.impl.lazy.parallel.list.MultiReaderParallelListIterable;
//import org.eclipse.collections.impl.list.mutable.SynchronizedMutableList;
//import org.eclipse.collections.impl.list.mutable.UnmodifiableMutableList;
//import org.jetbrains.annotations.Nullable;
//
//import java.io.Externalizable;
//import java.io.IOException;
//import java.io.ObjectInput;
//import java.io.ObjectOutput;
//import java.util.*;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.locks.ReadWriteLock;
//import java.util.concurrent.locks.ReentrantReadWriteLock;
//import java.util.function.Consumer;
//
///**
// * extension of org.eclipse collections MultiRWFasterList
// */
//public class MultiRWFasterList<T> extends AbstractMultiReaderMutableCollection<T>
//        implements RandomAccess, Externalizable, MutableList<T> {
//    private static final long serialVersionUID = 1L;
//
//    private transient ReadWriteLock lock;
//    private FasterList<T> delegate;
//
//
//    public <E> E[] toArray(IntToObjectFunction<E[]> arrayBuilder, int extraSize) {
//        this.acquireReadLock();
//        try {
//            return delegate.toArray(arrayBuilder.apply(delegate.size() + extraSize));
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    /**
//     * note: this is a semi-volatile operation and relies on the delegate list implementation to allow concurrent isEmpty()
//     */
//    public boolean ifNotEmptyAcquireReadLock() {
//
//        if (delegate.isEmpty())
//            return false;
//
//        super.acquireReadLock();
//        return true;
//    }
//
//    /**
//     * note: this is a semi-volatile operation and relies on the delegate list implementation to allow concurrent isEmpty()
//     */
//    public boolean ifNotEmptyAcquireWriteLock() {
//
//        if (delegate.isEmpty())
//            return false;
//
//        super.acquireWriteLock();
//        return true;
//    }
//
//    @Override
//    public void acquireReadLock() {
//        super.acquireReadLock();
//    }
//
//    @Override
//    public void unlockReadLock() {
//        super.unlockReadLock();
//    }
//
//    @Override
//    public void unlockWriteLock() {
//        super.unlockWriteLock();
//    }
//
//    /**
//     * note: this is a semi-volatile operation and relies on the delegate list implementation to allow concurrent isEmpty()
//     */
//    @Override
//    public boolean removeIf(java.util.function.Predicate<? super T> filter) {
//        if (delegate.isEmpty())
//            return false;
//        this.acquireWriteLock();
//        try {
//
//            boolean removed = false;
//            final Iterator<T> each = delegate.iterator();
//            while (each.hasNext()) {
//                T x = each.next();
//                if (filter.test(x)) {
//                    each.remove();
//                    onRemoved(x);
//                    removed = true;
//                }
//            }
//            return removed;
//
//        } finally {
//            this.unlockWriteLock();
//        }
//    }
//
//    protected void onRemoved(T x) {
//
//    }
//
//    /**
//     * note: this is a semi-volatile operation and relies on the delegate list implementation to allow concurrent isEmpty()
//     */
//    @Override
//    public void forEach(Consumer<? super T> action) {
//        if (delegate.isEmpty())
//            return;
//
//        this.acquireReadLock();
//        try {
//            delegate.forEach(action);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public void each(Procedure<? super T> procedure) {
//        throw new UnsupportedOperationException();
//    }
//
//    //adds isEmpty() test inside the lock
//    @Nullable
//    @Override
//    public <V extends Comparable<? super V>> T maxBy(Function<? super T, ? extends V> function) {
//        this.acquireReadLock();
//        try {
//            MutableList<T> d = getDelegate();
//            return !d.isEmpty() ? d.maxBy(function) : null;
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    /**
//     * @deprecated Empty default constructor used for serialization.
//     */
//    @SuppressWarnings("UnusedDeclaration")
//    @Deprecated
//    public MultiRWFasterList() {
//        // For Externalizable use only
//    }
//
//    protected MultiRWFasterList(FasterList<T> newDelegate) {
//        this(newDelegate, new ReentrantReadWriteLock());
//    }
//
//    private MultiRWFasterList(FasterList<T> newDelegate, ReadWriteLock newLock) {
//        this.lock = newLock;
//        this.delegate = newDelegate;
//    }
//
//    public static <T> MultiRWFasterList<T> newList() {
//        return new MultiRWFasterList<>(new FasterList<>());
//    }
//
//    public static <T> MultiRWFasterList<T> newList(int capacity) {
//        return new MultiRWFasterList<>(new FasterList<>(capacity));
//    }
//
//
//    @Override
//    protected MutableList<T> getDelegate() {
//        return this.delegate;
//    }
//
//    @Override
//    protected ReadWriteLock getLock() {
//        return this.lock;
//    }
//
////    UntouchableMutableList<T> asReadUntouchable() {
////        return new UntouchableMutableList<>(this.delegate.asUnmodifiable());
////
////    }
////
////    UntouchableMutableList<T> asWriteUntouchable() {
////        return new UntouchableMutableList<>(this.delegate);
////    }
//
//    public void withReadLockAndDelegate(Procedure<MutableList<T>> procedure) {
//        this.acquireReadLock();
//        try {
//            //UntouchableMutableList<T> list = this.asReadUntouchable();
//            procedure.value(delegate);
//            //list.becomeUseless();
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    public void withWriteLockAndDelegate(Procedure<FasterList<T>> procedure) {
//        this.acquireWriteLock();
//        try {
//            //MutableList<T> list = this.asWriteUntouchable();
//
//            procedure.value(delegate);
//            //list.becomeUseless();
//        } finally {
//            this.unlockWriteLock();
//        }
//    }
//
//    public void ifSizeExceedsWriteWith(int n, Consumer<FasterList<T>> procedure) {
//        this.acquireWriteLock();
//        try {
//            if (delegate.size() > n)
//                procedure.accept(delegate);
//        } finally {
//            this.unlockWriteLock();
//        }
//    }
//
//
//    public <Y> Y ifNotEmptyWriteWith(Function<MutableList<T>, Y> procedure) {
//        Y result = null;
//
//        this.acquireWriteLock();
//        try {
//            if (!delegate.isEmpty())
//                result = procedure.apply(delegate);
//        } finally {
//            this.unlockWriteLock();
//        }
//
//        return result;
//    }
//
//    public <Y> Y ifNotEmptyReadWith(Function<MutableList<T>, Y> procedure) {
//        Y result = null;
//
//        this.acquireReadLock();
//        try {
//            MutableList<T> d = this.delegate;
//            if (!d.isEmpty()) {
//                result = procedure.apply(d);
//            }
//        } finally {
//            this.unlockReadLock();
//        }
//
//        return result;
//    }
//
//    @Override
//    public MutableList<T> asSynchronized() {
//        this.acquireReadLock();
//        try {
//            return SynchronizedMutableList.of(this);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public MutableList<T> asUnmodifiable() {
//        this.acquireReadLock();
//        try {
//            return UnmodifiableMutableList.of(this);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public ImmutableList<T> toImmutable() {
//        this.acquireReadLock();
//        try {
//            return Lists.immutable.withAll(this.delegate);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public MutableList<T> clone() {
//        throw new UnsupportedOperationException("TODO");
//
////        this.acquireReadLock();
////        try {
////            return new MultiRWFasterList<T>(this.delegate.clone());
////        } finally {
////            this.unlockReadLock();
////        }
//    }
//
//    @Override
//    public <V> MutableList<V> collect(Function<? super T, ? extends V> function) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.collect(function);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public MutableBooleanList collectBoolean(BooleanFunction<? super T> booleanFunction) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.collectBoolean(booleanFunction);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public MutableByteList collectByte(ByteFunction<? super T> byteFunction) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.collectByte(byteFunction);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public MutableCharList collectChar(CharFunction<? super T> charFunction) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.collectChar(charFunction);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public MutableDoubleList collectDouble(DoubleFunction<? super T> doubleFunction) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.collectDouble(doubleFunction);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public MutableFloatList collectFloat(FloatFunction<? super T> floatFunction) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.collectFloat(floatFunction);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public MutableIntList collectInt(IntFunction<? super T> intFunction) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.collectInt(intFunction);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public MutableLongList collectLong(LongFunction<? super T> longFunction) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.collectLong(longFunction);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public MutableShortList collectShort(ShortFunction<? super T> shortFunction) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.collectShort(shortFunction);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public <V> MutableList<V> flatCollect(
//            Function<? super T, ? extends Iterable<V>> function) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.flatCollect(function);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public <V> MutableList<V> collectIf(
//            Predicate<? super T> predicate,
//            Function<? super T, ? extends V> function) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.collectIf(predicate, function);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public <P, V> MutableList<V> collectWith(
//            Function2<? super T, ? super P, ? extends V> function,
//            P parameter) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.collectWith(function, parameter);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public MutableList<T> newEmpty() {
//        return MultiRWFasterList.newList();
//    }
//
//    @Override
//    public MutableList<T> reject(Predicate<? super T> predicate) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.reject(predicate);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public <P> MutableList<T> rejectWith(
//            Predicate2<? super T, ? super P> predicate,
//            P parameter) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.rejectWith(predicate, parameter);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public MutableList<T> tap(Procedure<? super T> procedure) {
//        this.acquireReadLock();
//        try {
//            this.forEach(procedure);
//            return this;
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public MutableList<T> select(Predicate<? super T> predicate) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.select(predicate);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public <P> MutableList<T> selectWith(
//            Predicate2<? super T, ? super P> predicate,
//            P parameter) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.selectWith(predicate, parameter);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public PartitionMutableList<T> partition(Predicate<? super T> predicate) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.partition(predicate);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public <P> PartitionMutableList<T> partitionWith(Predicate2<? super T, ? super P> predicate, P parameter) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.partitionWith(predicate, parameter);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public <S> MutableList<S> selectInstancesOf(Class<S> clazz) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.selectInstancesOf(clazz);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public MutableList<T> distinct() {
//        this.acquireReadLock();
//        try {
//            return this.delegate.distinct();
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public MutableList<T> distinct(HashingStrategy<? super T> hashingStrategy) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.distinct(hashingStrategy);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public <V> MutableList<T> distinctBy(Function<? super T, ? extends V> function) {
//        throw new UnsupportedOperationException("TODO");
//    }
//
//    @Override
//    public MutableList<T> sortThis() {
//        this.acquireWriteLock();
//        try {
//            this.delegate.sortThis();
//            return this;
//        } finally {
//            this.unlockWriteLock();
//        }
//    }
//
//    @Override
//    public MutableList<T> sortThis(Comparator<? super T> comparator) {
//        this.acquireWriteLock();
//        try {
//            this.delegate.sortThis(comparator);
//            return this;
//        } finally {
//            this.unlockWriteLock();
//        }
//    }
//
//    @Override
//    public <V extends Comparable<? super V>> MutableList<T> sortThisBy(
//            Function<? super T, ? extends V> function) {
//        this.acquireWriteLock();
//        try {
//            this.delegate.sortThisBy(function);
//            return this;
//        } finally {
//            this.unlockWriteLock();
//        }
//    }
//
//    @Override
//    public MutableList<T> sortThisByInt(IntFunction<? super T> function) {
//        this.acquireWriteLock();
//        try {
//            this.delegate.sortThisByInt(function);
//            return this;
//        } finally {
//            this.unlockWriteLock();
//        }
//    }
//
//    @Override
//    public MutableList<T> sortThisByBoolean(BooleanFunction<? super T> function) {
//        this.acquireWriteLock();
//        try {
//            this.delegate.sortThisByBoolean(function);
//            return this;
//        } finally {
//            this.unlockWriteLock();
//        }
//    }
//
//    @Override
//    public MutableList<T> sortThisByChar(CharFunction<? super T> function) {
//        this.acquireWriteLock();
//        try {
//            this.delegate.sortThisByChar(function);
//            return this;
//        } finally {
//            this.unlockWriteLock();
//        }
//    }
//
//    @Override
//    public MutableList<T> sortThisByByte(ByteFunction<? super T> function) {
//        this.acquireWriteLock();
//        try {
//            this.delegate.sortThisByByte(function);
//            return this;
//        } finally {
//            this.unlockWriteLock();
//        }
//    }
//
//    @Override
//    public MutableList<T> sortThisByShort(ShortFunction<? super T> function) {
//        this.acquireWriteLock();
//        try {
//            this.delegate.sortThisByShort(function);
//            return this;
//        } finally {
//            this.unlockWriteLock();
//        }
//    }
//
//    @Override
//    public MutableList<T> sortThisByFloat(FloatFunction<? super T> function) {
//        this.acquireWriteLock();
//        try {
//            this.delegate.sortThisByFloat(function);
//            return this;
//        } finally {
//            this.unlockWriteLock();
//        }
//    }
//
//    @Override
//    public MutableList<T> sortThisByLong(LongFunction<? super T> function) {
//        this.acquireWriteLock();
//        try {
//            this.delegate.sortThisByLong(function);
//            return this;
//        } finally {
//            this.unlockWriteLock();
//        }
//    }
//
//    @Override
//    public MutableList<T> sortThisByDouble(DoubleFunction<? super T> function) {
//        this.acquireWriteLock();
//        try {
//            this.delegate.sortThisByDouble(function);
//            return this;
//        } finally {
//            this.unlockWriteLock();
//        }
//    }
//
//    @Override
//    public MutableList<T> subList(int fromIndex, int toIndex) {
//        throw new UnsupportedOperationException("TODO");
////        this.acquireReadLock();
////        try {
////            return new MultiRWFasterList<T>(this.delegate.subList(fromIndex, toIndex), this.lock);
////        } finally {
////            this.unlockReadLock();
////        }
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.equals(o);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public int hashCode() {
//        this.acquireReadLock();
//        try {
//            return this.delegate.hashCode();
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public T get(int index) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.get(index);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public int indexOf(Object o) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.indexOf(o);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public int lastIndexOf(Object o) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.lastIndexOf(o);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public MutableList<T> with(T element) {
//        this.add(element);
//        return this;
//    }
//
//    @Override
//    public MutableList<T> without(T element) {
//        this.remove(element);
//        return this;
//    }
//
//    @Override
//    public MutableList<T> withAll(Iterable<? extends T> elements) {
//        this.addAllIterable(elements);
//        return this;
//    }
//
//    @Override
//    public MutableList<T> withoutAll(Iterable<? extends T> elements) {
//        this.removeAllIterable(elements);
//        return this;
//    }
//
//    /**
//     * This method is not supported directly on a MultiRWFasterList.  If you would like to use a ListIterator with
//     * MultiRWFasterList, then you must do the following:
//     * <p>
//     * <pre>
//     * multiReaderList.withReadLockAndDelegate(new Procedure<MutableList<Person>>()
//     * {
//     *     public void value(MutableList<Person> people)
//     *     {
//     *         Iterator it = people.listIterator();
//     *         ....
//     *     }
//     * });
//     * </pre>
//     */
//    @Override
//    public ListIterator<T> listIterator() {
//        throw new UnsupportedOperationException(
//                "ListIterator is not supported for MultiRWFasterList.  "
//                        + "If you would like to use a ListIterator, you must either use withReadLockAndDelegate() or withWriteLockAndDelegate().");
//    }
//
//    /**
//     * This method is not supported directly on a MultiRWFasterList.  If you would like to use a ListIterator with
//     * MultiRWFasterList, then you must do the following:
//     * <p>
//     * <pre>
//     * multiReaderList.withReadLockAndDelegate(new Procedure<MutableList<Person>>()
//     * {
//     *     public void value(MutableList<Person> people)
//     *     {
//     *         Iterator it = people.listIterator(0);
//     *         ....
//     *     }
//     * });
//     * </pre>
//     */
//    @Override
//    public ListIterator<T> listIterator(int index) {
//        throw new UnsupportedOperationException(
//                "ListIterator is not supported for MultiRWFasterList.  "
//                        + "If you would like to use a ListIterator, you must either use withReadLockAndDelegate() or withWriteLockAndDelegate().");
//    }
//
//    @Override
//    public T remove(int index) {
//        this.acquireWriteLock();
//        try {
//            T x = this.delegate.remove(index);
//            if (x!=null) {
//                onRemoved(x);
//            }
//            return x;
//        } finally {
//            this.unlockWriteLock();
//        }
//    }
//    @Override
//    public boolean remove(Object item)
//    {
//        this.acquireWriteLock();
//        try
//        {
//            if (delegate.remove(item)) {
//                onRemoved((T)item);
//                return true;
//            } else
//                return false;
//        }
//        finally
//        {
//            this.unlockWriteLock();
//        }
//    }
//
//    @Override
//    public T set(int index, T element) {
//        this.acquireWriteLock();
//        try {
//            return this.delegate.set(index, element);
//        } finally {
//            this.unlockWriteLock();
//        }
//    }
//
//    @Override
//    public boolean addAll(int index, Collection<? extends T> collection) {
//        this.acquireWriteLock();
//        try {
//            return this.delegate.addAll(index, collection);
//        } finally {
//            this.unlockWriteLock();
//        }
//    }
//
//    @Override
//    public void add(int index, T element) {
//        this.acquireWriteLock();
//        try {
//            this.delegate.add(index, element);
//        } finally {
//            this.unlockWriteLock();
//        }
//    }
//
//    @Override
//    public <S> boolean corresponds(OrderedIterable<S> other, Predicate2<? super T, ? super S> predicate) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.corresponds(other, predicate);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public void forEach(int startIndex, int endIndex, Procedure<? super T> procedure) {
//        this.acquireReadLock();
//        try {
//            this.delegate.forEach(startIndex, endIndex, procedure);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public int binarySearch(T key, Comparator<? super T> comparator) {
//        this.acquireReadLock();
//        try {
//            return Collections.binarySearch(this, key, comparator);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public int binarySearch(T key) {
//        this.acquireReadLock();
//        try {
//            return Collections.binarySearch((List<? extends Comparable<? super T>>) this, key);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public void reverseForEachWithIndex(ObjectIntProcedure<? super T> procedure) {
//        throw new UnsupportedOperationException("TODO");
//    }
//
//    @Override
//    public void reverseForEach(Procedure<? super T> procedure) {
//        this.withReadLockRun(() -> delegate.reverseForEach(procedure));
//    }
//
//    @Override
//    public void forEachWithIndex(int fromIndex, int toIndex, ObjectIntProcedure<? super T> objectIntProcedure) {
//        this.withReadLockRun(() -> delegate.forEachWithIndex(fromIndex, toIndex, objectIntProcedure));
//    }
//
//    @Override
//    public void writeExternal(ObjectOutput out) throws IOException {
//        out.writeObject(this.delegate);
//    }
//
//    @Override
//    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
//        this.delegate = (FasterList<T>) in.readObject();
//        this.lock = new ReentrantReadWriteLock();
//    }
//
//    /**
//     * direct access to delegate, use with caution
//     */
//    public MutableList<T> internal() {
//        return delegate;
//    }
//
//    // Exposed for testing
//
////    static final class UntouchableMutableList<T>
////            extends UntouchableMutableCollection<T>
////            implements MutableList<T> {
////        private final MutableList<UntouchableListIterator<T>> requestedIterators = mList();
////        private final MutableList<UntouchableMutableList<T>> requestedSubLists = mList();
////
////        private UntouchableMutableList(MutableList<T> delegate) {
////            this.delegate = delegate;
////        }
////
////        @Override
////        public <V> MutableList<T> distinctBy(Function<? super T, ? extends V> function) {
////            throw new UnsupportedOperationException("TODO");
////        }
////
////
////        @Override
////        public MutableList<T> with(T element) {
////            this.add(element);
////            return this;
////        }
////
////        @Override
////        public MutableList<T> without(T element) {
////            this.remove(element);
////            return this;
////        }
////
////        @Override
////        public MutableList<T> withAll(Iterable<? extends T> elements) {
////            this.addAllIterable(elements);
////            return this;
////        }
////
////        @Override
////        public MutableList<T> withoutAll(Iterable<? extends T> elements) {
////            this.removeAllIterable(elements);
////            return this;
////        }
////
////        @Override
////        public MutableList<T> asSynchronized() {
////            throw new UnsupportedOperationException("Cannot call asSynchronized() on " + this.getClass().getSimpleName());
////        }
////
////        @Override
////        public MutableList<T> asUnmodifiable() {
////            throw new UnsupportedOperationException("Cannot call asUnmodifiable() on " + this.getClass().getSimpleName());
////        }
////
////        @Override
////        public LazyIterable<T> asLazy() {
////            return LazyIterate.adapt(this);
////        }
////
////        @Override
////        public ImmutableList<T> toImmutable() {
////            return this.getDelegate().toImmutable();
////        }
////
////        @Override
////        public MutableList<T> clone() {
////            return this.getDelegate().clone();
////        }
////
////        @Override
////        public <V> MutableList<V> collect(Function<? super T, ? extends V> function) {
////            return this.getDelegate().collect(function);
////        }
////
////        @Override
////        public MutableBooleanList collectBoolean(BooleanFunction<? super T> booleanFunction) {
////            return this.getDelegate().collectBoolean(booleanFunction);
////        }
////
////        @Override
////        public <R extends MutableBooleanCollection> R collectBoolean(BooleanFunction<? super T> booleanFunction, R target) {
////            return this.getDelegate().collectBoolean(booleanFunction, target);
////        }
////
////        @Override
////        public MutableByteList collectByte(ByteFunction<? super T> byteFunction) {
////            return this.getDelegate().collectByte(byteFunction);
////        }
////
////        @Override
////        public <R extends MutableByteCollection> R collectByte(ByteFunction<? super T> byteFunction, R target) {
////            return this.getDelegate().collectByte(byteFunction, target);
////        }
////
////        @Override
////        public MutableCharList collectChar(CharFunction<? super T> charFunction) {
////            return this.getDelegate().collectChar(charFunction);
////        }
////
////        @Override
////        public <R extends MutableCharCollection> R collectChar(CharFunction<? super T> charFunction, R target) {
////            return this.getDelegate().collectChar(charFunction, target);
////        }
////
////        @Override
////        public MutableDoubleList collectDouble(DoubleFunction<? super T> doubleFunction) {
////            return this.getDelegate().collectDouble(doubleFunction);
////        }
////
////        @Override
////        public <R extends MutableDoubleCollection> R collectDouble(DoubleFunction<? super T> doubleFunction, R target) {
////            return this.getDelegate().collectDouble(doubleFunction, target);
////        }
////
////        @Override
////        public MutableFloatList collectFloat(FloatFunction<? super T> floatFunction) {
////            return this.getDelegate().collectFloat(floatFunction);
////        }
////
////        @Override
////        public <R extends MutableFloatCollection> R collectFloat(FloatFunction<? super T> floatFunction, R target) {
////            return this.getDelegate().collectFloat(floatFunction, target);
////        }
////
////        @Override
////        public MutableIntList collectInt(IntFunction<? super T> intFunction) {
////            return this.getDelegate().collectInt(intFunction);
////        }
////
////        @Override
////        public <R extends MutableIntCollection> R collectInt(IntFunction<? super T> intFunction, R target) {
////            return this.getDelegate().collectInt(intFunction, target);
////        }
////
////        @Override
////        public MutableLongList collectLong(LongFunction<? super T> longFunction) {
////            return this.getDelegate().collectLong(longFunction);
////        }
////
////        @Override
////        public <R extends MutableLongCollection> R collectLong(LongFunction<? super T> longFunction, R target) {
////            return this.getDelegate().collectLong(longFunction, target);
////        }
////
////        @Override
////        public MutableShortList collectShort(ShortFunction<? super T> shortFunction) {
////            return this.getDelegate().collectShort(shortFunction);
////        }
////
////        @Override
////        public <R extends MutableShortCollection> R collectShort(ShortFunction<? super T> shortFunction, R target) {
////            return this.getDelegate().collectShort(shortFunction, target);
////        }
////
////        @Override
////        public <V> MutableList<V> flatCollect(Function<? super T, ? extends Iterable<V>> function) {
////            return this.getDelegate().flatCollect(function);
////        }
////
////        @Override
////        public <V> MutableList<V> collectIf(
////                Predicate<? super T> predicate,
////                Function<? super T, ? extends V> function) {
////            return this.getDelegate().collectIf(predicate, function);
////        }
////
////        @Override
////        public <P, V> MutableList<V> collectWith(
////                Function2<? super T, ? super P, ? extends V> function,
////                P parameter) {
////            return this.getDelegate().collectWith(function, parameter);
////        }
////
////        @Override
////        public int detectIndex(Predicate<? super T> predicate) {
////            return this.getDelegate().detectIndex(predicate);
////        }
////
////        @Override
////        public int detectLastIndex(Predicate<? super T> predicate) {
////            return this.getDelegate().detectLastIndex(predicate);
////        }
////
////        @Override
////        public <V> MutableListMultimap<V, T> groupBy(Function<? super T, ? extends V> function) {
////            return this.getDelegate().groupBy(function);
////        }
////
////        @Override
////        public <V> MutableListMultimap<V, T> groupByEach(Function<? super T, ? extends Iterable<V>> function) {
////            return this.getDelegate().groupByEach(function);
////        }
////
////        @Override
////        public <V> MutableMap<V, T> groupByUniqueKey(Function<? super T, ? extends V> function) {
////            return this.getDelegate().groupByUniqueKey(function);
////        }
////
////        @Override
////        public <S> boolean corresponds(OrderedIterable<S> other, Predicate2<? super T, ? super S> predicate) {
////            return this.getDelegate().corresponds(other, predicate);
////        }
////
////        @Override
////        public void forEach(int fromIndex, int toIndex, Procedure<? super T> procedure) {
////            this.getDelegate().forEach(fromIndex, toIndex, procedure);
////        }
////
////        @Override
////        public void reverseForEach(Procedure<? super T> procedure) {
////            this.getDelegate().reverseForEach(procedure);
////        }
////
////        @Override
////        public void forEachWithIndex(int fromIndex, int toIndex, ObjectIntProcedure<? super T> objectIntProcedure) {
////            this.getDelegate().forEachWithIndex(fromIndex, toIndex, objectIntProcedure);
////        }
////
////        @Override
////        public MutableList<T> newEmpty() {
////            return this.getDelegate().newEmpty();
////        }
////
////        @Override
////        public MutableList<T> reject(Predicate<? super T> predicate) {
////            return this.getDelegate().reject(predicate);
////        }
////
////        @Override
////        public MutableList<T> distinct() {
////            return this.getDelegate().distinct();
////        }
////
////        @Override
////        public MutableList<T> distinct(HashingStrategy<? super T> hashingStrategy) {
////            return this.getDelegate().distinct(hashingStrategy);
////        }
////
////        @Override
////        public <P> MutableList<T> rejectWith(
////                Predicate2<? super T, ? super P> predicate,
////                P parameter) {
////            return this.getDelegate().rejectWith(predicate, parameter);
////        }
////
////        @Override
////        public MutableList<T> tap(Procedure<? super T> procedure) {
////            this.forEach(procedure);
////            return this;
////        }
////
////        @Override
////        public MutableList<T> select(Predicate<? super T> predicate) {
////            return this.getDelegate().select(predicate);
////        }
////
////        @Override
////        public <P> MutableList<T> selectWith(
////                Predicate2<? super T, ? super P> predicate,
////                P parameter) {
////            return this.getDelegate().selectWith(predicate, parameter);
////        }
////
////        @Override
////        public PartitionMutableList<T> partition(Predicate<? super T> predicate) {
////            return this.getDelegate().partition(predicate);
////        }
////
////        @Override
////        public <P> PartitionMutableList<T> partitionWith(Predicate2<? super T, ? super P> predicate, P parameter) {
////            return this.getDelegate().partitionWith(predicate, parameter);
////        }
////
////        @Override
////        public <S> MutableList<S> selectInstancesOf(Class<S> clazz) {
////            return this.getDelegate().selectInstancesOf(clazz);
////        }
////
////        @Override
////        public MutableList<T> sortThis() {
////            this.getDelegate().sortThis();
////            return this;
////        }
////
////        @Override
////        public MutableList<T> sortThis(Comparator<? super T> comparator) {
////            this.getDelegate().sortThis(comparator);
////            return this;
////        }
////
////        @Override
////        public MutableList<T> toReversed() {
////            return this.getDelegate().toReversed();
////        }
////
////        @Override
////        public MutableList<T> reverseThis() {
////            this.getDelegate().reverseThis();
////            return this;
////        }
////
////        @Override
////        public MutableList<T> shuffleThis() {
////            this.getDelegate().shuffleThis();
////            return this;
////        }
////
////        @Override
////        public MutableList<T> shuffleThis(Random rnd) {
////            this.getDelegate().shuffleThis(rnd);
////            return this;
////        }
////
////        @Override
////        public MutableStack<T> toStack() {
////            return ArrayStack.newStack(this.delegate);
////        }
////
////        @Override
////        public <V extends Comparable<? super V>> MutableList<T> sortThisBy(Function<? super T, ? extends V> function) {
////            this.getDelegate().sortThisBy(function);
////            return this;
////        }
////
////        @Override
////        public MutableList<T> sortThisByInt(IntFunction<? super T> function) {
////            this.getDelegate().sortThisByInt(function);
////            return this;
////        }
////
////        @Override
////        public MutableList<T> sortThisByBoolean(BooleanFunction<? super T> function) {
////            this.getDelegate().sortThisByBoolean(function);
////            return this;
////        }
////
////        @Override
////        public MutableList<T> sortThisByChar(CharFunction<? super T> function) {
////            this.getDelegate().sortThisByChar(function);
////            return this;
////        }
////
////        @Override
////        public MutableList<T> sortThisByByte(ByteFunction<? super T> function) {
////            this.getDelegate().sortThisByByte(function);
////            return this;
////        }
////
////        @Override
////        public MutableList<T> sortThisByShort(ShortFunction<? super T> function) {
////            this.getDelegate().sortThisByShort(function);
////            return this;
////        }
////
////        @Override
////        public MutableList<T> sortThisByFloat(FloatFunction<? super T> function) {
////            this.getDelegate().sortThisByFloat(function);
////            return this;
////        }
////
////        @Override
////        public MutableList<T> sortThisByLong(LongFunction<? super T> function) {
////            this.getDelegate().sortThisByLong(function);
////            return this;
////        }
////
////        @Override
////        public MutableList<T> sortThisByDouble(DoubleFunction<? super T> function) {
////            this.getDelegate().sortThisByDouble(function);
////            return this;
////        }
////
////        @Override
////        public MutableList<T> take(int count) {
////            return this.getDelegate().take(count);
////        }
////
////        @Override
////        public MutableList<T> takeWhile(Predicate<? super T> predicate) {
////            return this.getDelegate().takeWhile(predicate);
////        }
////
////        @Override
////        public MutableList<T> drop(int count) {
////            return this.getDelegate().drop(count);
////        }
////
////        @Override
////        public MutableList<T> dropWhile(Predicate<? super T> predicate) {
////            return this.getDelegate().dropWhile(predicate);
////        }
////
////        @Override
////        public PartitionMutableList<T> partitionWhile(Predicate<? super T> predicate) {
////            return this.getDelegate().partitionWhile(predicate);
////        }
////
////        @Override
////        public MutableList<T> subList(int fromIndex, int toIndex) {
////            UntouchableMutableList<T> subList = new UntouchableMutableList<>(
////                    this.getDelegate().subList(fromIndex, toIndex));
////            this.requestedSubLists.add(subList);
////            return subList;
////        }
////
////        @Override
////        public Iterator<T> iterator() {
////            UntouchableListIterator<T> iterator = new UntouchableListIterator<>(this.delegate.iterator());
////            this.requestedIterators.add(iterator);
////            return iterator;
////        }
////
////        @Override
////        public void add(int index, T element) {
////            this.getDelegate().add(index, element);
////        }
////
////        @Override
////        public boolean addAll(int index, Collection<? extends T> collection) {
////            return this.getDelegate().addAll(index, collection);
////        }
////
////        @Override
////        public T get(int index) {
////            return this.getDelegate().get(index);
////        }
////
////        @Override
////        public int indexOf(Object o) {
////            return this.getDelegate().indexOf(o);
////        }
////
////        @Override
////        public int lastIndexOf(Object o) {
////            return this.getDelegate().lastIndexOf(o);
////        }
////
////        @Override
////        public ListIterator<T> listIterator() {
////            UntouchableListIterator<T> iterator = new UntouchableListIterator<>(this.getDelegate().listIterator());
////            this.requestedIterators.add(iterator);
////            return iterator;
////        }
////
////        @Override
////        public ListIterator<T> listIterator(int index) {
////            UntouchableListIterator<T> iterator = new UntouchableListIterator<>(this.getDelegate().listIterator(index));
////            this.requestedIterators.add(iterator);
////            return iterator;
////        }
////
////        @Override
////        public T remove(int index) {
////
////            throw new UnsupportedOperationException("TODO"); //should pass through onRemoved
////            //return this.getDelegate().remove(index);
////        }
////
////        @Override
////        public T set(int index, T element) {
////            return this.getDelegate().set(index, element);
////        }
////
////        @Override
////        public <S> MutableList<Pair<T, S>> zip(Iterable<S> that) {
////            return this.getDelegate().zip(that);
////        }
////
////        @Override
////        public MutableList<Pair<T, Integer>> zipWithIndex() {
////            return this.getDelegate().zipWithIndex();
////        }
////
////        @Override
////        public LazyIterable<T> asReversed() {
////            return ReverseIterable.adapt(this);
////        }
////
////        @Override
////        public ParallelListIterable<T> asParallel(ExecutorService executorService, int batchSize) {
////            return new ListIterableParallelIterable<>(this, executorService, batchSize);
////        }
////
////        @Override
////        public int binarySearch(T key, Comparator<? super T> comparator) {
////            return Collections.binarySearch(this, key, comparator);
////        }
////
////        @Override
////        public int binarySearch(T key) {
////            return Collections.binarySearch((List<? extends Comparable<? super T>>) this, key);
////        }
////
////        public void becomeUseless() {
////            this.delegate = null;
////            this.requestedSubLists.each(UntouchableMutableList::becomeUseless);
////            this.requestedIterators.each(UntouchableListIterator::becomeUseless);
////        }
////
////        private MutableList<T> getDelegate() {
////            return (MutableList<T>) this.delegate;
////        }
////    }
//
//    private static final class UntouchableListIterator<T>
//            implements ListIterator<T> {
//        private Iterator<T> delegate;
//
//        private UntouchableListIterator(Iterator<T> newDelegate) {
//            this.delegate = newDelegate;
//        }
//
//        @Override
//        public void add(T o) {
//            ((ListIterator<T>) this.delegate).add(o);
//        }
//
//        @Override
//        public boolean hasNext() {
//            return this.delegate.hasNext();
//        }
//
//        @Override
//        public boolean hasPrevious() {
//            return ((ListIterator<T>) this.delegate).hasPrevious();
//        }
//
//        @Override
//        public T next() {
//            return this.delegate.next();
//        }
//
//        @Override
//        public int nextIndex() {
//            return ((ListIterator<T>) this.delegate).nextIndex();
//        }
//
//        @Override
//        public T previous() {
//            return ((ListIterator<T>) this.delegate).previous();
//        }
//
//        @Override
//        public int previousIndex() {
//            return ((ListIterator<T>) this.delegate).previousIndex();
//        }
//
//        @Override
//        public void remove() {
//            throw new UnsupportedOperationException("TODO"); //should pass through onRemoved
//            //this.delegate.remove();
//        }
//
//        @Override
//        public void set(T o) {
//            ((ListIterator<T>) this.delegate).set(o);
//        }
//
//        public void becomeUseless() {
//            this.delegate = null;
//        }
//    }
//
//    @Override
//    public int detectIndex(Predicate<? super T> predicate) {
//        this.acquireReadLock();
//        try {
//            return this.getDelegate().detectIndex(predicate);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public int detectLastIndex(Predicate<? super T> predicate) {
//        this.acquireReadLock();
//        try {
//            return this.getDelegate().detectLastIndex(predicate);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public <V> MutableListMultimap<V, T> groupBy(Function<? super T, ? extends V> function) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.groupBy(function);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public <V> MutableListMultimap<V, T> groupByEach(Function<? super T, ? extends Iterable<V>> function) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.groupByEach(function);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public <V> MutableMap<V, T> groupByUniqueKey(Function<? super T, ? extends V> function) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.groupByUniqueKey(function);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public <S> MutableList<Pair<T, S>> zip(Iterable<S> that) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.zip(that);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public MutableList<Pair<T, Integer>> zipWithIndex() {
//        this.acquireReadLock();
//        try {
//            return this.delegate.zipWithIndex();
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public MutableList<T> toReversed() {
//        this.acquireReadLock();
//        try {
//            return this.delegate.toReversed();
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public MutableList<T> reverseThis() {
//        this.acquireWriteLock();
//        try {
//            this.delegate.reverseThis();
//            return this;
//        } finally {
//            this.unlockWriteLock();
//        }
//    }
//
//    @Override
//    public MutableList<T> shuffleThis() {
//        this.acquireWriteLock();
//        try {
//            this.delegate.shuffleThis();
//            return this;
//        } finally {
//            this.unlockWriteLock();
//        }
//    }
//
//    @Override
//    public MutableList<T> shuffleThis(Random rnd) {
//        this.acquireWriteLock();
//        try {
//            this.delegate.shuffleThis(rnd);
//            return this;
//        } finally {
//            this.unlockWriteLock();
//        }
//    }
//
//    @Override
//    public MutableStack<T> toStack() {
//        this.acquireReadLock();
//        try {
//            return this.delegate.toStack();
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public RichIterable<RichIterable<T>> chunk(int size) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.chunk(size);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public MutableList<T> take(int count) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.take(count);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public MutableList<T> takeWhile(Predicate<? super T> predicate) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.takeWhile(predicate);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public MutableList<T> drop(int count) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.drop(count);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public MutableList<T> dropWhile(Predicate<? super T> predicate) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.dropWhile(predicate);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public PartitionMutableList<T> partitionWhile(Predicate<? super T> predicate) {
//        this.acquireReadLock();
//        try {
//            return this.delegate.partitionWhile(predicate);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public LazyIterable<T> asReversed() {
//        this.acquireReadLock();
//        try {
//            return ReverseIterable.adapt(this);
//        } finally {
//            this.unlockReadLock();
//        }
//    }
//
//    @Override
//    public ParallelListIterable<T> asParallel(ExecutorService executorService, int batchSize) {
//        return new MultiReaderParallelListIterable<>(this.delegate.asParallel(executorService, batchSize), this.lock);
//    }
//}
//
