package jcog.data.array;

/*		 
 * Copyright (C) 2002-2010 Sebastiano Vigna 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */


import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.IntToShortFunction;

import java.util.Random;

import static com.google.common.math.IntMath.factorial;

/**
 * A class providing static methods and objects that do useful things with arrays.
 * <p>
 * <p>In addition to commodity methods, this class contains {@link Swapper}-based implementations
 * of {@linkplain #quickSort(int, int, IntComparator, Swapper) quicksort} and of
 * a stable, in-place {@linkplain #mergeSort(int, int, IntComparator, Swapper) mergesort}. These
 * generic sorting methods can be used to sort any kind of list, but they find their natural
 * usage, for instance, in sorting arrays in parallel.
 *
 * @see Arrays
 */

public enum Arrays {
    ;


    /**
     * Ensures that a range given by its first (inclusive) and last (exclusive) elements fits an array of given length.
     * <p>
     * <P>This method may be used whenever an array range check is needed.
     *
     * @param arrayLength an array length.
     * @param from        a start index (inclusive).
     * @param to          an end index (inclusive).
     * @throws IllegalArgumentException       if <code>from</code> is greater than <code>to</code>.
     * @throws ArrayIndexOutOfBoundsException if <code>from</code> or <code>to</code> are greater than <code>arrayLength</code> or negative.
     */
    public static void ensureFromTo(int arrayLength, int from, int to) {
        if (from < 0) throw new ArrayIndexOutOfBoundsException("Start index (" + from + ") is negative");
        if (from > to)
            throw new IllegalArgumentException("Start index (" + from + ") is greater than end index (" + to + ')');
        if (to > arrayLength)
            throw new ArrayIndexOutOfBoundsException("End index (" + to + ") is greater than array length (" + arrayLength + ')');
    }

