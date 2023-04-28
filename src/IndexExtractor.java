import java.io.File;

public class IndexExtractor {
    public static Index[] mockExtractIndex(File rgbs, File audio) {
        // for testing
        return new Index[]{new Index(0, Level.scene),
                new Index(0, Level.scene),
                new Index(60, Level.scene),
                new Index(120, Level.shot),
                new Index(250, Level.shot),
                new Index(400, Level.scene),
                new Index(500, Level.shot),
                new Index(600, Level.subshot),
                new Index(700, Level.subshot),
                new Index(1000, Level.shot),
                new Index(10000, Level.scene),
                new Index(11000, Level.shot),
                new Index(11110, Level.shot),
                new Index(11199, Level.subshot)
        };
    }

    public static Index[] extractIndex(File rgbs, File audio) {
        // just a demo
        return new Index[]{new Index(0, Level.scene)};
    }

    // get the histogram-difference array for any 2 consecutive frames.
//    public static long[] histogramDifference(File rgbs, )
}
