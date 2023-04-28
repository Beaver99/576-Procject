import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

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
        Index[] res = new Index[]{};
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
        // just a demo
        return new Index[]{new Index(0, Level.scene)};
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
