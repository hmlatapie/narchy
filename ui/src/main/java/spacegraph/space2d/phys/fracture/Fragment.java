package spacegraph.space2d.phys.fracture;

import spacegraph.space2d.phys.common.PlatformMathUtils;
import spacegraph.util.math.Tuple2f;

/**
 * Polygon pre voronoi diagram. Funguje ako ArrayList 2D bodov typu Point2D,
 * ktory potom zotriedim podla ohniska na konvexny polygon. V pripade potreby si
 * moze uzivatel dodefinovat dalsie funkcie - oddedit od polygonu a nadefinovat
 * si funkcie na vypocet obsahu, alebo taziska. Jedna sa o specificky pripad polygonu.
 * Reprezentuje uz konkretny ulomok povodneho telesa.
 *
 * @author Marek Benovic
 * @version 1.0
 */
public class Fragment extends Polygon {
    /**
     * Inicializuje prazdny fragment
     */
    public Fragment() {
    }
    public Fragment(int sides) {
        super(sides);
    }

//    /**
//     * Inicializuje fragment na zaklade vstupneho pola (priradi referenciu).
//     *
//     * @param ar
//     */
//    public Fragment(Tuple2f[] ar) {
//        super(ar);
//    }

    /**
     * Ohnisko fragmentu
     */
    public Tuple2f focus;

    /**
     * Pomocna premenna pre vypocet do geometry kniznice.
     */
    public boolean visited;

    /**
     * Zotriedi konvexny polygon podla bodu focus na zaklade uhlov jednotlivych
     * vrcholov
     */
    public void resort() {
        int size = size();
        double[] comparer = new double[size];
        for (int i = 0; i != size; ++i) {
            comparer[i] = PlatformMathUtils.angle(get(i), focus);
        }
        for (int i = 0; i != size; ++i) {
            int maxIndex = i;
            for (int j = i + 1; j != size; ++j) {
                if (comparer[j] < comparer[maxIndex]) {
                    maxIndex = j;
                }
            }
            double swap = comparer[i];
            comparer[i] = comparer[maxIndex];
            comparer[maxIndex] = swap;
            swap(i, maxIndex);
        }
    }

//    /**
//     * Zotriedi vrcholy polygonu do konvexneho polygonu, ako idu za sebou.
//     * Triedi podla uhlu, aky zviera usecka tvoriaca bodmi focus a lubovolny
//     * vrchol polygonu. Polygony su vacsinou velmi male, cca 8 bodov, preto
//     * je vyuzivany selected sort ako najrychlejsi algoritmus na data takehoto
//     * typu.
//     *
//     * @param focus Vlozi vnutorny bod, podla ktoreho zotriedi polygon - podla
//     *              uhlu spojnice daneho bodu a parametra.
//     */
//    void sort(Tuple2f focus) {
//        this.focus = focus;
//        resort();
//    }

    /**
     * Vymeni 2 vrcholy polygonu
     *
     * @param i
     * @param j
     */
    private void swap(int i, int j) {
        Tuple2f[] a = this.array;
        Tuple2f item = a[i];
        a[i] = a[j];
        a[j] = item;
    }
}
