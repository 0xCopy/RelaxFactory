#!/bin/bash

JIT_LOGGING_OPT='-XX:+AggressiveOpts -XX:+AlwaysCompileLoopMethods -XX:+BackgroundCompilation -XX:+CMSClassUnloadingEnabled -XX:-DontCompileHugeMethods -XX:MaxHeapSize=134217728 -XX:+PrintCommandLineFlags -XX:+PrintCompilation -XX:+PrintGC -XX:+PrintVMOptions -XX:+RelaxAccessControlCheck'

rt=$PWD/$(dirname $0 )
p=$(grep '<artifactId>' ${rt}/../pom.xml |head -n1|sed -e 's,[^>]*>\([^<]*\).*,\1,g')
s=$(grep '<version>' ${rt}/../pom.xml |head -n1|sed -e 's,[^>]*>\([^<]*\).*,\1,g')
mvn -Plinode package war:exploded  &&
{
set -x
CP=$(mvn dependency:build-classpath |grep  -v '\[INFO\]'|grep -v ' ')
RD=${rt}/..
TD=${RD}/target/${p}-${s}/
pushd ${TD}
TD=$(pwd -P)
PATHSEP=":"
[[ ${OS} = Windows_NT ]] && PATHSEP=';'
FP="WEB-INF/classes${PATHSEP}${CP}"
java -Xmx126m -cp "${FP}" ro.server.KernelImpl
popd

}
