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
echo ""
echo "====="
echo "Running $ACTION operation"
echo "====="
echo ""

if [ "$ACTION" = "clean" ]; then
    ${MAVEN} clean
elif [ "$ACTION" = "build" ]; then
    ${MAVEN} --projects marmoset -DskipTests=true install assembly:single
elif [ "$ACTION" = "rebuild" ]; then
    ${MAVEN} --projects marmoset,core -DskipTests=true install assembly:single
elif [ "$ACTION" = "run" ]; then
    ${JAVA} -ea -cp marmoset/target/marmoset-0.7-SNAPSHOT-with-dep.jar com.graphhopper.marmoset.Marmoset
elif [ "$ACTION" = "test" ]; then
    ${MAVEN} test
fi

E_CODE=$?

# recursively call self so multiple commands can be stacked
if [ "$2" != "" ] && (($E_CODE == 0)); then
    shift
    ./$0 $@
elif (($E_CODE > 0)); then
    echo "Error code $E_CODE when running $ACTION"
    exit ${E_CODE}
fi