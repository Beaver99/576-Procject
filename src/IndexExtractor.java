import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;

public class IndexExtractor {
    static int width = 480; // width of the video frames
    static int height = 270; // height of the video frames
    static int fps = 30; // frames per second of the video

    static long prev_Cut_idx = 0;
    static int[] prev_Scene = new int[width * height];
    static int[] prev_Shot = new int[width * height];
    static int[] prev_Subshot = new int[width * height];
    static int[] currFrame = new int[width * height];


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

    static float getThreshold(BufferedImage image, int percentile) {
        int[] histogram = new int[256];
        int pixelCount = image.getWidth() * image.getHeight();

        // Calculate the histogram of pixel intensities
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color color = new Color(image.getRGB(x, y));
                int lum = rgb2lum(color.getRed(), color.getGreen(), color.getBlue());
                histogram[lum]++;
            }
        }

        // Determine the pixel intensity corresponding to the given percentile
        int sum = 0;
        for (int i = 0; i < histogram.length; i++) {
            sum += histogram[i];
            if (sum >= pixelCount * percentile / 100.0) {
                return i / 255.0f;
            }
        }

        // If the given percentile is too high, return the maximum pixel intensity
        return 1.0f;
    }

    public static ArrayList<Index> extractIndex(File rgbs, File audio) {
        ArrayList<Index> idxs = new ArrayList<>();

        long numFrames = rgbs.length();
        long minimal_interval = numFrames / 200;

        RandomAccessFile raf = null;
        FileChannel channel = null;
        ByteBuffer buffer = null;
        try {
            raf = new RandomAccessFile(rgbs, "r");
            channel = raf.getChannel();
            buffer = ByteBuffer.allocate(width * height * 3);

            // step 1 #todo
            // get thresholds T1 T2 T3 using statistical analysis
            // or google a known threshold set
            BufferedImage firstFrame = ImageIO.read(rgbs);
            float T1 = getThreshold(firstFrame, 85);
            float T2 = getThreshold(firstFrame, 50);
            float T3 = getThreshold(firstFrame, 30);

//            float T1 = 0.85F;
//            float T2 = 0.5F;
//            float T3 = 0.3F;

            // step 2
            for (long i = 0; i < numFrames; i++) {
                buffer.clear();
                channel.read(buffer);
                buffer.rewind();
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int r = buffer.get() & 0xff;
                        int g = buffer.get() & 0xff;
                        int b = buffer.get() & 0xff;

                        currFrame[y * width + x] = rgb2lum(r, g, b);
                    }
                }

                if (i - prev_Cut_idx >= minimal_interval && i != 0) {
                    // compare hDiff with 3 thresholds
                    if (histogramDifference(prev_Scene, currFrame) >= T1) {
                        idxs.add(new Index(i, Level.scene));
                        System.arraycopy(currFrame, 0, prev_Scene, 0, currFrame.length);
                        System.arraycopy(currFrame, 0, prev_Shot, 0, currFrame.length);
                        System.arraycopy(currFrame, 0, prev_Subshot, 0, currFrame.length);
                        continue;
                    }

                    if (histogramDifference(prev_Shot, currFrame) >= T2) {
                        idxs.add(new Index(i, Level.shot));
                        System.arraycopy(currFrame, 0, prev_Shot, 0, currFrame.length);
                        System.arraycopy(currFrame, 0, prev_Subshot, 0, currFrame.length);
                        continue;
                    }

                    if (histogramDifference(prev_Subshot, currFrame) >= T3) {
                        idxs.add(new Index(i, Level.subshot));
                        System.arraycopy(currFrame, 0, prev_Subshot, 0, currFrame.length);
                    }
                } else {
                    idxs.add(new Index(i, Level.scene));
                    System.arraycopy(currFrame, 0, prev_Scene, 0, currFrame.length);
                    System.arraycopy(currFrame, 0, prev_Shot, 0, currFrame.length);
                    System.arraycopy(currFrame, 0, prev_Subshot, 0, currFrame.length);
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

    // get the histogram-difference array for any 2 frames.
    public static float histogramDifference(int[] prev, int[] curr) {
        // step1: get histogram bins of macro-blocks of prev frame and curr frame
        int numBins = 256;
        int N = 16; // the macroblock size is N by N
        int numBlocks = prev.length / (N * N);
        int[][] prevHistogram = new int[numBlocks][numBins];
        int[][] currHistogram = new int[numBlocks][numBins];
        int A = (int) Math.ceil((double) width / N);
        int B = (int) Math.ceil((double) height / N);
        for (int i = 0; i < numBlocks; i++) {
            // #error
            // this is not a macroblock but a line in a frame
            // int[] prevBlock = Arrays.copyOfRange(prev, i * 16 * 16, (i + 1) * 16 * 16);
            // int[] currBlock = Arrays.copyOfRange(curr, i * 16 * 16, (i + 1) * 16 * 16);

            // #todo
            // check my implementation:
            int[] prevBlock = new int[N * N];
            int[] currBlock = new int[N * N];
            int base_row = i / A;
            int base_col = i % A;
            for (int ii = 0; ii < N; ii++) {
                if (base_row + ii >= height) {
                    break;
                }
                for (int jj = 0; jj < N; jj++) {
                    if (base_col + jj >= width) {
                        break;
                    }
                    int idx = (base_row + ii) * width + (base_col + jj);
                    prevBlock[ii * N + jj] = prev[idx];
                    currBlock[ii * N + jj] = curr[idx];
                }
            }
            prevHistogram[i] = calculateHistogram(prevBlock, numBins);
            currHistogram[i] = calculateHistogram(currBlock, numBins);
        }


        // step2: calculate the difference using the Block histogram difference (BH)
        float totalDifference = 0;
        for (int i = 0; i < numBlocks; i++) {
            float blockDifference = calculateBlockDifference(prevHistogram[i], currHistogram[i]);
            totalDifference += blockDifference;
        }
        float averageDifference = totalDifference / numBlocks;
        return averageDifference;
    }

    private static int[] calculateHistogram(int[] block, int numBins) {
//        int numBins = 256;
        int[] histogram = new int[numBins];
        for (int i = 0; i < block.length; i++) {
            histogram[block[i] * (numBins - 1) / 256]++;
        }
        return histogram;
    }

    private static float calculateBlockDifference(int[] prevHistogram, int[] currHistogram) {
        float difference = 0;
        for (int i = 0; i < prevHistogram.length; i++) {
            float binDifference = Math.abs(prevHistogram[i] - currHistogram[i]);
            difference += binDifference;
        }
        return difference;
    }
    // it is the 3rd measurements mentioned in 1994.pdf 3.1


    // extract luminance(0-255) from RGB;
    // the weight is from YCbCr color space
    public static int rgb2lum(int r, int g, int b) {
        return (int) Math.floor(0.299 * r + 0.587 * g + 0.114 * b);
    }

//    // rgb to HSV/LUV color space, not decided yet
//    public static int[] rgb2luv(int r, int g, int b) {
//        // rgb -> xyz
////        [ X ]   [  0.412453  0.357580  0.180423 ]   [ R ]
////        [ Y ] = [  0.212671  0.715160  0.072169 ] * [ G ]
////        [ Z ]   [  0.019334  0.119193  0.950227 ]   [ B ]
//
//        // xyz -> luv
//        // see https://en.wikipedia.org/wiki/CIELUV
//
//        return new int[]{0, 0, 0};
//    }

}
