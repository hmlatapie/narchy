package spacegraph.space3d.test;

import com.google.common.graph.SuccessorsFunction;
import spacegraph.space3d.widget.SimpleGraph;

import java.io.File;
import java.util.List;

public class SimpleTreeTest {
    /** from: guava */
    static final SuccessorsFunction<File> FILE_TREE = path ->
            fileTreeChildren(path);

    /** from: guava */
    private static Iterable<File> fileTreeChildren(File dir) {
        if (dir.isDirectory()) {
            return List.of(dir.listFiles());
        }
        return List.of();
    }

    public static void main(String[] args)  {


        SimpleGraph sg = new SimpleGraph();
        sg.commit(FILE_TREE, new File(
                "nal/src/test/"));

        sg.show(800, 600, false);

    }

}
