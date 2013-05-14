#!/bin/sh
javac -g -classpath 'lib/gdata/*:lib/jung2-2_0_1/*' -sourcepath src -d bin src/net/inaka/pong/rank/Ranker.java
jar cf lib/ranker.jar -C bin .
echo "-----------" >> history.txt
date "+%Y-%m-%d %H:%M:%S" >> history.txt
java -classpath 'lib/ranker.jar:lib/gdata/*:lib/jung2-2_0_1/*' net.inaka.pong.rank.Ranker | grep -v DEBUG >> history.txt
cat history.txt
