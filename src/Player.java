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

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
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
    static JPanel scenePanel = new JPanel();
    static JPanel videoPanel = new JPanel();
    static JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scenePanel, videoPanel);

    static JButton playButton, pauseButton, stopButton;
    static JPanel controlPanel;

    static Thread videoThread, audioThread;
    static long updatedFrame = 0;
    static Clip clip;
    static AudioInputStream audioInputStream;
    static boolean rerenderVideo = false, rerenderAudio = false;

    static boolean isPaused, isStopped;

    public static void display(File rgbs, File audio, ArrayList<Index> idxs) throws IOException, LineUnavailableException {
        // create the JFrame and JLabel to display the video
        frame.setTitle("Video Player");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(new Dimension(windowWidth, windowHeight));
        frame.setMinimumSize(new Dimension(windowWidth, windowHeight));
        frame.setLayout(new BorderLayout());
        frame.setVisible(true);
        
        label.setPreferredSize(new Dimension(videoWidth, videoHeight));
        scenePanel.setLayout(new BoxLayout(scenePanel, BoxLayout.PAGE_AXIS));
        for (Index i : idxs) {
            scenePanel.add(createButton(i));
        }

        controlPanel = new JPanel();
        playButton = new JButton("Play");
        playButton.addActionListener(e -> {
            if (isStopped) {
                rerenderVideo = true;
                rerenderAudio = true;
                isStopped = false;
            }
            if(isPaused) {
                rerenderAudio = true;
                isPaused = false;
            } 
        });
        
        pauseButton = new JButton("Pause");
        pauseButton.addActionListener(e -> {
            isPaused = true;
        });

        stopButton = new JButton("Stop");
        stopButton.addActionListener(e -> {
            isStopped = true;
        });

        controlPanel.add(playButton);
        controlPanel.add(pauseButton);
        controlPanel.add(stopButton);
        
        videoPanel.setLayout(new BoxLayout(videoPanel, BoxLayout.Y_AXIS));
        videoPanel.add(label);
        videoPanel.add(controlPanel);

        isPaused = false;
        isStopped = false;
        updatedFrame = 0;

        frame.add(splitPane);
        frame.pack();

        videoThread = new Thread(() -> {
            renderVideo(rgbs, updatedFrame);
        });

        audioThread = new Thread(() -> {
            try {
                renderAudio(audio, updatedFrame);
            } catch (LineUnavailableException | IOException | UnsupportedAudioFileException e) {
                e.printStackTrace();
            }
        });

        videoThread.start();
        audioThread.start();
    }

    static void renderAudio(File audio, long startFrame) throws LineUnavailableException, IOException, UnsupportedAudioFileException {
        audioInputStream = AudioSystem.getAudioInputStream(audio);
        clip = AudioSystem.getClip();
        
        long audioFrameRate = (long) audioInputStream.getFormat().getSampleRate();
        long videoFrameIndex = startFrame;
        long videoFrameRate = 30;
        int audioSamplesPerFrame = audioInputStream.getFormat().getFrameSize() / audioInputStream.getFormat().getChannels();
    
        long audioFrameIndexForVideoFrame = audioFrameRate / videoFrameRate * videoFrameIndex / audioSamplesPerFrame;
        // long bytesPerFrame = audioInputStream.getFormat().getFrameSize();
        // long skipBytes = audioFrameIndexForVideoFrame * bytesPerFrame;
        // audioInputStream.skip(skipBytes);
    
        clip.open(audioInputStream);
        clip.setFramePosition((int) audioFrameIndexForVideoFrame);
        clip.start();
    
        while (clip != null) {
            if (isStopped || isPaused) {
                long currentFrame = clip.getFramePosition();
                clip.stop();

                if (isPaused) {
                    updatedFrame = currentFrame;
                }
            } else if (clip.getFramePosition() > 0) {
                if (rerenderAudio) {
                    rerenderAudio = false;
                    clip.stop();
                    renderAudio(audio, updatedFrame);
                }
            }
        }
    }

    static void renderVideo(File rgbs, long startFrame) {
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
                if (rerenderVideo) {
                    rerenderVideo = false;
                    renderVideo(rgbs, updatedFrame);
                }

                if (!isPaused && !isStopped && !Thread.interrupted()) {
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
                    // videoPanel.add(label);
                    videoPanel.validate();
                    videoPanel.repaint();
    
                    try {
                        Thread.sleep(1000 / fps);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    currentFrame++;
                }
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
            updatedFrame = index.idx;
            rerenderVideo = true;
            rerenderAudio = true;
        });

        switch (index.level) {
            case scene:
                sceneCount++;
                shotCount = 0;
                subshotCount = 0;
                button.setText("Scene " + sceneCount + " (" + index.idx + ")");
                button.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 5));
                break;
            case shot:
                shotCount++;
                subshotCount = 0;
                button.setText("Shot " + shotCount + " (" + index.idx + ")");
                button.setBorder(BorderFactory.createEmptyBorder(0, 20, 5, 5));
                break;
            case subshot:
                subshotCount++;
                button.setText("Subshot " + subshotCount + " (" + index.idx + ")");
                button.setBorder(BorderFactory.createEmptyBorder(0, 40, 5, 5));
                break;
        }
        
        return button;
    }
}