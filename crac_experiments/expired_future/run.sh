#!/bin/bash

TEST=FutureTest

JDK=$1

$JDK/bin/javac $TEST.java

$JDK/bin/java -XX:CRaCCheckpointTo=cr $TEST

sleep 10

for i in {1..50}
do
    printf ">> restore $i >> "
    $JDK/bin/java -XX:CRaCRestoreFrom=cr $TEST
done
