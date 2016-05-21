package nars.nal.meta;

import com.google.common.base.Joiner;
import com.gs.collections.api.block.function.primitive.FloatFunction;
import com.gs.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import nars.term.Term;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.jetbrains.annotations.NotNull;
import org.magnos.trie.Trie;
import org.magnos.trie.TrieNode;
import org.magnos.trie.TrieSequencer;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;



/** indexes sequences of (a perfectly-hashable fixed number
 * of unique) terms in a magnos trie */
abstract public class TermTrie<K extends Term, V> {

    @NotNull
    public final Trie<List<K>, V> trie;

    public void printSummary() {
        printSummary(System.out);
    }

    public void printSummary(@NotNull PrintStream out) {
        printSummary(trie.root, out);
    }


    public TermTrie(@NotNull Iterable<V> R) {
        super();

        ObjectIntHashMap<Term> conds = new ObjectIntHashMap<>();

        trie = new Trie(new TrieSequencer<List<K>>() {

            @Override
            public int matches(@NotNull List<K> sequenceA, int indexA, @NotNull List<K> sequenceB, int indexB, int count) {
                for (int i = 0; i < count; i++) {
                    K a = sequenceA.get(i + indexA);
                    K b = sequenceB.get(i + indexB);
                    if (!a.equals(b))
                        return i;
                }

                return count;
            }

            @Override
            public int lengthOf(@NotNull List<K> sequence) {
                return sequence.size();
            }

            @Override
            public int hashOf(@NotNull List<K> sequence, int index) {
                //return sequence.get(index).hashCode();

                Term pp = sequence.get(index);
                return conds.getIfAbsentPutWithKey(pp, (p) -> 1 + conds.size());
            }
        });

        R.forEach(this::index);
    }

    /** called for each item on insert */
    abstract public void index(V v);

    public static <A, B> void printSummary(@NotNull TrieNode<List<A>,B> node, @NotNull PrintStream out) {

        node.forEach(n -> {
            List<A> seq = n.seq();

            int from = n.start();

            out.print(n.childCount() + "|" + n.getSize() + "  ");

            indent(from * 4);

            out.println(Joiner.on(" , ").join( seq.subList(from, n.end())));

            printSummary(n, out);
        });



    }

    public SummaryStatistics costAnalyze(FloatFunction<K> costFn, PrintStream o) {

        SummaryStatistics termCost = new SummaryStatistics();
        SummaryStatistics sequenceLength = new SummaryStatistics();
        SummaryStatistics branchFanOut = new SummaryStatistics();
        SummaryStatistics endDepth = new SummaryStatistics();
        int[] currentDepth = new int[1];

        costAnalyze(costFn, termCost, sequenceLength, branchFanOut, endDepth, currentDepth, trie.root);

        if (o!=null) {
            o.println("termCost: " + s(termCost));
            o.println("sequenceLength: " + s(sequenceLength));
            o.println("branchFanOut: " + s(branchFanOut));
            o.println("endDepth: " + s(endDepth));
        }
        return termCost;
    }

    private String s(SummaryStatistics s) {
        return s.getSummary().toString().replace('\n', ' ').replace("StatisticalSummaryValues: ", "");
    }

    public static <K,V> void costAnalyze(FloatFunction<K> costFn, SummaryStatistics termCost, SummaryStatistics sequenceLength, SummaryStatistics branchFanOut, SummaryStatistics endDepth, int[] currentDepth, TrieNode<List<K>, V> root) {

        branchFanOut.addValue(root.childCount());

        root.forEach(n -> {
            List<K> seq = n.seq();

            int from = n.start();

            //out.print(n.childCount() + "|" + n.getSize() + "  ");

            List<K> sqn = seq.subList(from, n.end());
            sequenceLength.addValue(sqn.size());

            for (K k : sqn) {
                termCost.addValue( costFn.floatValueOf(k) );
            }

            //indent(from * 4);
            currentDepth[0]++;

            costAnalyze(costFn, termCost, sequenceLength, branchFanOut, endDepth, currentDepth, n);

            currentDepth[0]--;

            endDepth.addValue(currentDepth[0]);
        });
    }

    public static void indent(int amount) {
        for (int i = 0; i < amount; i++) {
            System.out.print(' ');
        }
    }

    public String getSummary() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        printSummary(new PrintStream(baos));
        return baos.toString();
    }
}
