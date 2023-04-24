#!/bin/bash
set -e

ARCH=
JDK_INCLUDE=$JAVA_HOME/include
FLAGS="-O3 -Wall"

if [ $# -ge 1 ]; then
    ARCH=$1
fi
if [ $# -ge 2 ]; then
    JDK_INCLUDE="$2"
fi

mkdir -p build

case "$OSTYPE" in
  *darwin*)
    gcc -c -fPIC $FLAGS -I$JDK_INCLUDE -I$JDK_INCLUDE/darwin -o /tmp/NativeTools.o NativeTools.c
    gcc -dynamiclib -o build/native_tools.dylib /tmp/NativeTools.o -lc
    strip build/native_tools.dylib
    ;;
  *msys* | *win*)
    gcc -c $FLAGS -I$JDK_INCLUDE -I$JDK_INCLUDE/win32 -o /tmp/NativeTools$ARCH.o NativeTools.c
    gcc -shared -o build/native_tools$ARCH.dll /tmp/NativeTools$ARCH.o
    strip build/native_tools$ARCH.dll
    ;;
  *)
    gcc -c -fPIC $FLAGS -I$JDK_INCLUDE -I$JDK_INCLUDE/linux -o /tmp/NativeTools.o NativeTools.c
    gcc -shared -fPIC -o build/native_tools.so /tmp/NativeTools.o -lc
    strip build/native_tools.so
esac
