#!/bin/bash

JIT_LOGGING_OPT='-XX:+AggressiveOpts -XX:+AlwaysCompileLoopMethods -XX:+BackgroundCompilation -XX:+CMSClassUnloadingEnabled -XX:-DontCompileHugeMethods -XX:MaxHeapSize=134217728 -XX:+PrintCommandLineFlags -XX:+PrintCompilation -XX:+PrintGC -XX:+PrintVMOptions -XX:+RelaxAccessControlCheck'

mvn -Plinode package war:exploded && {
    set -x
    rt=$PWD/$(dirname $0)
    LAUNCHME=ds.server.DealSiteServer
    ###CARFEUL POMS USSUALLY PUT PARENT AS FIRST ARTIFACTId BUT THIS POM HAS BEEN ARRANGED
    a=$(grep '<artifactId>' ${rt}/../pom.xml | head -n1 | sed -e 's,[^>]*>\([^<]*\).*,\1,g')
    s=$(grep '<version>' ${rt}/../pom.xml | head -n1 | sed -e 's,[^>]*>\([^<]*\).*,\1,g')
    RD=${rt}/..
    TD=${RD}/${a}
    pushd ${TD}
    CP=$(mvn dependency:build-classpath | grep -v '\[INFO\]' | grep -v ' ')
    TD=${RD}/target/${a}-${s}/
    pushd ${TD}
    {
        TD=$(pwd -P)
        PATHSEP=":"
        [[ "${OS}" = "Windows_NT" ]] && PATHSEP=';'
        FP="WEB-INF/classes${PATHSEP}${CP}"
        java -Xmx4g -cp ${FP} ${LAUNCHME} $@
    }
    popd
}
