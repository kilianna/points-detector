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
${JDK_PATH}javac -encoding utf8 -cp ${FIJI_PATH}jars/ij-1.*.jar -d build Points_Detector.java Slices_Correction.java Slices_Div.java Colors_Move.java
cd build
${JDK_PATH}jar cvf ../dist/Points_Detector.jar ../plugins.config *.class
cd ..

if [ "$3" = "run" ]; then
	cp -f dist/Points_Detector.jar ${FIJI_PATH}plugins/
	kill $(xprop -name "(Fiji Is Just) ImageJ" _NET_WM_PID) || echo "OK"
	sleep 1
	gnome-terminal --title=FijiTerm -- ${FIJI_PATH}ImageJ-linux64 '/media/doki/One Touch/prv/gamma/2022_05_12/napromieniania gamma/500mGy/G_684_2019_500mGy_Cs137_od5um_co0.5um_30s004.nd2' &
	sleep 1
fi
