#!/bin/bash
cd src
javac -d ../out Level.java Index.java IndexExtractor.java Player.java Main.java
if [ $? -eq 0 ]; then
cd ..
java -classpath ./out Main ./data/InputVideo.rgb ./data/InputAudio.wav
else
echo -e "\nCompilation failed"
fi
