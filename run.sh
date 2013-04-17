#!/bin/sh
javac -g -classpath 'lib/gdata/*:lib/jung2-2_0_1/*' -sourcepath src -d bin src/net/inaka/pong/rank/Ranker.java
jar cf lib/ranker.jar -C bin .
java -classpath 'lib/ranker.jar:lib/gdata/*:lib/jung2-2_0_1/*' net.inaka.pong.rank.Ranker | grep -v DEBUG | ./hrm.sh -t 39552c1d0e1f38eef2f65bcc46deee -r "InakaPong" -f "PongRank" -c random -n
