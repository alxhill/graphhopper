#!/usr/bin/env bash

GH_HOME=$(dirname "$0")
JAVA=$JAVA_HOME/bin/java
if [ "$JAVA_HOME" = "" ]; then
    JAVA=java
fi
MAVEN="$GH_HOME/maven/bin/mvn"

ACTION=$1

if [ "$ACTION" = "" ]; then
    echo "Use 'build', 'clean', 'test' or 'run' ok? or is that too hard for you? seriously you wrote this get with it."
fi

if [ "$ACTION" = "clean" ]; then
    ${MAVEN} clean
    exit
elif [ "$ACTION" = "build" ]; then
    ${MAVEN} --projects marmoset -DskipTests=true install assembly:single
elif [ "$ACTION" = "run" ]; then
    ${JAVA} -cp marmoset/target/marmoset-0.7-SNAPSHOT-with-dep.jar com.graphhopper.marmoset.Marmoset
elif [ "$ACTION" = "test" ]; then
    ${MAVEN} test
fi

