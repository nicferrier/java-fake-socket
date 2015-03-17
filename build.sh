#!/bin/bash

DIR=$(cd $(dirname $0) ; pwd)
mkdir -p $DIR/classes
rm -rf classes/*
javac -d $DIR/classes $DIR/src/{Address,FakeSocketImpl,CommunicatingSocket,Backplane,TestFakeSocket}.java
java -classpath $DIR/classes fakesocket.TestFakeSocket

# build ends
