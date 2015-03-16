#!/bin/bash

DIR=$(cd $(dirname $0) ; pwd)
mkdir -p $DIR/classes
rm -rf classes/*
javac -d $DIR/classes $DIR/FakeSocket.java
java -classpath $DIR/classes FakeSocket
