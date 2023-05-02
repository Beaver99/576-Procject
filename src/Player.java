import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;

public class Player {
    static final int windowHeight = 480;
    static final int windowWidth = 640;
    static final int videoHeight = 270;
    static final int videoWidth = 480;

    static JFrame frame = new JFrame();
    static JLabel label = new JLabel();
    static JPanel panel1 = new JPanel();
    static JPanel panel2 = new JPanel();
    static JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panel1, panel2);

    static Thread videoThread;

    public static void play(File rgbs, File audio, ArrayList<Index> idxs) {
        // create the JFrame and JLabel to display the video
        frame.setTitle("Video Player");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(new Dimension(windowWidth, windowHeight));
        frame.setMinimumSize(new Dimension(windowWidth, windowHeight));
        frame.setLayout(new BorderLayout());
        frame.setVisible(true);
        
        label.setPreferredSize(new Dimension(videoWidth, videoHeight));
        panel1.setLayout(new BoxLayout(panel1, BoxLayout.PAGE_AXIS));
        for (Index i : idxs) {
            panel1.add(createButton(i));
        }
        
        frame.add(splitPane);
        frame.pack();

        videoThread = new Thread(() -> {
            renderVideo(rgbs, audio, 0);
        });
        videoThread.start();
    }

    static void renderVideo(File rgbs, File audio, long startFrame) {
        int width = videoWidth;
        int height = videoHeight;
        int fps = 30;
        long numFrames = rgbs.length() / (height * width * 3);

        // read the video file and display each frame
        try {
            RandomAccessFile raf = new RandomAccessFile(rgbs, "r");
            FileChannel channel = raf.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(height * width * 3);
            long currentFrame = 0;

            if (currentFrame < startFrame) {
                raf.seek(startFrame * height * width * 3);
                currentFrame = startFrame;
            }

            while (currentFrame < numFrames) {
                buffer.clear();
                channel.read(buffer);
                buffer.rewind();
                BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int r = buffer.get() & 0xff;
                        int g = buffer.get() & 0xff;
                        int b = buffer.get() & 0xff;
                        int color = (r << 16) | (g << 8) | b;
                        image.setRGB(x, y, color);
                    }
                }
                label.setIcon(new ImageIcon(image));
                panel2.add(label);
                panel2.validate();
                panel2.repaint();

                try {
                    Thread.sleep(1000 / fps);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                currentFrame++;
            }
            channel.close();
            raf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int sceneCount = 0;
    private static int shotCount = 0;
    private static int subshotCount = 0;
    static JButton createButton(Index index) {
        JButton button = new JButton();
        button.setBorderPainted(false);
        button.addActionListener(e -> {
        });

        switch (index.level) {
            case scene:
                sceneCount++;
                shotCount = 0;
                subshotCount = 0;
                button.setText("Scene " + sceneCount);
                button.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 5));
                break;
            case shot:
                shotCount++;
                subshotCount = 0;
                button.setText("Shot " + shotCount);
                button.setBorder(BorderFactory.createEmptyBorder(0, 20, 5, 5));
                break;
            case subshot:
                subshotCount++;
                button.setText("Subshot " + subshotCount);
                button.setBorder(BorderFactory.createEmptyBorder(0, 40, 5, 5));
                break;
        }
        
        return button;
    }
}