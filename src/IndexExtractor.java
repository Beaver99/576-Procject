import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class IndexExtractor {
    public static ArrayList<Index> mockExtractIndex(File rgbs, File audio) {
        // for testing
        ArrayList<Index> idxs = new ArrayList<>();
        idxs.add(new Index(0, Level.scene));

        idxs.add(new Index(100, Level.scene));
        idxs.add(new Index(200, Level.shot));
        idxs.add(new Index(250, Level.shot));
        idxs.add(new Index(300, Level.subshot));

        idxs.add(new Index(1000, Level.scene));
        idxs.add(new Index(2000, Level.shot));
        idxs.add(new Index(3000, Level.subshot));
        idxs.add(new Index(4000, Level.shot));
        idxs.add(new Index(4500, Level.shot));

        idxs.add(new Index(4800, Level.scene));

        idxs.add(new Index(5000, Level.scene));
        idxs.add(new Index(5300, Level.shot));
        idxs.add(new Index(5315, Level.subshot));
        idxs.add(new Index(5330, Level.subshot));
        return idxs;
    }

    public static ArrayList<Index> extractIndex(File rgbs, File audio) {
        ArrayList<Index> idxs = new ArrayList<>();
        int width = 480; // width of the video frames
        int height = 270; // height of the video frames
        int fps = 30; // frames per second of the video
        long numFrames = rgbs.length();

        RandomAccessFile raf = null;
        FileChannel channel = null;
        ByteBuffer buffer = null;
        try {
            raf = new RandomAccessFile(rgbs, "r");
            channel = raf.getChannel();
            buffer = ByteBuffer.allocate(width * height * 3);
            int[][] prevFrame = new int[width * height][];
            int[][] currFrame = new int[width * height][];

            // step 2
            for (int i = 0; i < numFrames; i++) {
                buffer.clear();
                channel.read(buffer);
                buffer.rewind();
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int r = buffer.get() & 0xff;
                        int g = buffer.get() & 0xff;
                        int b = buffer.get() & 0xff;

                        // use luv color space instead of rgb color space
                        currFrame[y * width + x] = rgb2luv(r, g, b);

                        if (i != 0) {

                        } else {

                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (channel != null) {
                    channel.close();
                }
                if (raf != null) {
                    raf.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return idxs;
    }

    // #todo
    // get the histogram-difference array for any 2 consecutive frames.
    public static long histogramDifference() {
        return 1;
    }

    //#todo
    // rgb to luv color space
    public static int[] rgb2luv(int r, int g, int b) {
        return new int[]{0, 0, 0};
    }

}
