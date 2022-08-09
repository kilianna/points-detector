#!/bin/bash
set -e

javac -cp /home/doki/my/Fiji.app/jars/ij-1.*.jar Plugin_Wykrywania.java
if [ "$1" = "run" ]; then
	cp -f Plugin_Wykrywania*.class /home/doki/my/Fiji.app/plugins/
	kill $(xprop -name "(Fiji Is Just) ImageJ" _NET_WM_PID) || echo "OK"
	sleep 1
	gnome-terminal --title=FijiTerm -- /home/doki/my/Fiji.app/ImageJ-linux64 /home/doki/my/filtry-ani/test.tif &
	sleep 1
fi
