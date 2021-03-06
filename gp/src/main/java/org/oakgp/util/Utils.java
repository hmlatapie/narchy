/*
 * Copyright 2015 S. Webber
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
package org.oakgp.util;

import org.oakgp.Type;
import org.oakgp.node.ConstantNode;
import org.oakgp.node.Node;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.groupingBy;
import static org.oakgp.Type.*;

/**
 * Utility methods that support the functionality provided by the rest of the framework.
 */
public enum Utils { ;
    /**
     * Represents the boolean value {@code true}.
     */
    public static final ConstantNode TRUE_NODE = new ConstantNode(TRUE, booleanType());
    /**
     * Represents the boolean value {@code false}.
     */
    public static final ConstantNode FALSE_NODE = new ConstantNode(FALSE, booleanType());



    /**
     * Returns an array consisting of a {@code ConstantNode} instance for each of the possible values of the specified enum.
     *
     * @param e the enum that the {@code ConstantNode} instances should wrap
     * @param t the {@code Type} that should be associated with the {@code ConstantNode} instances
     */
    public static ConstantNode[] createEnumConstants(Class<? extends Enum<?>> e, Type t) {
        Enum<?>[] enumConstants = e.getEnumConstants();
        ConstantNode[] constants = new ConstantNode[enumConstants.length];
        for (int i = 0; i < enumConstants.length; i++) {
            constants[i] = new ConstantNode(enumConstants[i], t);
        }
        return constants;
    }

    /**
     * Returns an array consisting of a {@code ConstantNode} instance for each of the integer values in the specified range.
     *
     * @param minInclusive the minimum value (inclusive) to be represented by a {@code ConstantNode} in the returned array
     * @param maxInclusive the minimum value (inclusive) to be represented by a {@code ConstantNode} in the returned array
     */
    public static ConstantNode[] createIntegerConstants(int minInclusive, int maxInclusive) {
        ConstantNode[] constants = new ConstantNode[maxInclusive - minInclusive + 1];
        for (int n = minInclusive, i = 0; n <= maxInclusive; i++, n++) {
            constants[i] = new ConstantNode(n, integerType());
        }
        return constants;
    }
    public static ConstantNode[] createDoubleConstants(int minInclusive, int maxInclusive) {
        ConstantNode[] constants = new ConstantNode[maxInclusive - minInclusive + 1];
        for (int n = minInclusive, i = 0; n <= maxInclusive; i++, n++) {
            constants[i] = new ConstantNode((double)n, doubleType());
        }
        return constants;
    }

    /**
     * Creates an array of the specified size and assigns the result of {@link Type#integerType()} to each element.
     */
    public static Type[] createIntegerTypeArray(int size) {
        Type[] array = new Type[size];
        Type type = integerType();
        Arrays.fill(array, type);
        return array;
    }

    /**
     * Returns a map grouping the specified nodes by their {@code Type}.
     */
    public static <T extends Node> Map<Type, List<T>> groupByType(T[] nodes) {
        return groupBy(nodes, Node::returnType);
    }

    /**
     * Returns a map grouping the specified values according to the specified classification function.
     *
     * @param values     the values to group
     * @param valueToKey the classification function used to group values
     */
    public static <K, V> Map<K, List<V>> groupBy(V[] values, Function<V, K> valueToKey) {
        Map<K, List<V>> nodesByType = Arrays.stream(values).collect(groupingBy(valueToKey));
        makeValuesImmutable(nodesByType);
        return nodesByType;
    }

    /**
     * Replaces each {@code List} stored as a value in the specified {@code Map} with an immutable version.
     */
    private static <K, V> void makeValuesImmutable(Map<K, List<V>> map) {
        for (Map.Entry<K, List<V>> e : map.entrySet()) {
            map.put(e.getKey(), unmodifiableList(e.getValue()));
        }
    }

    /**
     * Returns randomly selected index of a node from the specified tree.
     */
    public static int selectSubNodeIndex(Random random, Node tree) {
        int nodeCount = tree.size();
        if (nodeCount == 1) {
            // will get here if and only if 'tree' is a terminal (i.e. variable or constant) rather than a function node
            return 0;
        } else {
            return selectSubNodeIndex(random, nodeCount);
        }
    }

    /**
     * Returns a int value between 0 (inclusive) and the specified {@code nodeCount} value minus 1 (exclusive).
     */
    public static int selectSubNodeIndex(Random random, int nodeCount) {
        // Note: -1 to avoid selecting root node
        return random.nextInt(nodeCount - 1);
    }

    /**
     * Returns a copy of the specified array.
     */
    public static <T> T[] copyOf(T[] original) {
        return original.clone();
        //return Arrays.copyOf(original, original.length);
    }
}
