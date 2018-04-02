#!/bin/sh
rm -r bin
mkdir -p bin
mkdir -p bin/my_peers

javac $(find src | grep .java) -d bin

cd bin && rmiregistry 
#cd bin && java rmi.RMI
