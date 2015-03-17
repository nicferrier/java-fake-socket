#!/bin/bash

DIR=$(cd $(dirname $0) ; pwd)
mkdir -p $DIR/classes
rm -rf classes/*

cat <<EOF > .files
$DIR/src/fakesocket/Address.java
$DIR/src/fakesocket/FakeSocketImpl.java
$DIR/src/fakesocket/CommunicatingSocket.java
$DIR/src/fakesocket/Backplane.java
$DIR/src/fakesocket/TestFakeSocket.java
EOF

javac -d $DIR/classes @.files
java -classpath $DIR/classes fakesocket.TestFakeSocket

# build ends
