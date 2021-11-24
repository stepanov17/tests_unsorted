#!/bin/bash

TEST=Locks

JDK=$1

$JDK/bin/javac $TEST.java

#OPTS="-Xcomp -Xbatch"
OPTS="-Xint"

$JDK/bin/java $OPTS -XX:CRaCCheckpointTo=cr $TEST

sleep 5

for i in {1..50}
do
    echo ">> restore $i"
    $JDK/bin/java -XX:CRaCRestoreFrom=cr $TEST
#    printf "\n"
done
