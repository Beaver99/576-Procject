import java.io.File;
import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main {
    public static void main(String[] args) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        File rgbs = new File(args[0]); // name of the RGB video file
        File audio = new File(args[1]); // name of the audio file

       Player.display(rgbs, audio, IndexExtractor.extractIndex(rgbs, audio));
        // Player.display(rgbs, audio, IndexExtractor.mockExtractIndex(rgbs, audio));
    }
}