    /**
     * Ensures that a range given by an offset and a length fits an array of given length.
     * <p>
     * <P>This method may be used whenever an array range check is needed.
     *
     * @param arrayLength an array length.
     * @param offset      a start index for the fragment
     * @param length      a length (the number of elements in the fragment).
     * @throws IllegalArgumentException       if <code>length</code> is negative.
     * @throws ArrayIndexOutOfBoundsException if <code>offset</code> is negative or <code>offset</code>+<code>length</code> is greater than <code>arrayLength</code>.
     */
    public static void ensureOffsetLength(int arrayLength, int offset, int length) {
        if (offset < 0) throw new ArrayIndexOutOfBoundsException("Offset (" + offset + ") is negative");
        if (length < 0) throw new IllegalArgumentException("Length (" + length + ") is negative");
        if (offset + length > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Last index (" + (offset + length) + ") is greater than array length (" + arrayLength + ')');
    }

    private static final int SMALL = 7;
    private static final int MEDIUM = 40;

    /**
     * Transforms two consecutive sorted ranges into a single sorted range. The initial ranges are
     * <code>[first, middle)</code> and <code>[middle, last)</code>, and the resulting range is
     * <code>[first, last)</code>. Elements in the first input range will precede equal elements in
     * the second.
     */
    private static void inPlaceMerge(int from, int mid, int to, IntComparator comp, Swapper swapper) {
        if (from >= mid || mid >= to) return;
        if (to - from == 2) {
            if (comp.compare(mid, from) < 0) {
                swapper.swap(from, mid);
            }
            return;
        }
        int firstCut;
        int secondCut;
        if (mid - from > to - mid) {
            firstCut = from + (mid - from) / 2;
            secondCut = lowerBound(mid, to, firstCut, comp);
        } else {
            secondCut = mid + (to - mid) / 2;
            firstCut = upperBound(from, mid, secondCut, comp);
        }

        int first2 = firstCut;
        int middle2 = mid;
        int last2 = secondCut;
        if (middle2 != first2 && middle2 != last2) {
            int first1 = first2;
            int last1 = middle2;
            while (first1 < --last1)
                swapper.swap(first1++, last1);
            first1 = middle2;
            last1 = last2;
            while (first1 < --last1)
                swapper.swap(first1++, last1);
            first1 = first2;
            last1 = last2;
            while (first1 < --last1)
                swapper.swap(first1++, last1);
        }

        mid = firstCut + (secondCut - mid);
        inPlaceMerge(from, firstCut, mid, comp, swapper);
        inPlaceMerge(mid, secondCut, to, comp, swapper);
    }

    /**
     * Performs a binary search on an already-sorted range: finds the first position where an
     * element can be inserted without violating the ordering. Sorting is by a user-supplied
     * comparison function.
     *
     * @param mid      Beginning of the range.
     * @param to       One past the end of the range.
     * @param firstCut Element to be searched for.
     * @param comp     Comparison function.
     * @return The largest index i such that, for every j in the range <code>[first, i)</code>,
     * <code>comp.apply(array[j], x)</code> is <code>true</code>.
     */
    private static int lowerBound(int mid, int to, int firstCut, IntComparator comp) {
        // if (comp==null) throw new NullPointerException();
        int len = to - mid;
        while (len > 0) {
            int half = len / 2;
            int middle = mid + half;
            if (comp.compare(middle, firstCut) < 0) {
                mid = middle + 1;
                len -= half + 1;
            } else {
                len = half;
            }
        }
        return mid;
    }

    /**
     * Returns the index of the median of the three indexed chars.
     */
    private static int med3(int a, int b, int c, IntComparator comp) {
        int ab = comp.compare(a, b);
        int ac = comp.compare(a, c);
        int bc = comp.compare(b, c);
        return (ab < 0 ?
                (bc < 0 ? b : ac < 0 ? c : a) :
                (bc > 0 ? b : ac > 0 ? c : a));
    }

    /**
     * Sorts the specified range of elements using the specified swapper and according to the order induced by the specified
     * comparator using mergesort.
     * <p>
     * <p>This sort is guaranteed to be <i>stable</i>: equal elements will not be reordered as a result
     * of the sort. The sorting algorithm is an in-place mergesort that is significantly slower than a
     * standard mergesort, but does not allocate additional memory.
     *
     * @param from    the index of the first element (inclusive) to be sorted.
     * @param to      the index of the last element (exclusive) to be sorted.
     * @param c       the comparator to determine the order of the generic data (arguments are positions).
     * @param swapper an object that knows how to swap the elements at any two positions.
     */
    public static void mergeSort(int from, int to, IntComparator c, Swapper swapper) {
        /*
		 * We retain the same method signature as quickSort. Given only a comparator and swapper we
		 * do not know how to copy and move elements from/to temporary arrays. Hence, in contrast to
		 * the JDK mergesorts this is an "in-place" mergesort, i.e. does not allocate any temporary
		 * arrays. A non-inplace mergesort would perhaps be faster in most cases, but would require
		 * non-intuitive delegate objects...
		 */
        int length = to - from;

        // Insertion sort on smallest arrays
        if (length < SMALL) {
            for (int i = from; i < to; i++) {
                for (int j = i; j > from && (c.compare(j - 1, j) > 0); j--) {
                    swapper.swap(j, j - 1);
                }
            }
            return;
        }

        // Recursively sort halves
        int mid = (from + to) >>> 1;
        mergeSort(from, mid, c, swapper);
        mergeSort(mid, to, c, swapper);

        // If list is already sorted, nothing left to do. This is an
        // optimization that results in faster sorts for nearly ordered lists.
        if (c.compare(mid - 1, mid) <= 0) return;

        // Merge sorted halves
        inPlaceMerge(from, mid, to, c, swapper);
    }

    /**
     * Sorts the specified range of elements using the specified swapper and according to the order induced by the specified
     * comparator using quicksort.
     * <p>
     * <p>The sorting algorithm is a tuned quicksort adapted from Jon L. Bentley and M. Douglas
     * McIlroy, &ldquo;Engineering a Sort Function&rdquo;, <i>Software: Practice and Experience</i>, 23(11), pages
     * 1249&minus;1265, 1993.
     *
     * @param from    the index of the first element (inclusive) to be sorted.
     * @param to      the index of the last element (exclusive) to be sorted.
     * @param comp    the comparator to determine the order of the generic data.
     * @param swapper an object that knows how to swap the elements at any two positions.
     */
    public static void quickSort(int from, int to, IntComparator comp, Swapper swapper) {
        int len = to - from;
        // Insertion sort on smallest arrays
        if (len < SMALL) {
            for (int i = from; i < to; i++)
                for (int j = i; j > from && (comp.compare(j - 1, j) > 0); j--) {
                    swapper.swap(j, j - 1);
                }
            return;
        }

        // Choose a partition element, v
        int m = from + len / 2; // Small arrays, middle element
        if (len > SMALL) {
            int l = from;
            int n = to - 1;
            if (len > MEDIUM) { // Big arrays, pseudomedian of 9
                int s = len / 8;
                l = med3(l, l + s, l + 2 * s, comp);
                m = med3(m - s, m, m + s, comp);
                n = med3(n - 2 * s, n - s, n, comp);
            }
            m = med3(l, m, n, comp); // Mid-size, med of 3
        }
        // int v = x[m];

        int a = from;
        int b = a;
        int c = to - 1;
        // Establish Invariant: v* (<v)* (>v)* v*
        int d = c;
        while (true) {
            int comparison;
            while (b <= c && ((comparison = comp.compare(b, m)) <= 0)) {
                if (comparison == 0) {
                    if (a == m) m = b; // moving target; DELTA to JDK !!!
                    else if (b == m) m = a; // moving target; DELTA to JDK !!!
                    swapper.swap(a++, b);
                }
                b++;
            }
            while (c >= b && ((comparison = comp.compare(c, m)) >= 0)) {
                if (comparison == 0) {
                    if (c == m) m = d; // moving target; DELTA to JDK !!!
                    else if (d == m) m = c; // moving target; DELTA to JDK !!!
                    swapper.swap(c, d--);
                }
                c--;
            }
            if (b > c) break;
            if (b == m) m = d; // moving target; DELTA to JDK !!!
            else if (c == m) m = c; // moving target; DELTA to JDK !!!
            swapper.swap(b++, c--);
        }

        // Swap partition elements back to middle
        int s;
        int n = to;
        s = Math.min(a - from, b - a);
        vecSwap(swapper, from, b - s, s);
        s = Math.min(d - c, n - d - 1);
        vecSwap(swapper, b, n - s, s);

        // Recursively sort non-partition-elements
        if ((s = b - a) > 1) quickSort(from, from + s, comp, swapper);
        if ((s = d - c) > 1) quickSort(n - s, n, comp, swapper);
    }

    /**
     * Performs a binary search on an already sorted range: finds the last position where an element
     * can be inserted without violating the ordering. Sorting is by a user-supplied comparison
     * function.
     *
     * @param from      Beginning of the range.
     * @param mid       One past the end of the range.
     * @param secondCut Element to be searched for.
     * @param comp      Comparison function.
     * @return The largest index i such that, for every j in the range <code>[first, i)</code>,
     * <code>comp.apply(x, array[j])</code> is <code>false</code>.
     */
    private static int upperBound(int from, int mid, int secondCut, IntComparator comp) {
        // if (comp==null) throw new NullPointerException();
        int len = mid - from;
        while (len > 0) {
            int half = len / 2;
            int middle = from + half;
            if (comp.compare(secondCut, middle) < 0) {
                len = half;
            } else {
                from = middle + 1;
                len -= half + 1;
            }
        }
        return from;
    }

    /**
     * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
     */
    private static void vecSwap(Swapper swapper, int from, int l, int s) {
        for (int i = 0; i < s; i++, from++, l++) swapper.swap(from, l);
    }

    public static void shuffle(int[] array, Random random) {
        shuffle(array, array.length, random);
    }

    public static void shuffle(int[] array, int len, Random random) {

        //probabality for no shuffle at all:
        if (random.nextInt(factorial(len)) == 0) return;

        for (int i = len; i > 1; i--) {
            int a = i - 1;
            int b = random.nextInt(i);
            if (b != a) {
                int t = array[b];
                array[b] = array[a];
                array[a] = t;
            }
        }
    }

    public static void shuffle(byte[] array, int len, Random random) {

        //probabality for no shuffle at all:
        if (random.nextInt(factorial(len)) == 0) return;

        for (int i = len; i > 1; i--) {
            int a = i - 1;
            int b = random.nextInt(i);
            if (b != a) {
                byte t = array[b];
                array[b] = array[a];
                array[a] = t;
            }
        }
    }

    public static void sort(int[] a, int left, int right, IntToShortFunction v) {
//        // Use counting sort on large arrays
//        if (right - left > COUNTING_SORT_THRESHOLD_FOR_BYTE) {
//            int[] count = new int[NUM_BYTE_VALUES];
//
//            for (int i = left - 1; ++i <= right;
//                 count[a[i] - Byte.MIN_VALUE]++
//                    )
//                ;
//            for (int i = NUM_BYTE_VALUES, k = right + 1; k > left; ) {
//                while (count[--i] == 0) ;
//                byte value = (byte) (i + Byte.MIN_VALUE);
//                int s = count[i];
//
//                do {
//                    a[--k] = value;
//                } while (--s > 0);
//            }
//        } else { // Use insertion sort on small arrays
        for (int i = left, j = i; i < right; j = ++i) {
            int ai = a[i + 1];
            while (v.valueOf(ai) > v.valueOf(a[j])) {
                a[j + 1] = a[j];
                if (j-- == left)
                    break;
            }
            a[j + 1] = ai;
        }
//        }
    }

    public static void sort(int[] a, IntToFloatFunction v) {
        sort(a, 0, a.length-1, v);
    }
    public static void sort(int[] a, int left, int right /* inclusive */, IntToFloatFunction v) {
//        // Use counting sort on large arrays
//        if (right - left > COUNTING_SORT_THRESHOLD_FOR_BYTE) {
//            int[] count = new int[NUM_BYTE_VALUES];
//
//            for (int i = left - 1; ++i <= right;
//                 count[a[i] - Byte.MIN_VALUE]++
//                    )
//                ;
//            for (int i = NUM_BYTE_VALUES, k = right + 1; k > left; ) {
//                while (count[--i] == 0) ;
//                byte value = (byte) (i + Byte.MIN_VALUE);
//                int s = count[i];
//
//                do {
//                    a[--k] = value;
//                } while (--s > 0);
//            }
//        } else { // Use insertion sort on small arrays
        for (int i = left, j = i; i < right; j = ++i) {
            int ai = a[i + 1];
            while (v.valueOf(ai) > v.valueOf(a[j])) {
                a[j + 1] = a[j];
                if (j-- == left)
                    break;
            }
            a[j + 1] = ai;
        }
//        }
    }
}