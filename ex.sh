#!/bin/bash
cd src
javac -d ../out Level.java Index.java IndexExtractor.java Player.java Main.java
cd ..
java -classpath ./out Main ./data/InputVideo.rgb ./data/InputAudio.wav