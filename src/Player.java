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
import javax.swing.JScrollPane;
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

    static JFrame frame;
    static JSplitPane splitPane;

    static JScrollPane scrollPane;
    static JPanel scenePanel;
    static JLabel label;

    static JPanel videoPanel, controlPanel;
    static JButton playButton, pauseButton, stopButton;

    static Thread videoThread, audioThread;
    static long currUpdatedFrame, shotStartFrame;
    static Clip clip;
    static AudioInputStream audioInputStream;
    static boolean rerenderVideo, rerenderAudio, currFrameUpdate;
    static boolean isPaused, isStopped;

    static ArrayList<Index> mIndexes;

    public static void display(File rgbs, File audio, ArrayList<Index> idxs) throws IOException, LineUnavailableException, UnsupportedAudioFileException {
        mIndexes = idxs;

        frame = new JFrame();
        frame.setTitle("Video Player");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(new Dimension(windowWidth, windowHeight));
        frame.setMinimumSize(new Dimension(windowWidth, windowHeight));
        frame.setLayout(new BorderLayout());

        currUpdatedFrame = 0;
        shotStartFrame = 0;
        currFrameUpdate = false;
        rerenderAudio = false;
        rerenderVideo = false;
        
        isPaused = false;
        isStopped = false;
        
        scenePanel = new JPanel();
        scenePanel.setLayout(new BoxLayout(scenePanel, BoxLayout.Y_AXIS));
        for (Index i : idxs) {
            scenePanel.add(createButton(i));
        }

        scrollPane = new JScrollPane(scenePanel);
        scrollPane.setPreferredSize(new Dimension(220, 400));
        scrollPane.setMinimumSize(new Dimension(220, 400));
        frame.getContentPane().add(scrollPane, BorderLayout.WEST);
        scrollPane.revalidate();
        scrollPane.repaint();

        videoPanel = new JPanel();
        videoPanel.setLayout(new BoxLayout(videoPanel, BoxLayout.Y_AXIS));
        label = new JLabel();
        label.setPreferredSize(new Dimension(600, 350));
        label.setMinimumSize(new Dimension(600, 350));

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
            currFrameUpdate = true;
        });

        stopButton = new JButton("Stop");
        stopButton.addActionListener(e -> {
            isStopped = true;
        });

        controlPanel.add(playButton);
        controlPanel.add(pauseButton);
        controlPanel.add(stopButton);
        
        videoPanel.add(label);
        videoPanel.add(controlPanel);

        splitPane = new JSplitPane();
        splitPane.setLeftComponent(scrollPane);
        splitPane.setRightComponent(videoPanel);

        frame.add(splitPane);
        frame.pack();
        frame.setVisible(true);

        videoThread = new Thread(() -> {
            renderVideo(rgbs, shotStartFrame);
        });

        audioThread = new Thread(() -> {
            try {
                renderAudio(audio, shotStartFrame);
            } catch (LineUnavailableException | IOException | UnsupportedAudioFileException e) {
                e.printStackTrace();
            }
        });

        videoThread.start();
        audioThread.start();
    }

    static long calculateAudioframe(long videoFrame) {
        long audioFrameRate = (long) audioInputStream.getFormat().getSampleRate();
        long videoFrameIndex = videoFrame;
        long videoFrameRate = 30;
        int audioSamplesPerFrame = audioInputStream.getFormat().getFrameSize() / audioInputStream.getFormat().getChannels();
    
        long audioFrameIndexForVideoFrame = audioFrameRate / videoFrameRate * videoFrameIndex / audioSamplesPerFrame;
        return audioFrameIndexForVideoFrame;
    }

    static void renderAudio(File audio, long startFrame) throws LineUnavailableException, IOException, UnsupportedAudioFileException {
        audioInputStream = AudioSystem.getAudioInputStream(audio);
        clip = AudioSystem.getClip();

        long audioFrameIndexForVideoFrame = calculateAudioframe(startFrame);
        long bytesPerFrame = audioInputStream.getFormat().getFrameSize();
        long skipBytes = audioFrameIndexForVideoFrame * bytesPerFrame;
        audioInputStream.skip(skipBytes);
    
        clip.open(audioInputStream);
        clip.setFramePosition((int) audioFrameIndexForVideoFrame);
        clip.start();
    
        while (clip != null) {
            if (isStopped || isPaused) {
                clip.stop();
                clip.flush();
            } 
            
            if (clip.getFramePosition() >= 0 && !isPaused && !isStopped) {
                if (rerenderAudio) {
                    rerenderAudio = false;
                    clip.stop();
                    clip.flush();

                    audioInputStream = AudioSystem.getAudioInputStream(audio);
                    clip = AudioSystem.getClip();

                    if (currFrameUpdate) {
                        currFrameUpdate = false;
                        audioFrameIndexForVideoFrame = calculateAudioframe(currUpdatedFrame);
                    } else {
                        audioFrameIndexForVideoFrame = calculateAudioframe(shotStartFrame);
                    }
                    bytesPerFrame = audioInputStream.getFormat().getFrameSize();
                    skipBytes = audioFrameIndexForVideoFrame * bytesPerFrame;
                    audioInputStream.skip(skipBytes);
                
                    clip.open(audioInputStream);
                    clip.setFramePosition((int) audioFrameIndexForVideoFrame);
                    clip.start();
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
                    if (currFrameUpdate) {
                        currFrameUpdate = false;
                        renderVideo(rgbs, currUpdatedFrame);
                    } else {
                        renderVideo(rgbs, shotStartFrame);
                    }
                }

                if (!isPaused && !isStopped) {
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
                    videoPanel.validate();
                    videoPanel.repaint();
    
                    try {
                        Thread.sleep(1000 / fps);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    currentFrame++;
                }

                currUpdatedFrame = currentFrame;
                // Update last shot frame index
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
            // currUpdatedFrame = index.idx;
            shotStartFrame = index.idx;
            currFrameUpdate = false;
            rerenderVideo = true;
            rerenderAudio = true;
            isStopped = false;
            isPaused = false;
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