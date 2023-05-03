import java.io.File;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main {
    public static void main(String[] args) {
        File rgbs = new File(args[0]); // name of the RGB video file
        File audio = new File(args[1]); // name of the audio file

        Player.play(rgbs, audio, IndexExtractor.extractIndex(rgbs, audio));
//        Player.play(rgbs, audio, IndexExtractor.mockExtractIndex(rgbs, audio));
    }
}