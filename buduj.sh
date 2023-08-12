#!/bin/bash
set -e

JDK_PATH=
FIJI_PATH=

if [ $# -ge 1 ]; then
    FIJI_PATH=$1
fi
if [ $# -ge 2 ]; then
    JDK_PATH=$2
fi

mkdir -p build
mkdir -p dist
${JDK_PATH}javac -encoding utf8 -cp ${FIJI_PATH}jars/ij-1.*.jar -d build Points_Detector.java Slices_Correction.java Slices_Mul.java Colors_Move.java Stack_CSV.java Utils.java NativeTools.java
cd build
${JDK_PATH}jar cvf ../dist/Points_Detector.jar ../plugins.config *.*
cd ..

if [ "$3" = "run" ]; then
	cp -f dist/Points_Detector.jar ${FIJI_PATH}plugins/
	kill $(xprop -name "(Fiji Is Just) ImageJ" _NET_WM_PID) || echo "OK"
	sleep 1
	gnome-terminal --title=FijiTerm -- ${FIJI_PATH}ImageJ-linux64 '/home/doki/Downloads/gamma 20mGy stack od 5um co 1um002.nd2' &
	sleep 1
fi
