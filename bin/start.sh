#!/bin/bash

JIT_LOGGING_OPT='-XX:+AggressiveOpts -XX:+AlwaysCompileLoopMethods -XX:+BackgroundCompilation -XX:+CMSClassUnloadingEnabled -XX:-DontCompileHugeMethods -XX:MaxHeapSize=134217728 -XX:+PrintCommandLineFlags -XX:+PrintCompilation -XX:+PrintGC -XX:+PrintVMOptions -XX:+RelaxAccessControlCheck'

mvn -Plinode package war:exploded && {

    set -x
    rt=$PWD/$(dirname $0)
    s=$(grep '<version>' ${rt}/../pom.xml | head -n1 | sed -e 's,[^>]*>\([^<]*\).*,\1,g')
    RD=${rt}/..
    TD=${RD}/rxf-server
    pushd ${TD}
    LAUNCHCP=$(mvn dependency:build-classpath | grep -v '\[INFO\]' | grep -v ' ')
    LAUNCHME=${RD}/rxf-server/target/rxf-server-${s}-jar-with-dependencies.jar
    mvn assembly:assembly
    popd

} && {
    set -x
    rt=$PWD/$(dirname $0)
    s=$(grep '<version>' ${rt}/../pom.xml | head -n1 | sed -e 's,[^>]*>\([^<]*\).*,\1,g')
    RD=${rt}/..
    TD=${RD}/rxf-examples
    pushd ${TD}
    CP=$(mvn dependency:build-classpath | grep -v '\[INFO\]' | grep -v ' ')
    TD=${RD}/rxf-examples/target/rxf-examples-${s}/
    pushd ${TD}
    {
        TD=$(pwd -P)
        PATHSEP=":"
        [[ "${OS}" = "Windows_NT" ]] && PATHSEP=';'
        FP="WEB-INF/classes${PATHSEP}${CP}"
        java -Xmx4g -cp ${FP} -jar $LAUNCHME $@
    }
    popd
}
