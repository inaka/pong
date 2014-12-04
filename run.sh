#!/bin/sh
javac -g -classpath 'lib/gdata/*:lib/jung2-2_0_1/*' -sourcepath src -d bin src/net/inaka/pong/rank/Ranker.java
jar cf lib/ranker.jar -C bin .
java -classpath 'lib/ranker.jar:lib/gdata/*:lib/jung2-2_0_1/*' net.inaka.pong.rank.Ranker | grep -v DEBUG | ./hrm.sh -t Rusue4l8hoBFwq5nVYgdWsm2dzcmHC8QgP9xmNUN -r "1008140" -c random -n
#java -classpath 'lib/ranker.jar:lib/gdata/*:lib/jung2-2_0_1/*' net.inaka.pong.rank.Ranker